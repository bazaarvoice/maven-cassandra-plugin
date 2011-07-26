package com.bazaarvoice.core.jms.util;

import javax.jms.MessageListener;
import javax.jms.Session;

/**
 * An extension of the JMS MessageListener that knows what JMS
 * session it belongs to.
 */
public interface SessionAwareMessageListener extends MessageListener {

    void setSession(Session session);
}
