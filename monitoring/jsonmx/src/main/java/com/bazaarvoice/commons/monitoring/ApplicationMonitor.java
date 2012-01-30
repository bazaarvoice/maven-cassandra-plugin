package com.bazaarvoice.commons.monitoring;

import com.bazaarvoice.commons.monitoring.core.HTTPConnectorServer;
import com.bazaarvoice.commons.monitoring.core.MBeanServerUtils;
import com.bazaarvoice.commons.monitoring.core.MonitoringApplication;
import com.bazaarvoice.commons.monitoring.core.MonitoringApplicationFactory;
import com.bazaarvoice.commons.monitoring.transform.MonitoringDataTransform;
import com.bazaarvoice.commons.monitoring.transform.ObjectNameTransformFactory;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Entry point for starting up application monitoring with its own HTTP server listening on a single port.
 * Defaults to port 9999 if no port is specified. The {@link #startMonitor() startMonitor} method should be called to start the
 * server listening and register it with the MBeanServer. The {@link #stopMonitor() stopMonitor} method should be called to stop
 * the server and unbind from the port when the application is shutting down. If using Spring, the monitoring server
 * can be started by adding a bean like the following to your application context:
 * <p>{@code
 * <bean class="com.bazaarvoice.commons.monitoring.ApplicationMonitor" init-method="startMonitor" destroy-method="stopMonitor"/>
 * }
 * </p>
 * {@link MonitoringDataTransform MonitoringDataTransform}s can be registered via calls to the
 * {@link #setTransforms(java.util.List) setTransforms} and {@link #setTransformsFromMaps(java.util.List) setTransformsFromMaps} methods.
 */
public final class ApplicationMonitor {

    private static final Logger _logger = LoggerFactory.getLogger(ApplicationMonitor.class);

    /**
     * Port the monitoring server starts on if none is specified.
     */
    public static final int DEFAULT_PORT = 9999;
    private static final String NAME = "JmxHttpConnectorServer";
    private static final String NAME_KEY = "name";
    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";

    private final String _host;
    private final int _port;
    private final MBeanServer _server;
    private final MonitoringApplication _jerseyApp;
    private HTTPConnectorServer _connector;
    private ObjectNameTransformFactory _transformFactory;

    public ApplicationMonitor() {
        this(DEFAULT_PORT, null);
    }

    public ApplicationMonitor(int port) {
        this(port, null);
    }

    public ApplicationMonitor(MBeanServer server) {
        this(DEFAULT_PORT, server);
    }

    /**
     * @param port the port the spawned HTTP server should bind to when started.
     * @param server the {@code MBeanServer} to query for JMX data.
     * @throws IllegalArgumentException if {@code port} is negative.
     */
    public ApplicationMonitor(int port, MBeanServer server) {
        if (port < 0) {
            throw new IllegalArgumentException("Port must be non-negative.");
        }
        this._port = port;
        this._host = NetworkListener.DEFAULT_NETWORK_HOST;
        if (server == null) {
            this._server = MBeanServerUtils.findMBeanServer();
        } else {
            this._server = server;
        }
        this._jerseyApp = MonitoringApplicationFactory.createMonitoringApplication(_server);
    }

    /**
     * Sets the {@code ObjectNameTransformFactory} to be used when
     * creating {@link com.bazaarvoice.commons.monitoring.transform.ObjectNameTransform ObjectNameTransform}s. If not set,
     * a default one will be used.
     * @param transformFactory the factory to be used for creating transforms from parameter maps.
     */
    public void setTransformFactory(ObjectNameTransformFactory transformFactory) {
        _transformFactory = transformFactory;
    }

    /**
     * Adds {@link com.bazaarvoice.commons.monitoring.transform.ObjectNameTransform ObjectNameTransform}s. The method name starts with "{@code set}" for
     * convenience when working with Spring, but the current list of transforms is appended to, not overwritten. Calls to this and
     * {@link #setTransforms(java.util.List) setTransforms} may be interwoven as necessary to get transforms in the desired order.
     * <p>Transforms added with this method are created via calls to the
     * {@link #setTransformFactory(com.bazaarvoice.commons.monitoring.transform.ObjectNameTransformFactory) TransformFactory}'s
     * {@link ObjectNameTransformFactory#create(java.util.Map) create} method.
     * @param transforms a {@code List} of {@code Map}s containing specifications of transforms. Each Map should contain the key "{@code type}" with.
     * a value identifying the type of transform, then other keys and values appropriate for that transform.
     */
    public void setTransformsFromMaps(List<Map<String, String>> transforms) {
        for (Map<String, String> transform : transforms) {
            _jerseyApp.getProvider().addTransform(_transformFactory.create(transform));
        }
    }

    /**
     * Adds {@link MonitoringDataTransform}s. If {@link com.bazaarvoice.commons.monitoring.transform.ObjectNameTransform}s are desired,
     * it may be preferable to call {@link #setTransformsFromMaps(java.util.List) setTransformsFromMaps} instead. The method name starts with "set" for
     * convenience when working with Spring, but the current list of transforms is appended to, not overwritten. Calls to this and
     * {@code setTransformsFromMaps} may be interwoven as necessary to get transforms in the desired order.
     * @param transforms a {@code List} of transforms to modify the outputted data.
     */
    public void setTransforms(List<MonitoringDataTransform> transforms) {
        for (MonitoringDataTransform transform : transforms) {
            _jerseyApp.getProvider().addTransform(transform);
        }
    }

    /**
     * Starts the monitoring server and registers it with the {@link MBeanServer}. Should only be called once per instance of {@code ApplicationMonitor}.
     * @return {@code true} if the server started successfully, {@code false} otherwise.
     * @throws IllegalStateException if this {@code startMonitor} is called multiple times for a single instance.
     * @throws RuntimeException if the host the server tries to start on is somehow invalid.
     */
    public boolean startMonitor() {
        if (_connector != null) {
            throw new IllegalStateException("Can't restart application monitor");
        }
        _logger.info("Attempting to start monitoring server on port {}", _port);
        ObjectName objectName = buildObjectName();
        try {
            _connector = new HTTPConnectorServer(_host, _port, _server, _jerseyApp);
            _server.registerMBean(_connector, objectName);
            _connector.start();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid host name", e);
        } catch (InstanceAlreadyExistsException e) {
            _logger.warn("Monitoring server on {}:{} already running, not starting second instance", _host, _port);
            return false;
        } catch (Exception e) {
            _logger.error("Unable to start monitoring server", e);
            if (e instanceof IOException) {
                try {
                    _server.unregisterMBean(objectName);
                    if (_connector.isActive()) {
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

    /**
     * Stops the monitoring server, unbinds it from its port, and unregisters it from the {@link MBeanServer}.
     * @throws InstanceNotFoundException if the server is unregistered by another thread after this method checks its registration status, but before
     * it tries to unregister. Should never happen, in theory.
     * @throws IOException if the server does not shut down properly.
     * @throws MBeanRegistrationException if the {@code MBeanServer} does not succeed in unregistering.
     */
    public void stopMonitor()
            throws InstanceNotFoundException, IOException, MBeanRegistrationException {
        try {
            ObjectName objectName = buildObjectName();
            if (_server.isRegistered(objectName)) {
                _server.unregisterMBean(objectName);
            }
        } finally {
            if (_connector != null) {
                _connector.stop();
            }
        }
    }

    /**
     * Stops and unregisters the server in case nothing else has.
     */
    @Override
    protected void finalize()
            throws Throwable {
        try {
            stopMonitor();
        } finally {
            super.finalize();
        }
    }

    private ObjectName buildObjectName() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(NAME_KEY, NAME);
        properties.put(HOST_KEY, _host);
        properties.put(PORT_KEY, String.valueOf(_port));
        try {
            return ObjectName.getInstance(this.getClass().getPackage().getName(), properties);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid host name");
        }
    }

}
