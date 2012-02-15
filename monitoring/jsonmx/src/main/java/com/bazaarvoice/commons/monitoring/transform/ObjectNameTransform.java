package com.bazaarvoice.commons.monitoring.transform;

import com.bazaarvoice.commons.monitoring.core.MBeanServerUtils;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An abstract implementation of {@code MonitoringDataTransform} which bases its behavior
 * on information available from the {@link ObjectName}s of MBeans.
 * <p>
 * Provides methods for for registering {@code ObjectName} patterns and identifying {@code ObjectNames} that match
 * any registered pattern.
 * <p>
 * An {@code ObjectNameTransform} can typically expect to be in sequence after the {@link DefaultMonitoringDataTransform},
 * which inserts data with keys equal to the canonical name of an MBean's {@code ObjectName}.
 * Implementations of the {@link #attributeNames(java.util.Collection, java.util.Map) attributeNames} and
 * {@link #attributes(java.util.Collection, java.util.Map) attributes} methods
 * can thus choose to pull data from the map using keys produced by {@link javax.management.ObjectName#getCanonicalName()} rather than querying the MBeanServer
 * directly.
 */
public abstract class ObjectNameTransform extends AbstractMonitoringDataTransform {

    private static final Logger _logger = LoggerFactory.getLogger(ObjectNameTransform.class);
    
    private final HashSet<ObjectName> _expressions;
    private final MBeanServerConnection _mBeanServer;

    /**
     * @param mBeanServer the MBeanServer to query for MBeans with matching {@code ObjectName}s.
     */
    public ObjectNameTransform(MBeanServerConnection mBeanServer) {
        this._expressions = new HashSet<ObjectName>();
        this._mBeanServer = mBeanServer;
    }

    /**
     * Enumeration of initial parameter names that the transform recognizes.
     */
    public static enum RecognizedInitParam {
        PATTERN("", ""), DOMAIN("", ":*"), KEYPROPERTY("*:", "=*,*");

        private String prefix;
        private String postfix;

        private RecognizedInitParam(String prefix, String postfix) {
            this.prefix = prefix;
            this.postfix = postfix;
        }

        private ObjectName buildObjectName(String data) {
            return MBeanServerUtils.toObjectName(prefix + data + postfix);
        }
    }

    /**
     * Gets the MBeanServer this transform queries. Useful for subclasses for which
     * {@link #matchingNames(javax.management.QueryExp)} is insufficient.
     * @return the MBeanServer this transform queries.
     */
    public MBeanServerConnection getMBeanServer() {
        return _mBeanServer;
    }

    /**
     * Checks if a given {@code ObjectName} matches any of the patterns registered with this transform.
     * Patterns can be registered through calls to {@link #include(javax.management.ObjectName) include}
     * or from an initial parameter map passed to {@link #loadParams(java.util.Map)}.
     * @param objectName the {@code ObjectName} to check.
     * @return {@code true} if {@code objectName} matches a pattern registered with this transform.
     */
    public boolean matches(ObjectName objectName) {
        if (objectName == null) {
            return false;
        }
        for (ObjectName expression : _expressions) {
            if (expression.apply(objectName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Queries the MBeanServer for the {@code ObjectName}s of all registered MBeans which match a pattern registered
     * with this transform and for which the filter's {@link QueryExp#apply(javax.management.ObjectName) apply} method returns {@code true}.
     * @param filter a {@code QueryExp} with which to limit the returned set.
     * Note that {@code ObjectName} implements {@code QueryExp}, so {@code ObjectName} patterns are valid {@code filter}s.
     * @return a {@code Set} of {@code ObjectName}s registered with the MBeanServer that this transform matches.
     */
    public Set<ObjectName> matchingNames(QueryExp filter) {
        HashSet<ObjectName> names = new HashSet<ObjectName>();
        for (ObjectName expression : _expressions) {
            try {
                names.addAll(_mBeanServer.queryNames(expression, filter));
            } catch (IOException e) {
                _logger.error("Unable to connect to MBeanServer", e);
            }
        }
        return names;
    }

    /**
     * Queries the MBeanServer for the {@code ObjectNames} of all registered MBeans which this transform matches.
     * @return the {@code Set} of {@code ObjectName}s registered with the MBeanServer that this transform matches.
     */
    public Set<ObjectName> matchingNames() {
        return matchingNames(ObjectName.WILDCARD);
    }

    /**
     * Configures this transform based on data from a {@code Map} of parameters. Subclasses that override this method
     * should call {@code super.loadParams()} to make sure that any parameters parent classes recognize will get loaded.
     * The base version of this recognizes parameter names that belong to {@link RecognizedInitParam},
     * but implementations may recognize additional parameter names.
     * @param params a {@code Map} of parameter names and their values.
     */
    public void loadParams(Map<String, String> params) {
        for (RecognizedInitParam param : RecognizedInitParam.values()) {
            String key = param.name().toLowerCase();
            String value = params.get(key);
            if (value != null) {
                ObjectName objectName = param.buildObjectName(value);
                if (objectName != null) {
                    include(param.buildObjectName(value));
                    params.remove(key);
                }
                else {
                    _logger.warn("Unable to build ObjectName from " + key + " " + value);
                }
            }
        }
    }

    /**
     * Gets the name of the object that {@code objectName} should correspond to.
     * Will only be called if {@link #matches(javax.management.ObjectName) matches}{@code (objectName)} returns true.
     * @param objectName the {@code ObjectName} to get the transformed name for.
     * @return the name of the object this transform creates or modifies using {@code ObjectName}.
     */
    public abstract String getName(ObjectName objectName);

    /**
     * The inverse of {@link #getName(javax.management.ObjectName)}. Gets a {@code Set} of all {@code ObjectNames} this transform
     * will need to produce an object with the given name.
     * Will only be called if {@link #isIntercepted(String) isIntecepted}{@code (name)} returns true.
     * @param name the name of the object created or modified by this transform.
     * @return the {@code Set} of {@code ObjectNames} need to produce the object with the given name.
     */
    public abstract Set<ObjectName> getObjectNames(String name);

    /**
     * Checks if this transform could produce or modify an object with the given name.
     * @param name the name of on object being queried.
     * @return true if this transform creates or modifies an object with the given name.
     */
    public abstract boolean isIntercepted(String name);

    /**
     * Generates the {@code Set} of attribute names this transform is responsible for for a single object.
     * Each {@code ObjectName} in {@code objectNames} will return the same value when passed to {@link #getName(javax.management.ObjectName)}.
     * The {@code ObjectNameTransform} implementation of {@link #constructAttributeNames(java.util.Set, java.util.Map)} will call this method.
     * @param objectNames a group of {@code ObjectName}s which have the same value for {@code getName}.
     * @param currentAttributeNames the attribute names produced by other transforms thus far.
     * @return the {@code Set} of attribute names this transform will create for the object.
     */
    public abstract Set<String> attributeNames(Collection<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames);

    /**
     * Generates the {@code Set} of attributes this transform is responsible for for a single object.
     * Each {@code ObjectName} in {@code objectNames} will return the same value when passed to {@link #getName(javax.management.ObjectName)}.
     * The {@code ObjectNameTransform} implementation of {@link #constructAttributes(java.util.Set, java.util.Map)} will call this method.
     * How these are merged with attributes produced by other transforms can be modified by overriding {@link #replaceAttributesOnCollision()}
     * and {@link #filterOtherAttributes()}.
     * @param objectNames a group of {@code ObjectName}s which have the same value for {@code getName}.
     * @param currentAttributes the attributes produced by other transforms thus far.
     * @return the {@code Set} of attribute names this transform will create for the object.
     */
    public abstract Map<String, Object> attributes(Collection<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes);

    /**
     * Whether this transform should filter out data matching data produced by the {@link DefaultMonitoringDataTransform}.
     * Data with a name that is the canonical name of an {@code ObjectName} for which {@link #matches(javax.management.ObjectName)} returns {@code true} will be filtered
     * during the call to {@link #filterNames(java.util.Set, java.util.Set)} if this returns {@code true}.
     * <p>
     * {@code true} by default. Subclasses should override this method if different behavior is required.
     * @return {@code true} if this transform should filter out the default data for MBeans with matching {@code ObjectName}s.
     */
    public boolean filterMatchingCanonicalObjectNames() {
        return true;
    }

    /**
     * Whether this transform should replace attributes that other transforms have already created with the same name as attributes this transform creates.
     * If both this and {@link #filterOtherAttributes()} return {@code true}, then the results of {@link #attributeNames(java.util.Collection, java.util.Map)} and
     * {@link #attributes(java.util.Collection, java.util.Map)} will be included in the data set by reference rather than by value.
     * <p>
     * {@code true} by default. Subclasses should override this method if different behavior is required.
     * @return {@code true} if this transform should replace existing attributes with the same name as ones it creates.
     */
    public boolean replaceAttributesOnCollision() {
        return true;
    }

    /**
     * Whether this transform should filter out attributes that other transforms create. If this is {@code true}, any attribute name not in the result of
     * {@link #attributeNames(java.util.Collection, java.util.Map)}, then that attribute will be filtered during the execution of {@link #filterAttributeNames(java.util.Set, java.util.Map)}.
     * If both this and {@link #replaceAttributesOnCollision()} return {@code true}, then the results of {@link #attributeNames(java.util.Collection, java.util.Map)} and
     * {@link #attributes(java.util.Collection, java.util.Map)} will be included in the data set by reference rather than by value.
     * <p>
     * {@code true} by default. Subclasses should override this method if different behavior is required.
     * @return {@code true} if this transform should filter out attributes not in {@link #attributeNames(java.util.Collection, java.util.Map)}.
     */
    public boolean filterOtherAttributes() {
        return true;
    }

    /**
     * Includes an {@link ObjectName ObjectName} pattern in this transform. {@link #matches(javax.management.ObjectName)} will return true if its
     * argument produces a positive result when passed to the {@link ObjectName#apply(javax.management.ObjectName)} method of any {@code ObjectName} registered
     * with a call to include.
     * @param objectName the expression that should be included in this transform for the purposes of matching.
     */
    protected void include(ObjectName objectName) {
        if (objectName != null) {
            _expressions.add(objectName);
        }
    }

    @Override
    public void addNames(Set<ObjectName> objectNames, Set<String> currentNames) {
        for (ObjectName objectName : objectNames) {
            if (matches(objectName)) {
                String name = getName(objectName);
                if (!Strings.isNullOrEmpty(name)) {
                    currentNames.add(name);
                }
            }
        }
    }

    @Override
    public Set<ObjectName> reverseRename(String name) {
        return isIntercepted(name) ? getObjectNames(name) : null;
    }

    @Override
    public void filterNames(Set<ObjectName> objectNames, Set<String> currentNames) {
        if (filterMatchingCanonicalObjectNames()) {
            for (ObjectName objectName : objectNames) {
                if (matches(objectName)) {
                    currentNames.remove(objectName.getCanonicalName());
                }
            }
        }
    }

    @Override
    public void constructAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
        Multimap<String, ObjectName> groups = groupObjectNames(objectNames);
        for (String name : groups.keySet()) {
            Set<String> attributes = currentAttributeNames.get(name);
            Set<String> desiredAttributes = attributeNames(groups.get(name), currentAttributeNames);
            if (replaceAttributesOnCollision() && filterOtherAttributes()) {
                currentAttributeNames.put(name, desiredAttributes);
            }
            else if (attributes != null && desiredAttributes != null) {
                attributes.addAll(desiredAttributes);
            }
        }
    }

    @Override
    public void constructAttributes(Set<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes) {
        Multimap<String, ObjectName> groups = groupObjectNames(objectNames);
        for (String name : groups.keySet()) {
            Map<String, Object> attributes = currentAttributes.get(name);
            Map<String, Object> desiredAttributes = attributes(groups.get(name), currentAttributes);
            if (replaceAttributesOnCollision() && filterOtherAttributes()) {
                currentAttributes.put(name, desiredAttributes);
            }
            else if (attributes != null && desiredAttributes != null) {
                for (Map.Entry<String, Object> newAttribute : desiredAttributes.entrySet()) {
                    if (!attributes.containsKey(newAttribute.getKey()) || replaceAttributesOnCollision()) {
                        attributes.put(newAttribute.getKey(), newAttribute.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void filterAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames) {
        if (!filterOtherAttributes()) {
            return;
        }
        Multimap<String, ObjectName> groups = groupObjectNames(objectNames);
        for (String name : groups.keySet()) {
            Set<String> attributes = currentAttributeNames.get(name);
            if (attributes != null && !attributes.isEmpty()) {
                Set<String> desiredAttributes = attributeNames(groups.get(name), currentAttributeNames);
                for (Iterator<String> iterator = attributes.iterator(); iterator.hasNext();) {
                    if (!desiredAttributes.contains(iterator.next())) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private Multimap<String, ObjectName> groupObjectNames(Collection<ObjectName> objectNames) {
        Multimap<String, ObjectName> groups = HashMultimap.create();
        for (ObjectName objectName : objectNames) {
            if (matches(objectName)) {
                String name = getName(objectName);
                if (!Strings.isNullOrEmpty(name)) {
                    groups.put(name, objectName);
                }
            }
        }
        return groups;
    }
}
