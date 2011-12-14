package com.bazaarvoice.commons.monitoring;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class HTTPConnectorServer extends JMXConnectorServer {

    private static final Logger _logger = LoggerFactory.getLogger(HTTPConnectorServer.class);

    private JMXServiceURL _url;
    private Map<String, ?> _attributes;
    private HttpServer _grizz;
    private ResourceConfig _application;

    public HTTPConnectorServer(JMXServiceURL jmxServiceURL, Map<String, ?> environment, MBeanServer mBeanServer) {
        super(mBeanServer);
        this._url = jmxServiceURL;
        this._attributes = new HashMap<String, Object>(environment);
        if (_attributes.containsKey("jerseyApplication")) {
            Object application = _attributes.remove("jerseyApplication");
            if(application != null && application instanceof Application) {
                if(application instanceof ResourceConfig) {
                    this._application = (ResourceConfig) application;
                }
                else {
                    this._application = new ApplicationAdapter((Application) application);
                }
            }
        }
    }

    public HTTPConnectorServer(String host, int port, MBeanServer mBeanServer, Application application)
            throws MalformedURLException {
        super(mBeanServer);
        _url = new JMXServiceURL("http", host, port);
        _attributes = new HashMap<String, Object>();
        if(application != null && application instanceof ResourceConfig) {
            this._application = (ResourceConfig) application;
        }
    }

    public HTTPConnectorServer(String host, int port)
            throws MalformedURLException {
        this(host, port, null, null);
    }

    @Override
    public void start()
            throws IOException {
        if (isActive()) {
            return;
        }
        if(_application == null) {
            _application = MonitoringApplicationFactory.createMonitoringResourceConfig(getMBeanServer());
        }
        if (_grizz == null) {
            _grizz = GrizzlyServerFactory.createHttpServer("http://" + _url.getHost() + ":" + _url.getPort() + _url.getURLPath(), _application);
        }
        _logger.info("Starting monitoring server on port {}", _url.getPort());
        _grizz.start();
    }

    @Override
    public void stop()
            throws IOException {
        if (isActive()) {
            _grizz.stop();
        }
    }

    @Override
    public boolean isActive() {
        return _grizz != null && _grizz.isStarted();
    }

    @Override
    public JMXServiceURL getAddress() {
        return _url;
    }

    // TODO: Filter attributes that get returned?
    @Override
    public Map<String, ?> getAttributes() {
        return _attributes;
    }

}
