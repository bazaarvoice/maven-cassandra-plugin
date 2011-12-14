package com.bazaarvoice.commons.monitoring;

import org.glassfish.grizzly.http.server.NetworkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Hashtable;

public class ApplicationMonitor {

    private static final Logger _logger = LoggerFactory.getLogger(ApplicationMonitor.class);

    private static final int DEFAULT_PORT = 9999;
    private static final String NAME = "JmxHttpConnectorServer";
    private static final String NAME_KEY = "name";
    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";

    private String _host;
    private int _port;
    private MBeanServer _server;
    private Application _jerseyApp;
    private HTTPConnectorServer _connector;

    public ApplicationMonitor() {
        this(NetworkListener.DEFAULT_NETWORK_HOST, DEFAULT_PORT, null);
    }

    public ApplicationMonitor(int port) {
        this(NetworkListener.DEFAULT_NETWORK_HOST, port, null);
    }

    public ApplicationMonitor(String host, int port) {
        this(host, port, null);
    }

    public ApplicationMonitor(String url) {
        this(url, null);
    }

    public ApplicationMonitor(MBeanServer server) {
        this(NetworkListener.DEFAULT_NETWORK_HOST, DEFAULT_PORT, server);
    }

    public ApplicationMonitor(int port, MBeanServer server) {
        this(NetworkListener.DEFAULT_NETWORK_HOST, port, server);
    }

    public ApplicationMonitor(String host, int port, MBeanServer server) {
        // Will throw exception if host or port are not valid
        URI.create("http://" + host + ":" + port);
        this._server = server;
        this._host = host;
        this._port = port;
    }

    public ApplicationMonitor(String url, MBeanServer server) {
        if(url.split(":").length > 2) {
            throw new IllegalArgumentException("Host must not contain scheme");
        }
        this._server = server;
        URI uri = URI.create("http://" + url);
        this._host = uri.getHost();
        if(this._host == null) {
            this._host = NetworkListener.DEFAULT_NETWORK_HOST;
        }
        this._port = uri.getPort();
        if(this._port < 0) {
            this._port = DEFAULT_PORT;
        }
    }

    public void setHost(String host) {
        // Will throw exception if host is not valid
        URI.create("http://" + host + ":" + _port);
        _host = host;
    }

    public void setPort(int port) {
        _port = port;
    }

    public void setServer(MBeanServer server) {
        _server = server;
    }

    public boolean startMonitor() {
        if (_connector != null) {
            throw new IllegalStateException("can't restart application monitor");
        }
        _logger.info("Attempting to start monitoring server on {}:{}", _host, _port);
        if (_server == null) {
            _server = MBeanServerLocator.findMBeanServer();
        }
        ObjectName objectName = buildObjectName();
        try {
            _connector = new HTTPConnectorServer(_host, _port, _server, _jerseyApp);
            _server.registerMBean(_connector, objectName);
            _connector.start();
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Invalid host name");
        }
        catch (InstanceAlreadyExistsException e) {
            _logger.warn("Monitoring server on {}:{} already running, not starting second instance", _host, _port);
            return false;
        }
        catch (Exception e) {
            _logger.error("Unable to start monitoring server", e);
            if(e instanceof IOException) {
                try {
                    _server.unregisterMBean(objectName);
                    if(_connector.isActive()) {
                        _connector.stop();
                    }
                } catch (Exception e2) {
                    _logger.error("Unable to unregister monitoring server", e2);
                }
            }
            return false;
        }
        return true;
    }

    private ObjectName buildObjectName() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(NAME_KEY, NAME);
        properties.put(HOST_KEY, _host);
        properties.put(PORT_KEY, String.valueOf(_port));
        try {
            return ObjectName.getInstance(this.getClass().getPackage().getName(), properties);
        }
        catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid host name");
        }
    }

}
