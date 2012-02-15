package com.bazaarvoice.commons.monitoring.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class with utility methods for working with JMX {@link MBeanServer}s.
 */
public final class MBeanServerUtils {

    private static Logger _logger = LoggerFactory.getLogger(MBeanServerUtils.class);

    private MBeanServerUtils() {
    }

    /**
     * @return the first {@link MBeanServer} found, or, if none are found, the {@link ManagementFactory#getPlatformMBeanServer() PlatformMBeanServer}.
     */
    public static MBeanServer findMBeanServer() {
        ArrayList<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        MBeanServer defaultServer;
        if (servers.size() != 0) {
            defaultServer = servers.get(0);
        }
        else {
            defaultServer = ManagementFactory.getPlatformMBeanServer();
        }
        return defaultServer;
    }

    /**
     *
     * @param objectName the {@code ObjectName} to get the attribute names for.
     * @param mBeanServer the MBeanServer to query.
     * @return a {@code Set} consisting of the names of the readable attributes for the desired MBean,
     * or null if the {@code ObjectName} is not registered or the MBeanServer is inaccessible.
     */
    public static Set<String> getReadableAttributeNames(ObjectName objectName, MBeanServerConnection mBeanServer) {
        HashSet<String> names = new HashSet<String>();
        MBeanInfo info;
        try {
            info = mBeanServer.getMBeanInfo(objectName);
            for (MBeanAttributeInfo attr : info.getAttributes()) {
                if (attr.isReadable()) {
                    names.add(attr.getName());
                }
            }
            return names;
        } catch (Exception e) {
            _logger.warn("Unable to retrieve attribute names for " + objectName.getCanonicalName() + " from MBeanServer", e);
            return null;
        }
    }

    /**
     * @param objectName the {@code ObjectName} of the MBean to get the attributes for.
     * @param attributeNames the {@code Set} of attributes to retrieve.
     * @param mBeanServer the MBeanServer to query.
     * @return a {@code Map} of attribute names and their corresponding values for the MBean.
     */
    public static Map<String, Object> getAttributesAsMap(ObjectName objectName, Set<String> attributeNames, MBeanServerConnection mBeanServer) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            for (Attribute one : mBeanServer.getAttributes(objectName, attributeNames.toArray(new String[attributeNames.size()])).asList()) {
                map.put(one.getName(), one.getValue());
            }
        } catch (Exception e) {
            _logger.error("Unable to retrieve attributes from MBeanServer", e);
            return null;
        }
        return map;
    }

    /**
     * @param objectName the {@code ObjectName} of the MBean to get the attributes for.
     * @param mBeanServer the MBeanServer to query.
     * @return a {@code Map} of all readable attributes for MBean.
     */
    public static Map<String, Object> getReadableAttributesAsMap(ObjectName objectName, MBeanServerConnection mBeanServer) {
        return getAttributesAsMap(objectName, getReadableAttributeNames(objectName, mBeanServer), mBeanServer);
    }

    /**
     * Converts a {@code String} representation of an {@code ObjectName} into an actual {@code ObjectName}.
     * Returns {@code null} on invalid input rather than throw an exception.
     * {@code name} can be a pattern, in which case the created {@code ObjectName} will be as well.
     * @param name a {@code String} representation of an {@code ObjectName}.
     * @return an {@code ObjectName} built from the given {@code name}, or {@code null} if {@code name} is not a valid {@code ObjectName}.
     */
    public static ObjectName toObjectName(String name) {
        try {
            return ObjectName.getInstance(name);
        } catch (MalformedObjectNameException e) {
            _logger.warn("Invalid ObjectName " + name, e);
        }
        return null;
    }
    
}
