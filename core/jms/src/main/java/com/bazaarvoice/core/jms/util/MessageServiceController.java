package com.bazaarvoice.core.jms.util;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jms.support.JmsUtils;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Starts and stops a set of JMS message listeners using a set of message brokers.
 */
public class MessageServiceController implements PausableServiceController {

    private static final Log _sLog = LogFactory.getLog(MessageServiceController.class);

    public interface ServiceFactory {
        ServiceInstance createInstance(Connection connection) throws JMSException;
    }

    public interface ServiceInstance {
        long getNumMessagesHandled();
        long getCurrentIdleTime();
        void waitUntilIdle(long maxWait) throws TimeoutException, InterruptedException;
        void close() throws JMSException;
    }

    private BrokerAdapter _messageBrokerAdapter;
    private Map<String, ConnectionFactory> _messageBrokers;
    private ServiceFactory[] _messageServices;
    private final List<ConnectionHolder> _connections = new ArrayList<ConnectionHolder>();
    private boolean _connected;
    private boolean _started;

    @Required
    public void setMessageBrokerAdapter(BrokerAdapter messageBrokerAdapter) {
        _messageBrokerAdapter = messageBrokerAdapter;
    }

    @Required
    public void setMessageBrokerURLs(String messageBrokerURLs) throws JMSException {
        _messageBrokers = _messageBrokerAdapter.createNonPoolingConnectionFactories(messageBrokerURLs, true);
    }

    @Required
    public void setMessageServices(ServiceFactory[] messageServices) {
        _messageServices = messageServices;
    }

    public synchronized boolean isConnected() {
        return _connected;
    }

    public synchronized void connect() throws JMSException, InterruptedException {
        if (isConnected()) {
            throw new IllegalStateException("Already connected to message brokers.");
        }
        if (_messageBrokers.isEmpty()) {
            throw new IllegalStateException("No message brokers have been configured for message service controller.");
        }

        final MutableInt numInitializing = new MutableInt(_messageBrokers.size());
        final MutableInt numSucceeded = new MutableInt(0);

        // start a separate initialization thread for each broker
        int counter = 0;
        for (Map.Entry<String, ConnectionFactory> entry : _messageBrokers.entrySet()) {
            final int threadNumber = counter++;
            final String brokerURL = entry.getKey();
            final ConnectionFactory connectionFactory = entry.getValue();
            new Thread("MessageServiceControllerConnect" + (threadNumber + 1)) {
                public void run() {

                    // We use delay to work around the Tomcat classloader multithreading issue
                    // via staggering creating JMS connections. See ticket #11296 for details.
                    try {
                        Thread.sleep(threadNumber * 200L);
                    } catch (InterruptedException e) {
                        // ignore
                    }

                    boolean success = false;
                    try {
                        // connect to a message broker.
                        Connection connection = connectionFactory.createConnection();

                        ConnectionHolder connectionHolder = new ConnectionHolder(connection);

                        // configure Queue and Topic listeners.  IF THE BROKER IS DOWN THIS
                        // MAY HANG FOREVER!  it will also prevent a clean shutdown of the app
                        for (ServiceFactory service : _messageServices) {
                            ServiceInstance serviceInstance = service.createInstance(connection);
                            addInstance(connectionHolder, serviceInstance);
                        }

                        // add the connection to the set of connections eligible for message delivery
                        addConnection(connectionHolder);

                        success = true;

                    } catch (Throwable t) {
                        _sLog.error("Error initializing message service controller: " + brokerURL, t);
                    } finally {
                        // notify the main thread that this initialization thread is complete
                        synchronized (MessageServiceController.this) {
                            numInitializing.decrement();
                            if (success) {
                                numSucceeded.increment();
                            }
                            MessageServiceController.this.notifyAll();
                        }
                    }
                }
            }.start();
        }

        _connected = true;

        // wait for at least one thread to complete initialization successfully.
        int prevInitializing = numInitializing.intValue();
        long timeOfLastChange = System.currentTimeMillis();
        for (; ;) {
            wait(15000L); // 15 second timeout

            if (numInitializing.intValue() == 0) {
                break;  // done with all brokers
            }

            // how long has it been since numInitializing changed?
            if (prevInitializing != numInitializing.intValue()) {
                prevInitializing = numInitializing.intValue();
                timeOfLastChange = System.currentTimeMillis();

            } else if (System.currentTimeMillis() - timeOfLastChange >= 15000L) {
                // it has been 15 seconds since numInitializing last changed.  stop waiting if at least one
                // broker is up--the secondary broker may be down unrecoverably and the app should work as
                // long as there at least one broker is running.
                if (numSucceeded.intValue() > 0) {
                    _sLog.warn("Secondary message service controller is taking a long time to start.  Continuing anyway...");
                    break;
                } else {
                    _sLog.warn("Message service controller is taking a long time to start.");
                }
            }
        }
        if (numSucceeded.intValue() == 0) {
            // all initialization threads failed with Java exceptions.  fail the start() method.
            // note: this won't happen if all brokers are down.  in that case we'll wait forever,
            // printing out a warning message every 15 seconds.
            throw new IllegalStateException("All message listeners failed to initialize for message service controller.");
        }
    }

    /**
     * Make a good faith effort to return the number of messages handled since the server started.
     * Because JMS messages are inherently asynchronous, by the time the call returns the number
     * of messages may have changed.
     * <p>
     * Useful for test cases and idle server detection.
     */
    public synchronized long getNumMessagesHandled() {
        // sum the counts across all the connections
        long numMessagesHandled = 0;
        for (ConnectionHolder connectionHolder : _connections) {
            for (ServiceInstance serviceInstance : connectionHolder.getServiceInstances()) {
                numMessagesHandled += serviceInstance.getNumMessagesHandled();
            }
        }
        return numMessagesHandled;
    }

    /**
     * Make a good faith effort to return the number of milliseconds since the last message
     * completed processing, or zero if there is at least one message in progress. Because
     * JMS messages are inherently asynchronous, this method can't guarantee that there
     * aren't pending messages when it returns.
     * <p>
     * Useful for test cases and idle server detection.
     */
    public synchronized long getCurrentIdleTime() {
        // return the minimum idle time across all the connections
        long minCurrentIdleTime = Long.MAX_VALUE;
        for (ConnectionHolder connectionHolder : _connections) {
            for (ServiceInstance serviceInstance : connectionHolder.getServiceInstances()) {
                minCurrentIdleTime = Math.min(minCurrentIdleTime, serviceInstance.getCurrentIdleTime());
            }
        }
        return minCurrentIdleTime;
    }

    /**
     * Make a good faith effort to wait until there are no pending messages for any of the
     * specified destinations.  Because JMS messages are inherently asynchronous, this method
     * can't guarantee that there aren't pending messages when it returns.
     * <p>
     * Useful for test cases.
     * @param maxWait Maximum time to wait without making any progress.
     */
    public synchronized void waitUntilIdle(long maxWait) throws TimeoutException, InterruptedException {
        for (ConnectionHolder connectionHolder : _connections) {
            for (ServiceInstance serviceInstance : connectionHolder.getServiceInstances()) {
                serviceInstance.waitUntilIdle(maxWait);
            }
        }
    }

    public synchronized void disconnect() throws JMSException {
        if (!isConnected()) {
            _sLog.warn("Already disconnected message service controller from message brokers.");
            return;
        }
        // loop through each active connection to synchronously close them.  this
        // is synchronous (on a single thread) to make sure that everything has
        // completely shut down before the disconnect() method returns.
        while (!_connections.isEmpty()) {
            safeClose(_connections.remove(_connections.size() - 1));
        }
        _connected = false;
    }

    /**
     * Returns true if the brokers are allowed to deliver messages to listeners.
     */
    public synchronized boolean isStarted() {
        return _started;
    }

    /**
     * Starts message delivery to the message listeners.
     */
    public synchronized void start() {
        if (!isConnected()) {
            throw new IllegalStateException("May not start message delivery without first connecting to message brokers.");
        }
        int numStarted = 0;
        for (ConnectionHolder connectionHolder : _connections) {
            numStarted += safeStartDelivery(connectionHolder) ? 1 : 0;
        }
        // ignore connections that failed to start as long as at least one did start
        if (numStarted == 0) {
            throw new IllegalStateException("Unable to start message delivery for all message brokers.");
        }
        _started = true;
    }

    /**
     * Pauses message delivery to all message listeners.
     */
    public synchronized void stop() {
        for (ConnectionHolder connectionHolder : _connections) {
            // if any fail, we presume they've already stopped delivering messages
            safeStopDelivery(connectionHolder);
        }
        _started = false;
    }

    private synchronized void addConnection(ConnectionHolder connectionHolder) throws JMSException {
        if (isConnected()) {
            _connections.add(connectionHolder);
            if (isStarted()) {
                safeStartDelivery(connectionHolder);
            }
        } else {
            safeClose(connectionHolder);
        }
    }

    private synchronized void addInstance(ConnectionHolder connectionHolder, ServiceInstance instance) {
        if (isConnected()) {
            connectionHolder.add(instance);
        } else {
            safeClose(instance);
        }
    }

    private boolean safeStartDelivery(ConnectionHolder connectionHolder) {
        try {
            connectionHolder.getConnection().start();
            return true;
        } catch (JMSException e) {
            _sLog.error("Error starting message delivery for a message broker.", e);
            return false;
        }
    }

    private void safeStopDelivery(ConnectionHolder connectionHolder) {
        try {
            connectionHolder.getConnection().stop();
        } catch (JMSException e) {
            _sLog.warn("Error stopping message delivery for a message broker.", e);
        }
    }

    private void safeClose(ConnectionHolder connectionHolder) {
        for (ServiceInstance serviceInstance : connectionHolder.getServiceInstances()) {
            safeClose(serviceInstance);
        }
        JmsUtils.closeConnection(connectionHolder.getConnection(), true);
    }

    private void safeClose(ServiceInstance instance) {
        try {
            instance.close();
        } catch (Throwable t) {
            _sLog.error("Error closing JMS session for message service controller.", t);
        }
    }

    private class ConnectionHolder {
        private final Connection _connection;
        private final List<ServiceInstance> _serviceInstances = new ArrayList<ServiceInstance>();

        private ConnectionHolder(Connection connection) {
            _connection = connection;
        }

        private Connection getConnection() {
            return _connection;
        }

        private List<ServiceInstance> getServiceInstances() {
            return _serviceInstances;
        }

        private void add(ServiceInstance serviceInstance) {
            _serviceInstances.add(serviceInstance);
        }
    }
}
