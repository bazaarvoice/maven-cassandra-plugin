package com.bazaarvoice.commons.monitoring.transform;

import com.bazaarvoice.commons.monitoring.core.MBeanServerUtils;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Basic filtering {@link ObjectNameTransform}.
 * Filters out all {@link ObjectName}s for which {@link #matches(javax.management.ObjectName) matches} returns true.
 */
public class FilteringObjectNameTransform extends ObjectNameTransform {

    public FilteringObjectNameTransform(MBeanServerConnection mBeanServer) {
        super(mBeanServer);
    }

    /**
     * @return {@code null}, since filtered objects have no name.
     */
    @Override
    public String getName(ObjectName objectName) {
        return null;
    }

    /**
     * @return {@link java.util.Collections#emptySet()}, since no MBeans are necessary to produce filtered data.
     */
    @Override
    public Set<ObjectName> getObjectNames(String name) {
        return Collections.emptySet();
    }

    @Override
    public boolean isIntercepted(String name) {
        return matches(MBeanServerUtils.toObjectName(name));
    }

    /**
     * @return {@code null}, since no attributes are necessary for filtered data.
     */
    @Override
    public Set<String> attributeNames(Collection<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
        return null;
    }

    /**
     * @return {@code null}, since no attributes are necessary for filtered data.
     */
    @Override
    public Map<String, Object> attributes(Collection<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes) {
        return null;
    }
}
