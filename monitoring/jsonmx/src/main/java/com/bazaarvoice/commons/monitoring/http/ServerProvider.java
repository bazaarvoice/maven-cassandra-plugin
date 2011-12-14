package com.bazaarvoice.commons.monitoring.http;


import com.bazaarvoice.commons.monitoring.HTTPConnectorServer;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerProvider;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Map;

public class ServerProvider implements JMXConnectorServerProvider {

    @Override
    public JMXConnectorServer newJMXConnectorServer(JMXServiceURL jmxServiceURL, Map<String, ?> stringMap, MBeanServer mBeanServer)
            throws IOException {
        return new HTTPConnectorServer(jmxServiceURL, stringMap, mBeanServer);
    }
}
