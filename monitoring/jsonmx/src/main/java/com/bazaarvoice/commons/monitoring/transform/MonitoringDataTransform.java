package com.bazaarvoice.commons.monitoring.transform;

import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

/**
 * Interface for transforming monitoring data.
 * The first parameter to most methods is a {@link Set} of {@link ObjectName}s
 * of the MBeans that are relevant for producing the requested data.
 * The second parameter is the data produced thus far, which should be modified as necessary to perform the transform.
 * <p>
 * Generally, classes that implement this interface should inherit from {@link AbstractMonitoringDataTransform}
 * and only override the methods salient to the transformations it will perform. If decisions about which MBeans to use for a transformation
 * can be made based on information available from its {@code ObjectName}, consider instead inheriting from {@link ObjectNameTransform}.
 * <p>
 * It may be worth noting that, while the constructive methods ({@link #addNames(java.util.Set, java.util.Set) addNames},
 * {@link #constructAttributeNames(java.util.Set, java.util.Map) constructAttributeNames},
 * and {@link #constructAttributes(java.util.Set, java.util.Map) constructAttributes}) are intended to build
 * data, there are no actual restrictions preventing removal of data inside those methods. Removing data during the constructive passes should be
 * avoided if possible, but is not prohibited. In some cases, doing so as a sort of preemptive filter (so subsequent transforms don't have access to
 * the data being removed) may be useful.
 */
public interface MonitoringDataTransform {

    /**
     * Adds to currentNames the names of objects this transform is responsible for providing.
     * @param objectNames the {@code ObjectNames} of MBeans that should be considered for this operation.
     * @param currentNames the {@code Set} of names thus far.
     */
    public void addNames(Set<ObjectName> objectNames, Set<String> currentNames);

    /**
     * If this transform could produce the object of the given name, returns the {@code Set} of {@code ObjectName}s of the MBeans necessary to do so,
     * or {@code null} otherwise.
     * Transforms should try to return a minimal set, but are not required to do so. Transforms that can produce arbitrary or nearly arbitrary names
     * but depend on the results of other transforms to do so might return the Set of all currently registered {@code ObjectName}s.
     * @param name the name of the object being requested.
     * @return if this transform might produce an object of the given name, a {@code Set} containing {@code ObjectName}s of all MBeans necessary to do so.
     * {@code null} otherwise.
     */
    public Set<ObjectName> reverseRename(String name);

    /**
     * Removes from {@code currentNames} the names of objects that should not be displayed.
     * @param objectNames the {@code ObjectName}s of MBeans that should be considered for this operation.
     * @param currentNames the {@code Set} of names thus far.
     */
    public void filterNames(Set<ObjectName> objectNames, Set<String> currentNames);

    /**
     * Adds to {@code currentAttributeNames} the names of attributes this transform is responsible for providing.
     * @param objectNames the {@code ObjectNames} of MBeans that should be considered for this operation.
     * @param currentAttributeNames a {@code Map} of the attribute names thus far. The key of the map is the name of an object,
     * and the value is the set of attribute names for the object thus far. The values might be null.
     */
    public void constructAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames);

    /**
     * Adds to {@code currentAttributes} the attributes this transform is responsible for providing, or modifies existing attributes.
     * If a transform is wholly incorporating the attributes of one object in another (typically to rename it or include it as an attribute),
     * it is encouraged to copy by reference if modifications to the original object by other transforms should be reflected in the containing object.
     * @param objectNames the {@code ObjectName}s of MBeans that should be considered for this operation.
     * @param currentAttributes a {@code Map} of the attributes thus far. The key of the map is the name of an object, and the value is
     * a map of the attributes for that object thus far. The key to the submap is the name of an attribute, and its value the corresponding data.
     */
    public void constructAttributes(Set<ObjectName> objectNames, Map<String, Map<String, Object>> currentAttributes);

    /**
     * Removes from {@code currentAttributeNames} the names of any attributes that should not be displayed.
     * @param objectNames the {@code ObjectName}s of MBeans that should be considered for this operation.
     * @param currentAttributeNames a {@code Map} of the attribute names thus far. The key of the map is the name of an object,
     * and the value is the set of attribute names for the object thus far. The values might be null.
     */
    public void filterAttributeNames(Set<ObjectName> objectNames, Map<String, Set<String>> currentAttributeNames);

}
