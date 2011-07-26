package com.bazaarvoice.core.jms.util;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;

public interface MessageListenerDecorator {

    MessageListener decorateListener(MessageListener listener, Session session, Destination destination) throws JMSException;
}
