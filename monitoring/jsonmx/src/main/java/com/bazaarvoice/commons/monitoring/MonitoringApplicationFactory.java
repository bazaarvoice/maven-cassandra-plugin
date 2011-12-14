package com.bazaarvoice.commons.monitoring;

import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.management.MBeanServer;
import javax.ws.rs.core.Application;
import java.lang.management.ManagementFactory;

public class MonitoringApplicationFactory {

    public static Application createMonitoringApplication() {
        return createMonitoringApplication(null);
    }

    public static Application createMonitoringApplication(MBeanServer server) {
        if (server == null) {
            server = MBeanServerLocator.findMBeanServer();
        }
        MonitoringApplication application = new MonitoringApplication();
        MonitoredObjectProvider provider = new MonitoredObjectProvider(server);
        MonitoredObjectView view = new MonitoredObjectView();
        view.setProvider(provider);
        application.addSingletons(view, new JacksonJsonProvider(MonitoringObjectMapperFactory.createObjectMapper()));
        return application;
    }

    public static ResourceConfig createMonitoringResourceConfig() {
        return createMonitoringResourceConfig(null);
    }

    public static ResourceConfig createMonitoringResourceConfig(MBeanServer server) {
        return new ApplicationAdapter(createMonitoringApplication(server));
    }

}
