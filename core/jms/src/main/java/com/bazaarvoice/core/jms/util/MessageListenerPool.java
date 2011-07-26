package com.bazaarvoice.core.jms.util;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Topic;

/**
 * A pool of MessageListener objects listening to various JMS queues and topics.
 */
public interface MessageListenerPool {

	void addConsumer(Destination destination, MessageListener listener) throws JMSException;

	void addConsumer(Destination destination, String messageSelector, MessageListener listener) throws JMSException;

	void addDurableSubscriber(Topic topic, String subscriberName, MessageListener listener) throws JMSException;

	void addDurableSubscriber(Topic topic, String subscriberName, String messageSelector, boolean nolocal, MessageListener listener) throws JMSException;
}
