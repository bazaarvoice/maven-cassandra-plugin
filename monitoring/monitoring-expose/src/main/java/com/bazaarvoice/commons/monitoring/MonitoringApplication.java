package com.bazaarvoice.commons.monitoring;

import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MonitoringApplication extends Application {

    private final Set<Class<?>> _classes = new HashSet<Class<?>>();
    private Set<Object> _singletons = new HashSet<Object>();

    public void setClasses(Set<Class<?>> classes) {
        _classes.clear();
        _classes.addAll(classes);
    }

    public void setSingletons(Set<Object> singletons) {
        _singletons.clear();
        _singletons.addAll(singletons);
    }

    public void addClasses(Class<?>... classes) {
        Collections.addAll(_classes, classes);
    }

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
}
