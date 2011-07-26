package com.bazaarvoice.prr.util;

import com.bazaarvoice.cca.util.multimap.SetMultiMap;
import com.bazaarvoice.core.util.Assert;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collection utility methods
 */
public abstract class BVCollectionUtils {

    /**
     * Utility method for getting a ContentSource from a singleton collection.
     */
    public static <T> T getSingleton(List<T> list) {
        Assert.isState(list.size() <= 1);
        return list.size() != 0 ? list.get(0) : null;
    }

    /**
     * Utility method for setting a ContentSource from a singleton collection.  Works with Hibernate persistent collections.
     */
    public static <T> void setSingleton(List<T> list, T source) {
        Assert.isState(list.size() <= 1);
        // Hibernate persistent collections are more efficient if you don't modify them when they're already correct.
        if (!Objects.equal(getSingleton(list), source)) {
            if (list.size() != 0) {
                list.clear();
            }
            if (source != null) {
                list.add(source);
            }
        }
    }

    /**
     * Removes duplicates in the specified list, preserving order.
     */
    public static <T> List<T> dedup(List<T> list) {
        return new ArrayList<T>(new LinkedHashSet<T>(list));
    }

    /**
     * Split a collection of elements into to a collection of lists, each of
     * size less than or equal to batchSize.
     */
    public static <T> List<List<T>> split(Collection<T> items, int batchSize) {
        return split(new ArrayList<T>(items), batchSize);
    }

    /**
     * Split a list of elements into to a collection of lists, each of
     * size less than or equal to batchSize.
     */
    public static <T> List<List<T>> split(List<T> items, int batchSize) {
        List<List<T>> itemBatches = new ArrayList<List<T>>();
        for (int startIndex = 0; startIndex < items.size(); startIndex += batchSize) {
            int endIndex = Math.min(startIndex + batchSize, items.size());
            itemBatches.add(items.subList(startIndex, endIndex));
        }
        return itemBatches;
    }

    /**
     * Converts a list of object arrays into a map.  It assumes that the
     * first element in each array is the key and the second element is the value.
     */
    public static <T, V> Map<T, V> buildMap(List<Object[]> list, Map<T, V> target) {
        for (Object[] element : list) {
            //noinspection unchecked
            target.put((T) element[0], (V) element[1]);
        }
        return target;
    }

    public static <T, V> Map<T, V> mapToConstant(Collection<T> keys, V value) {
        ImmutableMap.Builder<T, V> mapBuilder = ImmutableMap.builder();
        for (T key : keys) {
            mapBuilder.put(key, value);
        }
        return mapBuilder.build();
    }

    public static Set<String> toLowerCase(Set<String> strings) {
        Set<String> result = new LinkedHashSet<String>();
        for (String string : strings) {
            result.add(string.toLowerCase());
        }
        return result;
    }

    /**
     * Returns true if c1 contains all of c2 (null-safe).
     */
    public static <T> boolean containsAll(Collection<T> c1, Collection<T> c2) {
        return CollectionUtils.isEmpty(c2) || (!CollectionUtils.isEmpty(c1) && c1.containsAll(c2));
    }

    /**
     * Returns true if c1 contains any of c2 (null-safe).
     */
    public static <T> boolean containsAny(Collection<T> c1, Collection<T> c2) {
        return CollectionUtils.containsAny(c1, c2);
    }

    /**
     * Returns a sorted copy of the specified collection.  Compare this to java.util.Collections.sort()
     * which takes a list and modifies it, while this version takes a collection and returns a fresh copy.
     */
    public static <T extends Comparable<? super T>> List<T> sort(Collection<T> collection) {
        return sort(collection, null);
    }

    /**
     * Returns a sorted copy of the specified collection.  Compare this to java.util.Collections.sort()
     * which takes a list and modifies it, while this version takes a collection and returns a fresh copy.
     */
    @SuppressWarnings ({"unchecked"})
    public static <T> List<T> sort(Collection<T> collection, Comparator<? super T> comparator) {
        Object[] array = collection.toArray();
        Arrays.sort(array, (Comparator) comparator);
        return (List<T>) Arrays.asList(array);
    }

    /**
     * Returns a new list containing the specified list plus zero or one extra members.
     */
    public static <T> List<T> append(List<T> list, T... members) {
        return concat(list, Arrays.asList(members));
    }

    /**
     * Concatenates a sequence of lists together to return a single list.
     */
    @SuppressWarnings ({"unchecked"})
    public static <T> List<T> concat(Collection<T>... collections) {
        int totalLength = 0;
        for (Collection<T> collection : collections) {
            totalLength += collection.size();
        }
        T[] result = (T[]) new Object[totalLength];
        int pos = 0;
        for (Collection<T> collection : collections) {
            for (T member : collection) {
                result[pos++] = member;
            }
        }
        return Arrays.asList(result);
    }

    public static <T> Collection<T> flatten(Collection<? extends Collection<T>> collections) {
        return concat(collections.toArray(new Collection[collections.size()]));
    }

    /**
     * Returns a new array containing the specified array plus zero or one extra members.
     */
    public static <T> T[] append(T[] array, T... members) {
        return concat(array, members);
    }

    /**
     * Concatenates a sequence of arrays together to return a single array.
     */
    @SuppressWarnings ({"unchecked"})
    public static <T> T[] concat(T[]... arraysOfSameType) {
        int totalLength = 0;
        for (T[] array : arraysOfSameType) {
            totalLength += array.length;
        }
        Class<?> componentType = arraysOfSameType[0].getClass().getComponentType();
        T[] result = (T[]) Array.newInstance(componentType, totalLength);
        int pos = 0;
        for (T[] array : arraysOfSameType) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    public static <T> Set<T> asSet(T... values) {
        return new HashSet<T>(Arrays.asList(values));
    }

    /**
     * Returns a copy of specified collection except blank items
     */
    public static List<String> createListWithNoBlankValues(List<String> stringList) {
        List<String> newList = new ArrayList<String>(stringList.size());
        for (String element : stringList) {
            if (StringUtils.isNotBlank(element)) {
                newList.add(element);
            }
        }
        return newList;
    }

    public static <T> Set<T> getIntersection(Set<T> s1, Set<T> s2) {
        Set<T> intersection = new HashSet<T>(s1);
        intersection.retainAll(s2);
        return intersection;
    }

    /**
     * Returns up to maxElements elements of the collection.  May create a new Collection or simply return the input collection.
     */
    public static <T> Collection<T> getFirst(Collection<T> collection, int maxElements) {
        if (CollectionUtils.isEmpty(collection)) {
            return Collections.emptySet();
        }

        // based on the resulting set, either pick only the first maxElements, or return the entire set as is
        int numNodes = collection.size();
        if (numNodes <= maxElements) {
            return collection;
        } else {
            // grab the first maxElements elements and put them in a collection to return
            List<T> returnList = new ArrayList<T>(maxElements);

            for (T node : collection) {
                returnList.add(node);
                if (returnList.size() >= maxElements) {
                    break;
                }
            }

            return returnList;
        }
    }

    public static <T extends Comparable<T>> T getMin(T... values) {
        return Collections.min(Arrays.asList(values));
    }

    /**
     * Filters a collection in place by removing all elements for which the given predicate
     * returns false.
     */
    public static <T> void filterInPlace(Collection<T> collection, Predicate<T> predicate) {
        if (collection != null && predicate != null) {
            for (Iterator<T> it = collection.iterator(); it.hasNext();) {
                if (!predicate.apply(it.next())) {
                    it.remove();
                }
            }
        }
    }

    public static Map<String, String> collapseMultiProperties(SetMultiMap<String, String> multiProperties) {
        if (multiProperties == null) {
            return null;
        }
        if (multiProperties.isEmpty()) {
            return Collections.emptyMap();
        }
        HashMap<String, String> map = new HashMap<String, String>();
        for (Map.Entry<String, Set<String>> entry : multiProperties.entrySet()) {
            Set<String> values = entry.getValue();
            map.put(entry.getKey(), values.iterator().next());
        }
        return map;
    }

    public static <T> List<T> getSafeSublist(List<T> values, int startRow, int maxRows) {
        int fromIndex = startRow < 0 ? 0 : startRow;
        int toIndex = fromIndex + maxRows > values.size() ? values.size() : fromIndex + maxRows;
        return new ArrayList<T>(values.subList(fromIndex, toIndex));
    }

    /**
     * Creates a set and adds all elements of input collection(s) into it.
     */
    public static <T> Set<T> union(Collection<T>... collectionsToAdd) {
        Set<T> returnValue = new HashSet<T>();
        for (Collection<T> ts : collectionsToAdd) {
            if (ts != null) {
                returnValue.addAll(ts);
            }
        }
        return returnValue;
    }

    public static <T> void truncate(final Collection<T> collection, final int limit) {
        filterInPlace(collection, new Predicate<T>() {
            int itemsToSkip = limit;

            @Override
            public boolean apply(T t) {
                if (itemsToSkip-- > 0) {
                    return true;
                }
                return false;
            }
        });
    }

    public static <T> boolean contains(Collection<T> collection, T element) {
        return collection != null && collection.contains(element);
    }

}
