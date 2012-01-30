package com.bazaarvoice.commons.monitoring.core;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
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

/**
 * Connector to output JMX data over HTTP in Json.
 * Will usually be spawned automatically from an
 * {@link com.bazaarvoice.commons.monitoring.ApplicationMonitor ApplicationMonitor}
 * or using {@link javax.management.remote.JMXConnectorServerFactory#newJMXConnectorServer(javax.management.remote.JMXServiceURL, java.util.Map, javax.management.MBeanServer) JMXConnectorServerFactory.newJMXConnectorServer}.
 */
public class HTTPConnectorServer extends JMXConnectorServer {

    private static final Logger _logger = LoggerFactory.getLogger(HTTPConnectorServer.class);

    private final JMXServiceURL _url;
    private final Map<String, ?> _attributes;
    private HttpServer _grizz;
    private ResourceConfig _application;
    private boolean _active;

    public HTTPConnectorServer(JMXServiceURL jmxServiceURL, Map<String, ?> environment, MBeanServer mBeanServer) {
        super(mBeanServer);
        this._url = jmxServiceURL;
        this._attributes = new HashMap<String, Object>(environment);
        if (_attributes.containsKey("jerseyApplication")) {
            Object application = _attributes.remove("jerseyApplication");
            if (application != null && application instanceof Application) {
                if (application instanceof ResourceConfig) {
                    this._application = (ResourceConfig) application;
                }
                else {
                    this._application = new ApplicationAdapter((Application) application);
                }
            }
        }
        this._active = false;
    }

    public HTTPConnectorServer(String host, int port, MBeanServer mBeanServer, Application application)
            throws MalformedURLException {
        super(mBeanServer);
        _url = new JMXServiceURL("http", host, port);
        _attributes = new HashMap<String, Object>();
        if (application != null) {
            if (application instanceof ResourceConfig) {
                this._application = (ResourceConfig) application;
            }
            else {
                this._application = new ApplicationAdapter(application);
            }
        }
        this._active = false;
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
        this._active = true;
        if (_application == null) {
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
        if (_grizz != null) {
            _logger.info("Stopping monitoring server on port {}", _url.getPort());
            if (_grizz.isStarted()) {
                _grizz.stop();
            }
            for (NetworkListener nl : _grizz.getListeners()) {
                if (nl.isStarted()) {
                    nl.stop();
                }
                nl.getTransport().stop();
                nl.getTransport().unbindAll();
            }
            this._active = false;
        }
        else {
            this._active = false;
        }
    }

    @Override
    public boolean isActive() {
        return _active;
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
