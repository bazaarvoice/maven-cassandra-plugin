package com.bazaarvoice.core.jms.util;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jms.support.JmsUtils;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.Map;

/**
 * Utility class for browsing (not consuming) messages waiting in a queue on one or
 * more message brokers.  This is useful for debugging and/or monitoring a server.
 */
public class MessageBrowser {

    private BrokerAdapter _messageBrokerAdapter;
    private Map<String, ConnectionFactory> _messageBrokers;

    @Required
    public void setMessageBrokerAdapter(BrokerAdapter messageBrokerAdapter) {
        _messageBrokerAdapter = messageBrokerAdapter;
    }

    @Required
    public void setMessageBrokerURLs(String messageBrokerURLs) throws JMSException {
        // connect to the broker with retry disabled, so if the broker is down we'll get an exception and
        // continue.  it is important in a MessageBrowser that, if the broker is off the network, we don't
        //  hang forever waiting for it to appear.
        _messageBrokers = _messageBrokerAdapter.createNonPoolingConnectionFactories(messageBrokerURLs, false);
    }

    public void browse(MessageBrowserCallback callback) throws JMSException {
        doInJms(callback, false, Session.AUTO_ACKNOWLEDGE);
    }

    public void receive(MessageBrowserCallback callback, int acknowledgeMode) throws JMSException {
        doInJms(callback, true, acknowledgeMode);
    }

    private void doInJms(MessageBrowserCallback callback, boolean startListeners, int acknowledgeMode) throws JMSException {
        for (Map.Entry<String, ConnectionFactory> entry : _messageBrokers.entrySet()) {
            String brokerURL = entry.getKey();
            ConnectionFactory connectionFactory = entry.getValue();

            // connect to the JMS broker, making a note if the broker is down
            Connection connection;
            try {
                connection = connectionFactory.createConnection();
            } catch (JMSException e) {
                callback.connectionFailed(brokerURL, e);
                continue;
            }
            try {
                if (startListeners) {
                    connection.start();
                }
                Session session = connection.createSession(false, acknowledgeMode);
                try {

                    // invoke the callback to let it gather information.  the callback should
                    // collect whatever information it needs on all the queues it's interested in.
                    callback.doInJms(session);

                } finally {
                    JmsUtils.closeSession(session);
                }
            } finally {
                JmsUtils.closeConnection(connection);
            }
        }
    }
}
