package com.bazaarvoice.core.jms.util;

import javax.jms.JMSException;

/**
 * Interface for creating JMS message sink classes that listen on a
 * JMS queue or topic and process messages.
 */
public interface MessageListenerFactory {

    /**
     * Adds a set of MessageListener message sink classes to a pool
     * of message listeners.  The messages will be dispatched according
     * to the semantics of the pool implementation.
     */
    void addListeners(MessageListenerPool pool) throws JMSException;
}
