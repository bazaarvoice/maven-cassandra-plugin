package com.bazaarvoice.core.jms.util;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Callback for the {@link MessageBrowser} class used to browse JMS messages
 * waiting in a queue on one or more message brokers.
 */
public interface MessageBrowserCallback {

    /**
     * Collects message information from the message broker and returns.
     * Implementations of this message usually use the JMS Session to
     * create QueueBrowsers to enumerate the messages waiting in a
     * specific set of Queues.
     */
    void doInJms(Session session) throws JMSException;

    /**
     * Notification that the broker at the specified URL was unavailable
     * and could not be browsed. 
     */
    void connectionFailed(String brokerURL, JMSException connectionException);
}
