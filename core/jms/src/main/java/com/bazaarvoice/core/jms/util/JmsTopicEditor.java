package com.bazaarvoice.core.jms.util;

import com.bazaarvoice.core.environment.AbstractApplicationConfig;
import org.springframework.util.StringUtils;

import javax.jms.JMSException;
import javax.jms.Topic;
import java.beans.PropertyEditorSupport;

/**
 * Spring property editor for JMS Topic to automatically convert
 * String topic names to JMS Topic objects.
 */
public class JmsTopicEditor extends PropertyEditorSupport {

    private final BrokerAdapter _brokerAdapter;

    public JmsTopicEditor(AbstractApplicationConfig config, String brokerTypeKey) {
        // create a BrokerAdapter using the AbstractApplicationConfig configuration setting.
        // we can't wire the BrokerAdapter directly using Spring because PropertyEditor
        // objects are created before Spring ${property} expansions are evaluated, so the
        // BrokerAdapter can't be created by Spring before the JmsTopicEditor is created. 
        _brokerAdapter = BrokerAdapter.newInstance(config, brokerTypeKey);
    }

    /**
     * Converts String values to javax.jms.Topic.
     */
    public void setAsText(String text) {
        if (StringUtils.hasText(text)) {
            Topic topic;
            try {
                topic = _brokerAdapter.createTopic(text);
            } catch (JMSException e) {
                throw new IllegalArgumentException("Error creating topic: " + text, e);
            }
            setValue(topic);
        } else {
            setValue(null);
        }
    }
}
