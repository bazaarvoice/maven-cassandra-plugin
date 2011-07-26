package com.bazaarvoice.core.jms.util;

import com.bazaarvoice.core.jms.service.AbstractMessageListener;
import com.bazaarvoice.core.logging.util.LoggingContextData;
import com.bazaarvoice.core.logging.util.LoggingContextUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jms.support.JmsUtils;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Configures a set of JMS message listeners that all share a single thread listening for messages.
 */
public class MultiThreadedMessagePool implements MessageServiceController.ServiceFactory {
    private static final Log _sLog = LogFactory.getLog(MultiThreadedMessagePool.class);

    /** OpenMQ error code C4075: Cannot acknowledge messages due to provider connection failover. Subsequent acknowledge calls will also fail until the application calls session.recover(). */
    private static final String OPENMQ_CLIENT_ACK_FAILOVER_OCCURRED =
            com.sun.messaging.jmq.jmsclient.resources.ClientResources.X_CLIENT_ACK_FAILOVER_OCCURRED;

    private List<MessageListenerDecorator> _messageListenerDecorators;
    private MessageListenerFactory[] _messageListenerFactories;
    private boolean _duplicatesOK = true;
    private int _concurrentConsumers = 1;

    public void setMessageListenerDecorators(List<MessageListenerDecorator> messageListenerDecorators) {
        _messageListenerDecorators = messageListenerDecorators;
    }

    @Required
    public void setMessageListenerFactories(MessageListenerFactory[] messageListenerFactories) {
        _messageListenerFactories = messageListenerFactories;
    }

    public void setDuplicatesOK(boolean duplicatesOK) {
        _duplicatesOK = duplicatesOK; // false means it's better to lose messages than receive them twice
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        _concurrentConsumers = concurrentConsumers;
    }

    /**
     * Creates the message listeners and configures them to listen to their
     * associated Queues and Topics.  Does not start JMS message delivery.
     */
    public MessageServiceController.ServiceInstance createInstance(Connection connection) throws JMSException {
        final Session[] sessions = new Session[_concurrentConsumers];

        // create a statistics tracker for all message activity on this connection
        final StatisticsTracker statisticsTracker = new StatisticsTracker();

        for (int i = 0; i < _concurrentConsumers; i++) {
            // create and initialize a JMS Session that gets its own listener thread.
            final Session session = connection.createSession(false, getAcknowledgeMode());

            MessageListenerPool pool = new MessageListenerPool() {
                @Override
                public void addConsumer(Destination destination, MessageListener listener) throws JMSException {
                    addConsumer(destination, null, listener);
                }
                @Override
                public void addConsumer(Destination destination, String messageSelector, MessageListener listener) throws JMSException {
                    session.createConsumer(destination, messageSelector).setMessageListener(decorateListener(listener, session, destination, statisticsTracker));
                }
                @Override
                public void addDurableSubscriber(Topic topic, String subscriberName, MessageListener listener) throws JMSException {
                    addDurableSubscriber(topic, subscriberName, null, false, listener);
                }
                @Override
                public void addDurableSubscriber(Topic topic, String subscriberName, String messageSelector, boolean nolocal, MessageListener listener) throws JMSException {
                    session.createDurableSubscriber(topic, subscriberName, messageSelector, nolocal).setMessageListener(decorateListener(listener, session, topic, statisticsTracker));
                }
            };

            // register the message listeners with JMS.  once the connection's
            // start method has been called, JMS will start delivering messages
            // to the message listener.
            for (MessageListenerFactory messageListenerFactory : _messageListenerFactories) {
                messageListenerFactory.addListeners(pool);
            }

            sessions[i] = session;
        }

        // return a wrapper around the array of Sessions, so we can close() them later
        return new MessageServiceController.ServiceInstance() {
            @Override
            public long getNumMessagesHandled() {
                return statisticsTracker.getNumMessagesHandled();
            }
            @Override
            public long getCurrentIdleTime() {
                return statisticsTracker.getCurrentIdleTime();
            }
            @Override
            public void waitUntilIdle(long maxWait) throws TimeoutException, InterruptedException {
                statisticsTracker.waitUntilIdle(maxWait);
            }
            @Override
            public void close() {
                for (Session session : sessions) {
                    JmsUtils.closeSession(session);
                }
            }
        };
    }

    private int getAcknowledgeMode() {
        // - if dups are OK, acknowledge when the message listener returns.  if the application
        //   dies during message processing (server is forcibly killed, etc.) then the message
        //   will be redelivered.
        // - if dups aren't ok, acknowledge when the message listener begins.  if the application
        //   dies during message processing then the message will be lost.
        return _duplicatesOK ? Session.AUTO_ACKNOWLEDGE : Session.CLIENT_ACKNOWLEDGE;
    }

    @Override
    public String toString() {
        String[] listenerFactoryNames = new String[_messageListenerFactories.length];
        for (int i = 0; i < _messageListenerFactories.length; i++) {
            listenerFactoryNames[i] = _messageListenerFactories[i].getClass().getSimpleName();
        }
        Arrays.sort(listenerFactoryNames);
        return Arrays.toString(listenerFactoryNames);
    }

    private MessageListener decorateListener(MessageListener listener, Session session, Destination destination, StatisticsTracker statisticsTracker) throws JMSException {
        if (listener instanceof SessionAwareMessageListener) {
            ((SessionAwareMessageListener) listener).setSession(session);
        }

        // run any message listener decorators in order
        if (CollectionUtils.isNotEmpty(_messageListenerDecorators)) {
            for (MessageListenerDecorator decorator : _messageListenerDecorators) {
                listener = decorator.decorateListener(listener, session, destination);
            }
        }

        // if duplicates really really aren't ok, acknowledge receipt of the message immediately
        // this needs to match
        if (!_duplicatesOK) {
            listener = new AcknowledgeImmediatelyListener(listener, session);
        }

        // catch, log and supress all exceptions
        listener = new LoggingMessageListener(listener);

        // absolutely last thing--count messages in progress so we can tell if the pool is idle (not guaranteed--beware race conditions!)
        return new StatisticsTrackerListener(listener, statisticsTracker);
    }

    private static class LoggingMessageListener implements MessageListener {
        private final MessageListener _listener;

        public LoggingMessageListener(MessageListener listener) {
            _listener = listener;
        }

        public void onMessage(final Message message) {
            // catch, log and swallow *all* exceptions.  we don't propagate
            // any exceptions up to the JMS provider since we don't really
            // want the JMS provider to try to resend the message, as it might
            // if it thinks the exception is temporary.
            try {
                LoggingContextUtils.setActivity(JmsSupportUtils.getDestinationName(message));
                LoggingContextUtils.setOnBehalfOf(message.getStringProperty("Client_Name"));
                LoggingContextUtils.setRequestDetail(new LoggingContextData() {
                    @Override
                    protected String constructString() {
                        return message.toString();  // delay calling message.toString() until we need it
                    }
                });

                _listener.onMessage(message);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                _sLog.error(message, t);
            } finally {
                LoggingContextUtils.clear();
            }
        }
    }

    /**
     * Acknowledges a message when it is received instead of when it has finished being
     * processed.  This is useful for listeners where it's a more severe error to receive
     * duplicate messages than it is to lose a message.  Note that the JMS Session needs
     * to be in CLIENT_ACKNOWLEDGE for this listener.
     */
    private class AcknowledgeImmediatelyListener extends AbstractMessageListener {
        private final MessageListener _listener;
        private final Session _session;

        public AcknowledgeImmediatelyListener(MessageListener listener, Session session) {
            _listener = listener;
            _session = session;
        }

        protected void doOnMessage(Message message) throws Exception {
            try {
                message.acknowledge();
            } catch (JMSException jmse) {
                // if we just failed over from one JMS provider to another, OpenMQ will throw
                // an exception when we try to acknowledge a message in client-acknowledge mode.
                // we don't need to do anything special to recover after a failover.  Just call
                // session.recover() to tell OpenMQ that everything is OK, then fall through and
                // continue processing the original message.
                if (OPENMQ_CLIENT_ACK_FAILOVER_OCCURRED.equals(jmse.getErrorCode())) {
                    _session.recover();
                } else {
                    throw jmse;
                }
            }

            // the point of this wrapper class it to make sure we never process a message more
            // than once.  to be safe, always ignore redelivered messages.
            if (message.getJMSRedelivered()) {
                return;
            }

            _listener.onMessage(message);
        }
    }

    private static class StatisticsTrackerListener implements MessageListener {
        private final MessageListener _listener;
        private final StatisticsTracker _statisticsTracker;

        public StatisticsTrackerListener(MessageListener listener, StatisticsTracker statisticsTracker) {
            _listener = listener;
            _statisticsTracker = statisticsTracker;
        }

        @Override
        public void onMessage(Message message) {
            _statisticsTracker.notifyMessageStarted();
            try {
                _listener.onMessage(message);
            } finally {
                _statisticsTracker.notifyMessageFinished();
            }
        }
    }

    private static class StatisticsTracker {
        private long _numMessagesHandled;
        private int _numMessagesInProgress;
        private long _lastTimeInProgressChanged = System.currentTimeMillis();

        /**
         * Make a good faith effort to wait until there are no pending messages for any of the
         * specified destinations.  Because JMS messages are inherently asynchronous, this method
         * can't guarantee that there aren't messages in progress when it returns.
         * <p>
         * Useful for test cases.
         */
        public void waitUntilIdle(long maxWait) throws TimeoutException, InterruptedException {
            boolean waited = false;
            long timeoutTime = System.currentTimeMillis() + maxWait;  // maxWait is the max time we'll wait for the pool to clear
            // require a small amount of idle time before we really consider the message pool to be idle,
            // just in case the messaging system is about to dispatch a new message.
            while (getCurrentIdleTime() < 50) {
                if (waited && System.currentTimeMillis() > timeoutTime) {
                    throw new TimeoutException("Timeout while waiting for JMS message pool to become idle: " + toString());
                }
                Thread.sleep(50);
                waited = true;
            }
        }

        public synchronized long getNumMessagesHandled() {
            return _numMessagesHandled;
        }

        public synchronized long getCurrentIdleTime() {
            if (_numMessagesInProgress == 0) {
                return System.currentTimeMillis() - _lastTimeInProgressChanged; // idle
            } else {
                return 0; // not idle
            }
        }

        public synchronized void notifyMessageStarted() {
            _numMessagesInProgress++;
            _lastTimeInProgressChanged = System.currentTimeMillis();
        }

        public synchronized void notifyMessageFinished() {
            _numMessagesHandled++;
            _numMessagesInProgress--;
            _lastTimeInProgressChanged = System.currentTimeMillis();
        }
    }
}
