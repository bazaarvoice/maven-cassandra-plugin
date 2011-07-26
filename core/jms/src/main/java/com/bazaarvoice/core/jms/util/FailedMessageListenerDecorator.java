package com.bazaarvoice.core.jms.util;

import com.bazaarvoice.core.jms.service.AbstractMessageListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

/**
 * If a message fails, forward it to a "...$FAILED" queue so we can look at it or retry it later.
 */
public class FailedMessageListenerDecorator implements MessageListenerDecorator {

    private static final Log _sLog = LogFactory.getLog(FailedMessageListenerDecorator.class);

    private FailedMessageHandler _failedMessageHandler;

    @Required
    public void setFailedMessageHandler(FailedMessageHandler failedMessageHandler) {
        _failedMessageHandler = failedMessageHandler;
    }

    @Override
    public MessageListener decorateListener(final MessageListener listener, final Session session, Destination destination)
            throws JMSException {
        // Skip decoration if handler doesn't care about this destination
        if (!_failedMessageHandler.register(destination)) {
            return listener;
        }

        return new AbstractMessageListener() {
            @Override
            protected void doOnMessage(Message message)
                    throws Throwable {
                try {
                    listener.onMessage(message);
                } catch (Throwable t) {
                    try {
                        _failedMessageHandler.handle(message, session, t);
                    } catch (Throwable pt) {
                        _sLog.error("Exception during failure processing of message on queue " + JmsSupportUtils.getDestinationName(message) + ": " + pt, pt);
                    }
                    throw t;
                }
            }
        };
    }
}
