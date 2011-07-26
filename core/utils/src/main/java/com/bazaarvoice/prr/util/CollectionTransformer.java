package com.bazaarvoice.prr.util;

import com.bazaarvoice.cca.util.multimap.HashMapArrayListMultiMap;
import com.bazaarvoice.cca.util.multimap.ListMultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offers several ways to transform a collection into another collection, based on a new value calculated from the collection values.
 */
public class CollectionTransformer {

    /**
     * Transforms each item of given list by given transformer and returns list of transformed items
     *
     * @param collection  Source list
     * @param transformer Transformer
     * @return Returns transformed list. Order preserved
     */
    public static <T,V> List<V> transform(Collection<? extends T> collection, Transformer<T, V> transformer) {
        List<V> result = new ArrayList<V>(collection.size());
        for (T item : collection) {
            result.add(transformer.transform(item));
        }
        return result;
    }

    /**
     * Groups a collection of items by their transformed value.
     * @param collection  Source list
     * @param transformer Transformer
     * @return A map of transformed value to their corresponding items.
     */
    public static <T, C extends T, V> ListMultiMap<V, C> group(Collection<C> collection, Transformer<T, V> transformer) {
        // use LinkedHashMap and ArrayList so order is preserved
        ListMultiMap<V, C> multiMap = new HashMapArrayListMultiMap<V, C>();
        for (C item : collection) {
            multiMap.add(transformer.transform(item), item);
        }
        return multiMap;
    }

    /**
     * Creates a map of itemKey to item given a collection of items. Key and item should be in one-to-one relationship.
     * @param collection Source list
     * @param transformer Transformer which transforms an item into a key for that item
     * @return A map of transformed value to its corresponding item.
     */
    public static <T, C extends T, V> Map<V, C> map(Collection<C> collection, Transformer<T, V> transformer) {
        Map<V, C> map = new HashMap<V, C>();
        for (C item : collection) {
            map.put(transformer.transform(item), item);
        }
        return map;
    }

    /**
     * Determines the distinct values and counts the instances.  Relies on object equals.
     * @param collection Source list
     * @param transformer Transformer
     * @return A map of transformed value to the corresponding count.  Order of unique entries is preserved.
     */
    public static <T, V> Map<V, Integer> count(Collection<? extends T> collection, Transformer<T, V> transformer) {
        LinkedHashMap<V, Integer> result = new LinkedHashMap<V, Integer>();
        for (T item : collection) {
            V key = transformer.transform(item);
            Integer value = result.get(key);
            result.put(key, value != null ? value + 1 : 1);
        }
        return result;
    }
}
