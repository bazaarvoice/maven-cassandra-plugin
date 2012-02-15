package com.bazaarvoice.commons.monitoring.transform;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating {@link ObjectNameTransform}s. Comes with a few built in transforms and a method to register others.
 */
public final class ObjectNameTransformFactory {

    private static Logger _logger = LoggerFactory.getLogger(ObjectNameTransformFactory.class);

    public static final TransformInfo FILTER_TRANSFORM = new TransformInfo("filter", FilteringObjectNameTransform.class);
    public static final TransformInfo GROUP_TRANSFORM = new TransformInfo("group", GroupingObjectNameTransform.class);
    public static final TransformInfo JVM_GROUP_TRANSFORM = new TransformInfo("groupjvm",
                                                                              GroupingObjectNameTransform.class,
                                                                              ImmutableMap.of("pattern", "java.lang:type=*,*",
                                                                                              "basename", "jvm.",
                                                                                              "distinguishingproperty", "type",
                                                                                              "attributenamingproperty", "name")
                                                                             );
    public static final TransformInfo TOMCAT_TRANSFORM = new TransformInfo("tomcat", TomcatTransform.class);

    private final Map<String, TransformInfo> _registry;
    private final MBeanServerConnection _mBeanServer;

    /**
     * Wrapper class used for registering transforms archetypes with the factory. Has a {@code verb}, which is used when looking up transforms by type,
     * a {@code Class}, and an optional {@code Map} of default parameters to use when constructing instances of the transform. The verb should
     * be unique per factory, but any number of {@code TransformInfo} with with the same transform class.
     */
    public static class TransformInfo {
        public final Class<? extends ObjectNameTransform> transformClass;
        public final String verb;
        public final Map<String, String> defaultParams;

        public TransformInfo(String verb, Class<? extends ObjectNameTransform> transformClass, Map<String, String> defaultParams) {
            this.transformClass = transformClass;
            this.verb = verb.toLowerCase();
            this.defaultParams = defaultParams;
        }

        public TransformInfo(String verb, Class<? extends ObjectNameTransform> transformClass) {
            this(verb, transformClass, null);
        }
    }

    public ObjectNameTransformFactory(MBeanServerConnection mBeanServer) {
        _mBeanServer = mBeanServer;
        _registry = new HashMap<String, TransformInfo>();
        _registry.put(FILTER_TRANSFORM.verb, FILTER_TRANSFORM);
        _registry.put(GROUP_TRANSFORM.verb, GROUP_TRANSFORM);
        _registry.put(JVM_GROUP_TRANSFORM.verb, JVM_GROUP_TRANSFORM);
        _registry.put(TOMCAT_TRANSFORM.verb, TOMCAT_TRANSFORM);
    }

    /**
     * Creates an {@code ObjectNameTransform} using the given properties. The properties map should contain the key
     * "{@code type}" with a value that equals one of the verbs of a registered {@link TransformInfo}. A new instance of that {@code TransformInfo}'s
     * {@link TransformInfo#transformClass transformClass} is created. Then the {@link TransformInfo#defaultParams}
     * of that {@code TransformInfo} are loaded into the provided properties map, and passed to the created {@code ObjectNameTransform}'s
     * {@link ObjectNameTransform#loadParams(java.util.Map)}.
     * @param properties the {@code Map} of properties that the transform should be built with.
     * @return an {@code ObjectNameTransform} matching the specified properties.
     */
    public ObjectNameTransform create(Map<String, String> properties) {
        properties = toLowerKeys(properties);
        return create(properties.remove("type"), properties);
    }

    /**
     * Does the same thing as {@link #create(java.util.Map)}, but with the transform type specified as a separate argument.
     * @param type the type of transform to create.
     * @param properties the {@code Map} of properties that the transform should be built with.
     * @return an {@code ObjectNameTransform} of the specified type matching the specified properties.
     */
    public ObjectNameTransform create(String type, Map<String, String> properties) {
        try {
            if (properties != null ) {
                properties = toLowerKeys(properties);
            }
            else {
                properties = new HashMap<String, String>();
            }
            TransformInfo info = _registry.get(type.toLowerCase());
            ObjectNameTransform transform = info.transformClass.getConstructor(MBeanServerConnection.class).newInstance(_mBeanServer);
            if (info.defaultParams != null) {
                properties.putAll(info.defaultParams);
            }
            transform.loadParams(properties);
            return transform;
        } catch(Exception e) {
            _logger.error("Error creating ObjectNameTransform from given properties", e);
            return null;
        }
    }

    /**
     * Registers with this factory a transform with the specified info. A transform of the registered type can then be created by calling
     * the {@link #create(String, java.util.Map) create} method with {@code type} equal to the {@code TransformInfo}'s {@link TransformInfo#verb verb}.
     * @param info the {@code TransformInfo} describing the transform to be registered.
     */
    public void register(TransformInfo info) {
        _registry.put(info.verb, info);
    }
    
    private Map<String, String> toLowerKeys(Map<String, String> map) {
        HashMap<String, String> loweredMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            loweredMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return loweredMap;
    }
}
