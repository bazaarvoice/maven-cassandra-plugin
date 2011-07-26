package com.bazaarvoice.cca.util.multimap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
public interface MultiMap<K, V, C extends Collection<V>> extends Map<K, C>, Cloneable {
    /**
     * Gets the size of the collection mapped to the specified key.
     */
    int size(K key);

    /**
     * Returns the collection mapped to the specified key, guaranteed not to return null.
     */
    C getCollection(K key);

    /**
     * Returns true if the collection mapped to the specified key contains the specified value.
     */
    boolean contains(K key, V value);

    /**
     * Returns a snapshot collection of all values in all collections.
     */
    List<V> collectionValues();

    /**
     * Adds the value to the collection associated with the specified key.
     */
    boolean add(K key, V value);

    /**
     * Adds a collection of values to the collection associated with the specified key.
     */
    boolean addAll(K key, Collection<? extends V> values);

    /**
     * Removes the value from the collection associated with the specified key.
     */
    boolean remove(K key, V value);

    /**
     * Removes the value from the collection associated with the specified key.
     */
    boolean removeAll(K key, Collection<? extends V> values);

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Object clone();
}
