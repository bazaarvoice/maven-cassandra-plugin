package com.bazaarvoice.cca.util.multimap;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MultiMap analog to the java.util.Collections class.
 */
public abstract class MultiMaps {
    private static final ListMultiMap EMPTY_LIST_MULTI_MAP = new EmptyListMultiMap();
    private static final SetMultiMap EMPTY_SET_MULTI_MAP = new EmptySetMultiMap();

    private MultiMaps() {}

    /** Utility method can be used to avoid respecifying the generic argument. */
    public static <K, V> ListMultiMap<K, V> newHashMapArrayListMultiMap() {
        return new HashMapArrayListMultiMap<K, V>();
    }

    /** Utility method can be used to avoid respecifying the generic argument. */
    public static <K, V> SetMultiMap<K, V> newHashMapHashSetMultiMap() {
        return new HashMapHashSetMultiMap<K, V>();
    }

    public static <K, V> ListMultiMap<K, V> emptyListMultiMap() {
        //noinspection unchecked
        return EMPTY_LIST_MULTI_MAP;
    }

    public static <K, V> SetMultiMap<K, V> emptySetMultiMap() {
        //noinspection unchecked
        return EMPTY_SET_MULTI_MAP;
    }
    
    private static abstract class EmptyMultiMap<K, V, C extends Collection<V>>
            extends AbstractMap<K, C> implements MultiMap<K, V, C> {
        public boolean isEmpty() {
            return true;
        }
        public int size() {
            return 0;
        }
        public int size(K key) {
            return 0;
        }
        public boolean contains(K key, V value) {
            return false;
        }
        public boolean containsKey(Object key) {
            return false;
        }
        public boolean containsValue(Object value) {
            return false;
        }
        public C get(Object key) {
            return null;
        }
        public Set<K> keySet() {
            return Collections.emptySet();
        }
        public Collection<C> values() {
            return Collections.emptySet();
        }
        public Set<Entry<K, C>> entrySet() {
            return Collections.emptySet();
        }
        public List<V> collectionValues() {
            return Collections.emptyList();
        }
        public void clear() {
            throw new UnsupportedOperationException();
        }
        public C put(K key, C value) {
            throw new UnsupportedOperationException();
        }
        public void putAll(Map<? extends K, ? extends C> m) {
            throw new UnsupportedOperationException();
        }
        public boolean add(K key, V value) {
            throw new UnsupportedOperationException();
        }
        public boolean addAll(K key, Collection<? extends V> values) {
            throw new UnsupportedOperationException();
        }
        public C remove(Object key) {
            throw new UnsupportedOperationException();
        }
        public boolean remove(K key, V value) {
            throw new UnsupportedOperationException();
        }
        public boolean removeAll(K key, Collection<? extends V> values) {
            throw new UnsupportedOperationException();
        }
        public Object clone() {
            return this;
        }
    }

    private static class EmptyListMultiMap<K, V> extends EmptyMultiMap<K, V, List<V>> implements ListMultiMap<K, V> {
        public List<V> getCollection(K key) {
            return Collections.emptyList();
        }
    }
    
    private static class EmptySetMultiMap<K, V> extends EmptyMultiMap<K, V, Set<V>> implements SetMultiMap<K, V> {
        public Set<V> getCollection(K key) {
            return Collections.emptySet();
        }
    }
}
