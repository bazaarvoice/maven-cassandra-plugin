package com.bazaarvoice.commons.monitoring;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MonitoredObjectProvider {

    // TODO: Devise grouping strategy

    private MBeanServerConnection _server;

    public MonitoredObjectProvider(MBeanServerConnection server) {
        _server = server;
    }

    public void setServer(MBeanServerConnection server) {
        _server = server;
    }

    private ObjectName toObjectName(String objectName) {
        try {
            return new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
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
            e.printStackTrace();
            return null;
        }
        return names;
    }

    public Set<String> getAttributeNames(String objectName) {
        MBeanInfo info;
        try {
            info = _server.getMBeanInfo(toObjectName(objectName));
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
