package com.bazaarvoice.core.jms.util;

import com.bazaarvoice.core.environment.AbstractApplicationConfig;
import com.sun.messaging.jmq.jmsserver.BrokerProcess;
import com.sun.messaging.jmq.jmsservice.JMSBroker;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Sun Java System Message Queue: http://mq.dev.java.net and http://www.sun.com/software/products/message_queue/index.xml
 */
public class BrokerAdapterSunMQ extends BrokerAdapter {
    private static final Log _sLog = LogFactory.getLog(BrokerAdapterSunMQ.class);

    private static final String OPENMQ_DEFAULT_CONFIG_RESOURCE = "BrokerAdapterSunMQ.default.properties";
    private static final String EMBEDDED_BROKER_SERVER = "localhost";
    private static final int EMBEDDED_BROKER_PORT = 7676;

    private static boolean _sEmbeddedBrokerStarted;

    public BrokerAdapterSunMQ(boolean startEmbeddedBroker) {
        if (startEmbeddedBroker) {
            try {
                startEmbeddedBroker();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Class getPoolingConnectionFactoryType() {
        return org.springframework.jms.connection.SingleConnectionFactory.class;
    }

    @Override
    public ConnectionFactory createPoolingConnectionFactory(String brokerURL) throws JMSException {
        ConnectionFactory connectionFactory = createNonPoolingConnectionFactory(brokerURL, true);
        return new org.springframework.jms.connection.SingleConnectionFactory(connectionFactory);
    }

    @Override
    public void destroyPoolingConnectionFactory(ConnectionFactory connectionFactory) throws Exception {
        ((org.springframework.jms.connection.SingleConnectionFactory) connectionFactory).destroy();
    }

    @Override
    public Map<String, ConnectionFactory> createNonPoolingConnectionFactories(String brokerURLs, boolean retryConnect) {
        String[] messageBrokerURLsArray = brokerURLs.split(";");
        Map<String, ConnectionFactory> factoryMap = new LinkedHashMap<String, ConnectionFactory>();
        for (String brokerURL : messageBrokerURLsArray) {
            brokerURL = StringUtils.trimToNull(brokerURL);
            if (brokerURL != null && !factoryMap.containsKey(brokerURL)) {
                ConnectionFactory factory;
                try {
                    factory = createNonPoolingConnectionFactory(brokerURL, retryConnect);
                } catch (JMSException e) {
                    throw new IllegalStateException("Invalid OpenMQ broker URL: " + brokerURL, e);
                }
                factoryMap.put(brokerURL, factory);
            }
        }
        return factoryMap;
    }

    @Override
    public ConnectionFactory createNonPoolingConnectionFactory(String brokerURL, boolean retryConnect) throws JMSException {
        com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
        // set the list of 1 or more brokers to connect to
        connectionFactory.setProperty(com.sun.messaging.ConnectionConfiguration.imqAddressList, brokerURL);
        // upon connection, loop through the list of brokers once (retryConnect=false) or forever
        // (retryConnect=true) trying to find a live broker to connect to.
        connectionFactory.setProperty(com.sun.messaging.ConnectionConfiguration.imqAddressListIterations, retryConnect ? "-1" : "1");
        // once a connection has been established, if the connection goes away then try to re-establish
        // the connection by looping through the brokers in the imqAddressList.
        connectionFactory.setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectEnabled, "true");
        // message delivery performance isn't an issue.  improve reliability by limiting message prefetch to 1 to
        // ensure messages are spread around evenly and a slow/stalled web server will affect as few messages as possible.
        connectionFactory.setProperty(com.sun.messaging.ConnectionConfiguration.imqConsumerFlowLimit, "1"); // default is 1000
        connectionFactory.setProperty(com.sun.messaging.ConnectionConfiguration.imqConsumerFlowThreshold, "0"); // default is 50 (%)
        // ignore future changes to System.properties()
        connectionFactory.setReadOnly();
        return connectionFactory;
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException {
        return new com.sun.messaging.Queue(sanitizeJavaIdentifier(queueName));
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException {
        return new com.sun.messaging.Topic(sanitizeJavaIdentifier(topicName));
    }

    @Override
    public String sanitizeMessagePropertyName(String propertyName) {
        return sanitizeJavaIdentifier(propertyName);
    }

    /** Sun MQ requires that destination names and message header names must be valid Java identifiers. */
    private String sanitizeJavaIdentifier(String name) {
        // Valid Java identifiers look something like [a-Z_$][a-Z0-9_$]*.  Change the characters
        // we use that violate these rules ('-' in hostname prefixes and '.' everywhere).
        return name.replace('-', '$').replace('.', '_');
    }

    /**
     * Starts a local in-process instance of the OpenMQ broker.  There are two
     * major pre-reqs for running the broker in-process:
     *  1. You must have the following jars in the classpath:
     *       imqbroker.jar
     *       imqjmx.jar
     *       imqutil.jar
     *  2. You must have a skeleton $IMQ_HOME directory structure containing
     *     (at least) a default configuration file.  The startEmbeddedBroker()
     *     method creates the directory under "$BAZAARVOICE_HOME/openmq" and
     *     populates it with a properties file taken from the OpenMQ distribution. 
     */
    public static synchronized void startEmbeddedBroker() throws IOException {
        if (_sEmbeddedBrokerStarted) {
            return;
        }

        _sEmbeddedBrokerStarted = true;

        // Check if the port is in use
        try {
            new Socket(EMBEDDED_BROKER_SERVER, EMBEDDED_BROKER_PORT).close();

            // If the port is already in use, another process may have started an embedded broker.
            // Let the app try to start with it.
            _sLog.warn("Attempting to reuse embedded broker on port " + EMBEDDED_BROKER_PORT + ". If the process that started that broker is terminated, " +
                    "this process will be unable to access JMS.");

            return;
        } catch (IOException e) {
            // ignore
        }

        // find the home directory
        File homeDirectory = AbstractApplicationConfig.getHomeDirectoryPath();
        if (homeDirectory == null) {
            throw new IllegalArgumentException("AbstractApplicationConfig has not been initialized.");
        }

        // hard code in-process broker configuration properties
        File imqHome = new File(homeDirectory, "openmq");
        String brokerName = "imqbroker";
        String listenHost = "localhost";

        // setup a minimal directory structure under /home/bazaarvoice/openmq
        // and copy in a default configuration file.
        if (!imqHome.exists() && !imqHome.mkdir()) {
            throw new IOException("Unable to create OpenMQ home: " + imqHome);
        }
        File imqProps = new File(imqHome, "lib/props/broker/default.properties");
        copyResource(OPENMQ_DEFAULT_CONFIG_RESOURCE, imqProps);

        // configure and start the embedded broker
        JMSBroker broker = BrokerProcess.getBrokerProcess();
        Properties brokerProperties = broker.parseArgs(new String[] {
                "-imqhome", imqHome.getPath(),
                "-port", Integer.toString(EMBEDDED_BROKER_PORT),
                "-name", brokerName,
                "-silent",
        });
        brokerProperties.setProperty("imq.hostname", listenHost);
        int code = broker.start(true, brokerProperties, null);

        if (code != 0) {
            throw new IllegalStateException("OpenMQ broker startup failed with error code: " + code);
        }
    }

    public static synchronized void stopEmbeddedBroker() {
        if (_sEmbeddedBrokerStarted) {
            BrokerProcess.getBrokerProcess().stop(true);
            _sEmbeddedBrokerStarted = false;
        }
    }

    private static void copyResource(String sourceResourceName, File destinationFile) throws IOException {
        destinationFile.getParentFile().mkdirs();
        InputStream in = BrokerAdapterSunMQ.class.getResourceAsStream(sourceResourceName);
        OutputStream out = new FileOutputStream(destinationFile);
        IOUtils.copyLarge(in, out);
        out.close();
        in.close();
    }
}
