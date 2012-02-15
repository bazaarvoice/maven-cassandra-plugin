package com.bazaarvoice.commons.monitoring.core;

import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;

import javax.management.MBeanServer;

/**
 * Factory class for creating fully functional {@link MonitoringApplication} instances.
 * It will have a {@link MonitoredObjectView} resource singleton
 * and a {@link JacksonJsonProvider} provider singleton registered.
 */
public final class MonitoringApplicationFactory {

    private MonitoringApplicationFactory() {
    }

    /**
     * @param server {@link MBeanServer} to get JMX data from.
     * @return a {@link MonitoringApplication} that has all necessary singletons to produce Json data from JMX data.
     */
    public static MonitoringApplication createMonitoringApplication(MBeanServer server) {
        MonitoredObjectProvider provider = new MonitoredObjectProvider(server);
        MonitoringApplication application = new MonitoringApplication(provider);
        ObjectMapper mapper = MonitoringObjectMapperFactory.createObjectMapper();
        ObjectGraphWalkerFactory walkerFactory = new ObjectGraphWalkerFactory(mapper);
        MonitoredObjectView view = new MonitoredObjectView(provider, walkerFactory);
        JacksonJsonProvider jsonProvider = new JacksonJsonProvider(mapper);
        application.addSingletons(view, jsonProvider);
        return application;
    }

    /**
     * Calls {@link #createMonitoringApplication(javax.management.MBeanServer) createMonitoringApplication} and wraps the result in a {@link ResourceConfig}.
     * @param server {@link MBeanServer} to get JMX data from.
     * @return a {@code ResourceConfig} containing a {@code MonitoringApplication}.
     */
    public static ResourceConfig createMonitoringResourceConfig(MBeanServer server) {
        return new ApplicationAdapter(createMonitoringApplication(server));
    }

}
