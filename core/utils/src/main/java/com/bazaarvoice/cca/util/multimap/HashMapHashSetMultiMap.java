package com.bazaarvoice.cca.util.multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HashMapHashSetMultiMap<K, V> extends AbstractHashMapMultiMap<K, V, Set<V>> implements SetMultiMap<K, V> {

    public HashMapHashSetMultiMap() {
    }

    public HashMapHashSetMultiMap(Map<? extends K, ? extends Collection<? extends V>> map) {
        super(map);
    }

    @Override
    protected Set<V> createCollection(Collection<? extends V> col) {
        return (col != null) ? new HashSet<V>(col) : new HashSet<V>();
    }

    @Override
    protected AbstractHashMapMultiMap<K, V, Set<V>> newInstance(Map<K, Set<V>> map) {
        return new HashMapHashSetMultiMap<K, V>(map);
    }

    @Override
    protected Set<V> emptyCollection() {
        return Collections.emptySet();
    }
}
