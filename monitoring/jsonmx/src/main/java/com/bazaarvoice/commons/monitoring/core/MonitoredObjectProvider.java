package com.bazaarvoice.commons.monitoring.core;

import com.bazaarvoice.commons.monitoring.transform.DefaultMonitoringDataTransform;
import com.bazaarvoice.commons.monitoring.transform.MonitoringDataTransform;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Service to retrieve and transform JMX data from an MBeanServer.
 * By default it makes no modifications to the data, but transforms can be added with
 * {@link #addTransform(MonitoringDataTransform) addTransform}.
 * <p>
 * In general, provided data will be constructed with a pass through the transforms
 * in the order they were added followed by a filtration pass in reverse order.
 * This way, transforms can operate on data provided by previous transforms,
 * then filter out anything that should not be publicly visible.
 * <p>
 * Note that the {@link MonitoringDataTransform#reverseRename(String) reverseRename} pass goes through the
 * transforms in reverse order so that more recent transforms will take precedence. Unlike other transforms, it stops at the first result.
 */
public class MonitoredObjectProvider {

    private static final Logger _logger = LoggerFactory.getLogger(MonitoredObjectProvider.class);

    private final MBeanServerConnection _server;
    private final LinkedList<MonitoringDataTransform> _transforms;

    public MonitoredObjectProvider(MBeanServerConnection server) {
        _server = server;
        _transforms = new LinkedList<MonitoringDataTransform>();
        _transforms.add(new DefaultMonitoringDataTransform(_server));
    }

    public MonitoredObjectProvider() {
        this(MBeanServerUtils.findMBeanServer());
    }

    public MBeanServerConnection getServer() {
        return _server;
    }

    public void addTransform(MonitoringDataTransform transform) {
        if (transform != null) {
            _transforms.add(transform);
        }
    }

    private Set<String> addNames(Set<ObjectName> objectNames) {
        HashSet<String> names = new HashSet<String>();
        for (MonitoringDataTransform transform : _transforms) {
            transform.addNames(objectNames, names);
        }
        return names;
    }

    private Set<ObjectName> reverseRename(String name) {
        Set<ObjectName> objectNames = null;
        for (Iterator<MonitoringDataTransform> iterator = _transforms.descendingIterator(); iterator.hasNext();) {
            MonitoringDataTransform transform = iterator.next();
            objectNames = transform.reverseRename(name);
            if (objectNames != null) {
                break;
            }
        }
        return objectNames;
    }

    private void filterNames(Set<ObjectName> objectNames, Set<String> names) {
        for (Iterator<MonitoringDataTransform> iterator = _transforms.descendingIterator(); iterator.hasNext();) {
            MonitoringDataTransform transform = iterator.next();
            transform.filterNames(objectNames, names);
        }
    }
    
    private void filterAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> attributeNames) {
        for (Iterator<MonitoringDataTransform> iterator = _transforms.descendingIterator(); iterator.hasNext();) {
            MonitoringDataTransform transform = iterator.next();
            transform.filterAttributeNames(objectNames, attributeNames);
        }
    }

    public Set<String> getObjectNames() {
        try {
            Set<ObjectName> objectNames = _server.queryNames(null, null); 
            Set<String> names = addNames(objectNames);
            filterNames(objectNames, names);
            return names;
        } catch (IOException e) {
            _logger.warn("Unable to retrieve MBeans from MBeanServer", e);
            return null;
        }
    }

    public Set<String> getAttributeNames(String name) {
        Set<ObjectName> objectNames = reverseRename(name);
        if (objectNames == null) {
            return null;
        }
        Set<String> names = addNames(objectNames);
        HashMap<String, Set<String>> attributeNames = new HashMap<String, Set<String>>();
        for (String key : names) {
            attributeNames.put(key, new HashSet<String>());
        }
        for (MonitoringDataTransform transform : _transforms) {
            transform.constructAttributeNames(objectNames, attributeNames);
        }
        filterNames(objectNames, attributeNames.keySet());
        filterAttributeNames(objectNames, attributeNames);
        return attributeNames.get(name);
    }

    private Map<String, Map<String, Object>> getObjects(Set<ObjectName> objectNames) {
        Set<String> names = addNames(objectNames);
        HashMap<String, Map<String, Object>> attributes = new HashMap<String, Map<String, Object>>();
        for (String key : names) {
            attributes.put(key, new HashMap<String, Object>());
        }
        for (MonitoringDataTransform transform : _transforms) {
            transform.constructAttributes(objectNames, attributes);
        }
        filterNames(objectNames, attributes.keySet());
        filterAttributeNames(objectNames, Maps.transformEntries(attributes, new Maps.EntryTransformer<String, Map<String, Object>, Set<String>>() {
            @Override
            public Set<String> transformEntry(@Nullable String key, @Nullable Map<String, Object> value) {
                if (value != null) {
                    return value.keySet();
                }
                return null;
            }
        }));
        return attributes;
    }
    
    public Map<String, Object> getAttributes(String name) {
        Set<ObjectName> objectNames = reverseRename(name);
        if (objectNames == null) {
            return null;
        }
        Map<String, Map<String, Object>> objects = getObjects(objectNames);
        return objects.get(name);
    }

    public Map<String, Map<String, Object>> getObjects() {
        try {
            Set<ObjectName> objectNames = _server.queryNames(null, null);
            return getObjects(objectNames);
        } catch (IOException e) {
            _logger.warn("Unable to retrieve MBeans from MBeanServer", e);
            return null;
        }
    }

}
