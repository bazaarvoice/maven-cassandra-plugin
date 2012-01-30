package com.bazaarvoice.commons.monitoring.transform;

import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

/**
 * No-op {@code MonitoringDataTransform MonitoringDataTransform}.
 * Transform classes should extend this and override the methods that are necessary for their transformation.
 */
public abstract class AbstractMonitoringDataTransform implements MonitoringDataTransform {

    @Override
    public void addNames(Set<ObjectName> objectNames, Set<String> currentNames) {
    }

    @Override
    public Set<ObjectName> reverseRename(String name) {
        return null;
    }

    @Override
    public void filterNames(Set<ObjectName> objectNames, Set<String> currentNames) {
    }

    @Override
    public void constructAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
    }

    @Override
    public void constructAttributes(Set<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes) {
    }

    @Override
    public void filterAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
    }
}
