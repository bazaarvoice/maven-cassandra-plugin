package com.bazaarvoice.core.jms.util;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Interface for classes that take some action when a message handler
 * throws an uncaught exception.
 */
public interface FailedMessageHandler {

    boolean register(Destination destination) throws JMSException;

    void handle(Message message, Session session, Throwable t) throws JMSException;
}
