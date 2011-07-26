package com.bazaarvoice.core.jms.util;

import org.springframework.beans.factory.annotation.Required;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;

/**
 * Simple implementation of MessageListenerFactory adapts a single MessageListener
 * for use by a MultiThreadedMessagePool object.
 */
public class MessageListenerAdapter implements MessageListenerFactory {

    private Destination _destination;
    private MessageListener _messageListener;

    @Required
    public void setDestination(Destination destination) {
        _destination = destination;
    }

    @Required
    public void setMessageListener(MessageListener messageListener) {
        _messageListener = messageListener;
    }

    @Override
    public void addListeners(MessageListenerPool pool) throws JMSException {
        pool.addConsumer(_destination, _messageListener);
    }
}
