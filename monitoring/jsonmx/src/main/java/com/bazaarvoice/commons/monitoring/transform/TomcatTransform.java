package com.bazaarvoice.commons.monitoring.transform;

import com.bazaarvoice.commons.monitoring.core.MBeanServerUtils;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * {@link MonitoringDataTransform} that filters out potentially problematic data provided by Tomcat.
 * Has three modes:
 * <ol>
 *     <li>{@code ATTRIBUTES} mode - the default - which filters out known problematic attributes.</li>
 *     <li>{@code BASIC} mode, which filters out all {@code MBean}s with known problematic attributes.</li>
 *     <li>{@code STRICT} mode, which filters out all Tomcat {@code MBean}s.</li>
 * </ol>
 */
public class TomcatTransform extends ObjectNameTransform {

    public static final String[] BASIC_OBJECT_NAMES = { "Catalina:type=Host,*",
                                                        "Catalina:j2eeType=WebModule,*",
                                                        "Catalina:type=Service,*",
                                                        "Catalina:type=Engine,*",
                                                        "Catalina:type=Server,*"
                                                       };
    public static final String[] STRICT_OBJECT_NAMES = { "Catalina:*",
                                                         "Users:*",
                                                       };
    public static final String[] ATTRIBUTE_NAMES = { "managedResource",
                                                     "manager",
                                                     "realm",
                                                     "loader",
                                                     "mappingObject",
                                                     "container",
                                                     "logger"
                                                   };

    /**
     * Enumeration of the different filtering policies ({@code ATTRIBUTES}, {@code BASIC}, and {@code STRICT}).
     * A {@code Policy} value name can be used as a key to a {@link Map} passed to the {@link #loadParams} method to configure an instance
     * of {@code TomcatTransform} to use the given policy. The value associated with that key in the {@code Map} is ignored and can be {@code null}.
     */
    public static enum Policy {
        ATTRIBUTES, BASIC, STRICT
    }

    private Policy _policy;

    /**
     * @param mBeanServer the MBeanServer to query for MBeans with matching {@code ObjectName}s.
     */
    public TomcatTransform(MBeanServerConnection mBeanServer) {
        super(mBeanServer);
        for (String name : BASIC_OBJECT_NAMES) {
            super.include(MBeanServerUtils.toObjectName(name));
        }
        _policy = Policy.ATTRIBUTES;
    }

    @Override
    public String getName(ObjectName objectName) {
        return objectName.getCanonicalName();
    }

    @Override
    public Set<ObjectName> getObjectNames(String name) {
        return Collections.singleton(MBeanServerUtils.toObjectName(name));
    }

    @Override
    public boolean isIntercepted(String name) {
        return matches(MBeanServerUtils.toObjectName(name));
    }

    @Override
    public Set<String> attributeNames(Collection<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
        return null;
    }

    @Override
    public Map<String, Object> attributes(Collection<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes) {
        return null;
    }

    @Override
    public boolean filterMatchingCanonicalObjectNames() {
        return !_policy.equals(Policy.ATTRIBUTES);
    }

    @Override
    public boolean filterOtherAttributes() {
        return false;
    }

    @Override
    public boolean replaceAttributesOnCollision() {
        return false;
    }

    @Override
    public void filterAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
        super.filterAttributeNames(objectNames, currentAttributeNames);
        if (_policy.equals(Policy.ATTRIBUTES)) {
            for (ObjectName objectName : objectNames) {
                if (matches(objectName)) {
                    Set<String> attributes = currentAttributeNames.get(objectName.getCanonicalName());
                    if(attributes != null) {
                        for (String attribute : ATTRIBUTE_NAMES) {
                            attributes.remove(attribute);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void loadParams(Map<String, String> params) {
        super.loadParams(params);
        for (Policy policy : Policy.values()) {
            String policyName = policy.name().toLowerCase();
            if (params.containsKey(policyName)) {
                _policy = policy;
            }
        }
        if (_policy.equals(Policy.STRICT)) {
            for (String name : STRICT_OBJECT_NAMES) {
                include(MBeanServerUtils.toObjectName(name));
            }
        }
    }
}