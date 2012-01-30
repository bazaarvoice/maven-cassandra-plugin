package com.bazaarvoice.commons.monitoring.transform;

import com.bazaarvoice.commons.monitoring.core.MBeanServerUtils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code MonitoringDataTransform} that takes data straight from JMX and makes no modifications to it.
 * Will use the result of an {@link ObjectName}'s {@link javax.management.ObjectName#getCanonicalName() getCanonicalName} method as the
 * name for the corresponding objects in the data it produces.
 */
public class DefaultMonitoringDataTransform extends AbstractMonitoringDataTransform {

    private final MBeanServerConnection _mBeanServer;
    
    public DefaultMonitoringDataTransform(MBeanServerConnection mBeanServer) {
        this._mBeanServer = mBeanServer;
    }
    
    @Override
    public void addNames(Set<ObjectName> objectNames, Set<String> currentNames) {
        for (ObjectName objectName : objectNames) {
            currentNames.add(objectName.getCanonicalName());
        }
    }

    @Override
    public Set<ObjectName> reverseRename(String name) {
        HashSet<ObjectName> set = new HashSet<ObjectName>();
        ObjectName objectName;
        try {
            objectName = ObjectName.getInstance(name);
            if (_mBeanServer.isRegistered(objectName)) {
                set.add(objectName);
            }
        } catch (Exception e) {
            return null;
        }
        return set.size() > 0 ? set : null;
    }

    @Override
    public void constructAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
        for (ObjectName objectName : objectNames) {
            Set<String> attributes = currentAttributeNames.get(objectName.getCanonicalName());
            if (attributes != null) {
                attributes.addAll(MBeanServerUtils.getReadableAttributeNames(objectName, _mBeanServer));
            }
        }
    }

    @Override
    public void constructAttributes(Set<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes) {
        for (ObjectName objectName : objectNames) {
            Map<String, Object> attributes = currentAttributes.get(objectName.getCanonicalName());
            if (attributes != null) {
                attributes.putAll(MBeanServerUtils.getReadableAttributesAsMap(objectName, _mBeanServer));
            }
        }
    }

}
