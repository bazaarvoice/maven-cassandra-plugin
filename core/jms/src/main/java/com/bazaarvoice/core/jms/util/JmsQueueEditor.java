package com.bazaarvoice.core.jms.util;

import com.bazaarvoice.core.environment.AbstractApplicationConfig;
import org.apache.commons.lang.StringUtils;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.beans.PropertyEditorSupport;

/**
 * Spring property editor for JMS Queues to automatically convert
 * String queue names to JMS Queue objects.
 */
public class JmsQueueEditor extends PropertyEditorSupport {

    private final BrokerAdapter _brokerAdapter;

    public JmsQueueEditor(AbstractApplicationConfig config, String brokerTypeKey) {
        // create a BrokerAdapter using the AbstractApplicationConfig configuration setting.
        // we can't wire the BrokerAdapter directly using Spring because PropertyEditor
        // objects are created before Spring ${property} expansions are evaluated, so the
        // BrokerAdapter can't be created by Spring before the JmsQueueEditor is created.
        _brokerAdapter = BrokerAdapter.newInstance(config, brokerTypeKey);
    }

    /**
     * Converts String values to javax.jms.Queue.
     */
    @Override
    public void setAsText(String text) {
        if (StringUtils.isNotBlank(text)) {
            Queue queue;
            try {
                queue = _brokerAdapter.createQueue(text);
            } catch (JMSException e) {
                throw new IllegalArgumentException("Error creating queue: " + text, e);
            }
            setValue(queue);
        } else {
            setValue(null);
        }
    }
}
