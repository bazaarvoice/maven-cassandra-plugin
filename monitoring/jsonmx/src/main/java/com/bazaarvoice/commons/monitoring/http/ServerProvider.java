package com.bazaarvoice.commons.monitoring.http;


import com.bazaarvoice.commons.monitoring.core.HTTPConnectorServer;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerProvider;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Map;

/**
 * Convenience class for spawning {@link HTTPConnectorServer} instances using
 * {@link javax.management.remote.JMXConnectorServerFactory}.
 * The {@link JMXServiceURL} should be of the form:
 * <blockquote><code>service:jmx:http://<em>[host[</em>:<em>port]][url-path]</em></code></blockquote>
 * To make the {@code JMXConnectorServerFactory} recognize this as a service provider for http, call
 * {@link javax.management.remote.JMXConnectorServerFactory#newJMXConnectorServer(javax.management.remote.JMXServiceURL, java.util.Map, javax.management.MBeanServer) newJMXConnctorServer}
 * under one of the following conditions:
 * <ul>
 *     <li>The environment map containing key {@code jmx.remote.protocol.provider.pkgs} with value {@code com.bazaarvoice.commons.monitoring}</li>
 *     <li>The file {@code META-INF/services/javax.management.remote.JMXConnectorServerProvider} containing line {@code com.bazaarvoice.commons.monitoring.http.ServerProvider}</li>
 * </ul>
 */
public class ServerProvider implements JMXConnectorServerProvider {

    @Override
    public JMXConnectorServer newJMXConnectorServer(JMXServiceURL jmxServiceURL, Map<String, ?> stringMap, MBeanServer mBeanServer)
            throws IOException {
        return new HTTPConnectorServer(jmxServiceURL, stringMap, mBeanServer);
    }
}
