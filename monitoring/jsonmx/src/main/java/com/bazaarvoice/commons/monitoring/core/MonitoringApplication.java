package com.bazaarvoice.commons.monitoring.core;

import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link Application} designed for use in monitoring.
 * Provides access to the {@link MonitoredObjectProvider} used for retrieving data as well as a method to register JAX-RS singletons.
 */
public class MonitoringApplication extends Application {

    private final Set<Class<?>> _classes;
    private final Set<Object> _singletons;
    private final MonitoredObjectProvider _provider;

    public MonitoringApplication(MonitoredObjectProvider provider) {
        _provider = provider;
        _singletons = new HashSet<Object>();
        _classes = Collections.emptySet();
    }

    /**
     * Convenience method for adding JAX-RS {@link com.sun.jersey.spi.resource.Singleton Singleton}s to the application.
     * This allows for using customized root resource and provider instances as necessary.
     * Resource classes will generally have one or more {@link javax.ws.rs.Path @Path} annotations.
     * Provider classes will generally be annotated with {@link javax.ws.rs.ext.Provider @Provider}.
     * @param singletons instances of resource and/or provider classes.
     */
    public void addSingletons(Object... singletons) {
        Collections.addAll(_singletons, singletons);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return _classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return _singletons;
    }

    public MonitoredObjectProvider getProvider() {
        return _provider;
    }
}
