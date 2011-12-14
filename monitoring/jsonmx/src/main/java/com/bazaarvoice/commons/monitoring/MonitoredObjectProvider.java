package com.bazaarvoice.commons.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MonitoredObjectProvider {

    // TODO: Devise grouping strategy

    private static final Logger _logger = LoggerFactory.getLogger(MonitoredObjectProvider.class);

    private MBeanServerConnection _server;

    public MonitoredObjectProvider(MBeanServerConnection server) {
        _server = server;
    }

    public MonitoredObjectProvider() {
        this(ManagementFactory.getPlatformMBeanServer());
    }

    public void setServer(MBeanServerConnection server) {
        _server = server;
    }

    private ObjectName toObjectName(String objectName) {
        try {
            return ObjectName.getInstance(objectName);
        } catch (MalformedObjectNameException e) {
            return null;
        }
    }

    public Set<String> getObjectNames() {
        HashSet<String> names = new HashSet<String>();
        try {
            for(ObjectName objectName : _server.queryNames(null, null)) {
                names.add(objectName.toString());
            }
        } catch (IOException e) {
            _logger.warn("Unable to retrieve MBeans from MBeanServer", e);
            return null;
        }
        return names;
    }

    public Set<String> getAttributeNames(String objectName) {
        MBeanInfo info;
        try {
            info = _server.getMBeanInfo(toObjectName(objectName));
        } catch (Exception e) {
            _logger.warn("Unable to retrieve MBean attribute names for {}", objectName);
            return null;
        }
        Set<String> attributeNames = new HashSet<String>();
        for(MBeanAttributeInfo attr : info.getAttributes()) {
            attributeNames.add(attr.getName());
        }
        return attributeNames;
    }

    public Map<String, Object> getAttributes(String objectName) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        Set<String> attributeNames = getAttributeNames(objectName);
        try {
            for(Attribute one : _server.getAttributes(toObjectName(objectName), attributeNames.toArray(new String[attributeNames.size()])).asList()) {
                attributes.put(one.getName(), one.getValue());
            }
        } catch (Exception e) {
            _logger.warn("Unable to get MBean attributes for {}", objectName);
            return null;
        }
        return attributes;
    }

    public Map.Entry<String, Map<String, Object>> getObject(String objectName) {
        return new AbstractMap.SimpleImmutableEntry<String, Map<String, Object>>(objectName, getAttributes(objectName));
    }

    public Map<String, Map<String, Object>> getObjects() {
        HashMap<String, Map<String, Object>> objects = new HashMap<String, Map<String, Object>>();
        for(String objectName : getObjectNames()) {
            objects.put(objectName, getAttributes(objectName));
        }
        return objects;
    }

}
