package com.bazaarvoice.core.jms.util;

import com.bazaarvoice.core.environment.AbstractApplicationConfig;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.Map;

/**
 * Abstraction layer on top of Java Message Service implementation-specific code.
 */
public abstract class BrokerAdapter {

    public static BrokerAdapter newInstance(String brokerType) {
        if ("sunmq".equals(brokerType)) {
            return new BrokerAdapterSunMQ(false);
        } else if ("sunmq-embedded".equals(brokerType)) {
            return new BrokerAdapterSunMQ(true);
        }
        throw new IllegalStateException("Unrecognized JMS broker type: " + brokerType);
    }

    /** Creates a new instance, pulling the type of broker from the AbstractApplicationConfig object. */
    public static BrokerAdapter newInstance(AbstractApplicationConfig config, String brokerTypeKey) {
        return newInstance(config.getEnvironment().getProperty(brokerTypeKey));
    }

    public abstract Class getPoolingConnectionFactoryType();

    /** Returns a ConnectionFactory that is appropriate for sending messages. */
    public abstract ConnectionFactory createPoolingConnectionFactory(String brokerURL) throws JMSException;

    /** Destroys a ConnectionFactory returned by the createPoolingConnectionFactory method. */ 
    public abstract void destroyPoolingConnectionFactory(ConnectionFactory connectionFactory) throws Exception;

    /** Returns a ConnectionFactory that is appropriate for setting up asynchronous message listeners. */
    public abstract ConnectionFactory createNonPoolingConnectionFactory(String brokerURL, boolean retryConnect) throws JMSException;

    /** Returns an map of JDBC URL to ConnectionFactory for a semi-colon-delimited list of JMS broker URLs. */
    public abstract Map<String, ConnectionFactory> createNonPoolingConnectionFactories(String brokerURLs, boolean retryConnect);

    public abstract Queue createQueue(String queueName) throws JMSException;

    public abstract Topic createTopic(String topicName) throws JMSException;

    /** Some JMS implementations restrict the characters allowed in a message property name. */
    public abstract String sanitizeMessagePropertyName(String propertyName);
}
