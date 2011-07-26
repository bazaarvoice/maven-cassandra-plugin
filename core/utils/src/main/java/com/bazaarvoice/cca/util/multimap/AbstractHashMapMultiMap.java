package com.bazaarvoice.cca.util.multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractHashMapMultiMap<K, V, C extends Collection<V>> implements MultiMap<K, V, C> {

    private Map<K, C> _map;

    protected AbstractHashMapMultiMap() {
        _map = createMap(16);
    }

    protected AbstractHashMapMultiMap(Map<? extends K, ? extends Collection<? extends V>> map) {
        _map = createMap((int) (map.size() * 1.4f));
        for (Map.Entry<? extends K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            // copy items to a new instance of collection
            _map.put(entry.getKey(), createCollection(entry.getValue()));
        }
    }

    /**
     * Creates target map with initial capacity.
     */
    protected Map<K, C> createMap(int capacity) {
        return new LinkedHashMap<K, C>(capacity);
    }

    /**
     * Gets the size of the collection mapped to the specified key.
     */
    @Override
    public int size(K key) {
        C col = _map.get(key);
        return (col != null) ? col.size() : 0;
    }

    /**
     * Returns the collection mapped to the specified key, guaranteed not to return null.
     */
    @Override
    public C getCollection(K key) {
        C col = _map.get(key);
        return (col != null) ? col : emptyCollection();
    }

    /**
     * Returns true if the collection mapped to the specified key contains the specified value.
     */
    @Override
    public boolean contains(K key, V value) {
        C col = _map.get(key);
        return (col != null) ? col.contains(value) : false;
    }

    @Override
    public List<V> collectionValues() {
        List<V> values = new ArrayList<V>();
        for (C col : _map.values()) {
            values.addAll(col);
        }
        return values;
    }

    /**
     * Adds the value to the collection associated with the specified key.
     */
    @Override
    public boolean add(K key, V value) {
        C col = _map.get(key);
        if (col == null) {
            col = createCollection(null);
            _map.put(key, col);
        }
        return col.add(value);
    }

    /**
     * Adds a collection of values to the collection associated with the specified key.
     */
    @Override
    public boolean addAll(K key, Collection<? extends V> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        C col = _map.get(key);
        if (col == null) {
            col = createCollection(values);
            if (col.isEmpty()) {
                return false;
            }
            _map.put(key, col);
            return true;
        }
        return col.addAll(values);
    }

    /**
     * Removes the value from the collection associated with the specified key.
     */
    @Override
    public boolean remove(K key, V value) {
        C col = _map.get(key);
        if (col == null) {
            return false;
        }
        boolean result = col.remove(value);
        if (col.isEmpty()) {
            _map.remove(key);
        }
        return result;
    }

    /**
     * Removes the value from the collection associated with the specified key.
     */
    @Override
    public boolean removeAll(K key, Collection<? extends V> values) {
        C col = _map.get(key);
        if (col == null) {
            return false;
        }
        boolean result = col.removeAll(values);
        if (col.isEmpty()) {
            _map.remove(key);
        }
        return result;
    }

    protected abstract AbstractHashMapMultiMap<K, V, C> newInstance(Map<K, C> map);

    @Override
    public Object clone() {
        return newInstance(_map);
    }

    /**
     * Creates a new instance of the map value Collection container.
     */
    protected abstract C createCollection(Collection<? extends V> col);

    /**
     * Creates an immutable empty instance of the map value Collection container.
     */
    protected abstract C emptyCollection();

    //
    // Map interface delegating method
    //

    @Override
    public int size() {
        return _map.size();
    }

    @Override
    public boolean isEmpty() {
        return _map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return _map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return _map.containsValue(value);
    }

    @Override
    public C get(Object key) {
        return _map.get(key);
    }

    @Override
    public C put(K key, C value) {
        return _map.put(key, value);
    }

    @Override
    public C remove(Object key) {
        return _map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends C> m) {
        _map.putAll(m);
    }

    @Override
    public void clear() {
        _map.clear();
    }

    @Override
    public Set<K> keySet() {
        return _map.keySet();
    }

    @Override
    public Collection<C> values() {
        return _map.values();
    }

    @Override
    public Set<Entry<K, C>> entrySet() {
        return _map.entrySet();
    }

    @Override
    public int hashCode() {
        return 31 + _map.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractHashMapMultiMap)) {
            return false;
        }
        return _map.equals(((AbstractHashMapMultiMap) o)._map);
    }

    @Override
    public String toString() {
        return _map.toString();
    }
}
