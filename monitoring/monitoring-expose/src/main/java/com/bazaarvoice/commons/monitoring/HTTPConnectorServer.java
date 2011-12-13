package com.bazaarvoice.commons.monitoring;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ApplicationAdapter;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HTTPConnectorServer extends JMXConnectorServer {

    private static final Logger _logger = LoggerFactory.getLogger(HTTPConnectorServer.class);

    private JMXServiceURL _url;
    private Map<String, ?> _attributes;
    private HttpServer _grizz;
    private Application _application;

    public HTTPConnectorServer(JMXServiceURL jmxServiceURL, Map<String, ?> environment, MBeanServer mBeanServer) {
        super(mBeanServer);
        this._url = jmxServiceURL;
        this._attributes = new HashMap<String, Object>(environment);
        if (_attributes.containsKey("jerseyApplication")) {
            this._application = (Application) _attributes.remove("jerseyApplication");
        }
        if (mBeanServer == null) {

        } else {
            MonitoringApplication application = new MonitoringApplication();
            MonitoredObjectProvider provider = new MonitoredObjectProvider(mBeanServer);
            MonitoredObjectView view = new MonitoredObjectView();
            view.setProvider(provider);
            application.addSingletons(view, new JacksonJsonProvider(MonitoringObjectMapperFactory.createObjectMapper()));
            this._application = application;
        }
    }

    @Override
    public void start()
            throws IOException {
        if (isActive()) {
            return;
        }
        if (_grizz == null) {
            ApplicationAdapter app = new ApplicationAdapter(_application);
            _grizz = GrizzlyServerFactory.createHttpServer("http://" + _url.getHost() + ":" + _url.getPort() + _url.getURLPath(), app);
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
