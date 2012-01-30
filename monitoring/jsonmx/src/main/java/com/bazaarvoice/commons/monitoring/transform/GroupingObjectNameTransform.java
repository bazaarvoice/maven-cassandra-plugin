package com.bazaarvoice.commons.monitoring.transform;

import com.bazaarvoice.commons.monitoring.core.MBeanServerUtils;
import com.google.common.base.Strings;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basic grouping/renaming {@link ObjectNameTransform}.
 * Will aggregate MBeans for which {@link #matches(javax.management.ObjectName) matches} returns true
 * and the value for the distinguishing key property is the same into a single object whose attributes are its members.
 * If there is only a single member in the group, and it does not have a key property matching the attribute naming property,
 * it will instead just rename the object.
 * <p>
 * For example, a {@code GroupingObjectNameTransform} which matches {@code ObjectName}s like {@code java.lang:type=*,*}
 * and has {@code baseName} "{@code jvm.}", {@code distinguishingProperty} "{@code type}", and {@code attributeNamingProperty} "{@code name}"
 * will convert these three objects:
 * </p>
<p><pre><code>
{
  "java.lang:type=GarbageCollector,name=ConcurrentMarkSweep" : {...},
  "java.lang:type=GrabageCollector,name="ParNew" : {...},
  "java.lang:type=Runtime" : {...}
}
</code></pre></p>
 * Into these two objects:
<p><pre><code>
{
  "jvm.GarbageCollector" : {
    "ConcurrentMarkSweep" : {...},
    "ParNew" : {...}
  },
  "jvm.Runtime: {...}
}
</code></pre></p>
 */
public class GroupingObjectNameTransform  extends ObjectNameTransform {

    /**
     * Key for the {@code baseName} parameter when loaded through {@link #loadParams(java.util.Map) loadParams}.
     */
    public static final String BASE_NAME_PARAMETER = "basename";
    /**
     * Key for the {@code distinguishingProperty} parameter when loaded through {@link #loadParams(java.util.Map) loadParams}.
     */
    public static final String DISTINGUISHING_PROPERTY_PARAMETER = "distinguishingproperty";
    /**
     * Key for the {@code attributeNamingProperty} parameter when loaded through {@link #loadParams(java.util.Map) loadParams}.
     */
    public static final String ATTRIBUTE_NAMING_PROPERTY_PARAMETER = "attributenamingproperty";

    private String _baseName;
    private String _distinguishingProperty;
    private String _attributeNamingProperty;

    /**
     * Creates a {@code GroupingObjectNameTransform} with an empty {@code baseName},
     * a {@code distinguishingProperty} of "{@code type}",
     * and an {@code attributeNamingProperty} of "{@code name}".
     * @param mBeanServer the {@code MBeanServer} to query for registered MBeans in a group
     */
    public GroupingObjectNameTransform(MBeanServerConnection mBeanServer) {
        super(mBeanServer);
        _baseName = "";
        _distinguishingProperty = "type";
        _attributeNamingProperty = "name";
    }

    public GroupingObjectNameTransform(String baseName, String distinguishingProperty, String attributeNamingProperty, MBeanServerConnection mBeanServer) {
        super(mBeanServer);
        _baseName = baseName;
        _distinguishingProperty = distinguishingProperty;
        _attributeNamingProperty = attributeNamingProperty;
    }

    @Override
    public String getName(ObjectName objectName) {
        StringBuilder name = new StringBuilder(_baseName);
        name.append(objectName.getKeyProperty(_distinguishingProperty));
        return name.toString();
    }

    @Override
    public Set<ObjectName> getObjectNames(String name) {
        return groupMembers(name);
    }

    @Override
    public boolean isIntercepted(String name) {
        Set<ObjectName> objectNames = groupMembers(name);
        return objectNames != null && !objectNames.isEmpty();
    }

    @Override
    public Set<String> attributeNames(Collection<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
        if (objectNames.size() == 1) {
            ObjectName single = objectNames.iterator().next();
            if (single.getKeyProperty(_attributeNamingProperty) == null) {
                return currentAttributeNames.get(single.getCanonicalName());
            }
        }
        HashSet<String> attributeNames = new HashSet<String>();
        for (ObjectName objectName : objectNames) {
            attributeNames.add(objectName.getKeyProperty(_attributeNamingProperty));
        }
        return attributeNames;
    }

    @Override
    public Map<String, Object> attributes(Collection<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes) {
        if (objectNames.size() == 1) {
            ObjectName single = objectNames.iterator().next();
            if (single.getKeyProperty(_attributeNamingProperty) == null) {
                return currentAttributes.get(single.getCanonicalName());
            }
        }
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        for (ObjectName objectName : objectNames) {
            Map<String, Object> subAttributes = currentAttributes.get(objectName.getCanonicalName());
            if (subAttributes == null) {
                subAttributes = MBeanServerUtils.getReadableAttributesAsMap(objectName, getMBeanServer());
            }
            attributes.put(objectName.getKeyProperty(_attributeNamingProperty), subAttributes);
        }
        return attributes;
    }

    @Override
    public void loadParams(Map<String, String> params) {
        super.loadParams(params);
        String baseName = params.get(BASE_NAME_PARAMETER);
        if (baseName != null) {
            _baseName = baseName;
        }
        String distinguishingProperty = params.get(DISTINGUISHING_PROPERTY_PARAMETER);
        if (distinguishingProperty != null) {
            _distinguishingProperty = distinguishingProperty; 
        }
        String attributeNamingProperty = params.get(ATTRIBUTE_NAMING_PROPERTY_PARAMETER);
        if (attributeNamingProperty != null) {
            _attributeNamingProperty = attributeNamingProperty;
        }
    }

    private Set<ObjectName> groupMembers(String name) {
        if (Strings.isNullOrEmpty(name) || !name.startsWith(_baseName)) {
            return null;
        }
        String group = name.substring(_baseName.length());
        ObjectName query = null;
        if (!Strings.isNullOrEmpty(group)) {
            try {
                query = ObjectName.getInstance("*:" + _distinguishingProperty + "=" + group + ",*");
            } catch (MalformedObjectNameException e) {
                return null;
            }
        }
        return matchingNames(query);
    }

}
