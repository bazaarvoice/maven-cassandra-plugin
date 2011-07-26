package com.bazaarvoice.core.jms.service;

import com.google.common.base.Throwables;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Abstract base class for MessageListener allows message processing to
 * throw checked exceptions.
 */
public abstract class AbstractMessageListener implements MessageListener {

    /**
     * Subclasses must implement this method.
     */
    protected abstract void doOnMessage(Message message) throws Throwable;

    public final void onMessage(final Message message) {
        try {
            doOnMessage(message);
        } catch (Throwable t) {
            throw Throwables.propagate(t);
        }
    }
}
