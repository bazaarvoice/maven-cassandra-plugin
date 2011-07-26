package com.bazaarvoice.core.jms.util;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * Bazaarvoice JMS utils.
 */
public class JmsSupportUtils {

    public static String getDestinationName(Message message) {
        try {
            Destination destination = message.getJMSDestination();
            if (destination instanceof Queue) {
                return "Queue " + ((Queue) destination).getQueueName();
            } else {
                return "Topic " + ((Topic) destination).getTopicName();
            }
        } catch (JMSException e) {
            return "<unknown>";
        }
    }

}
