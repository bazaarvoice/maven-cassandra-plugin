package com.bazaarvoice.cca.util.multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 */
public class HashMapTreeSetMultiMap<K, V> extends AbstractHashMapMultiMap<K, V, Set<V>> implements SetMultiMap<K, V> {

    public HashMapTreeSetMultiMap() {
    }

    public HashMapTreeSetMultiMap(Map<? extends K, ? extends Collection<? extends V>> map) {
        super(map);
    }

    @Override
    protected Set<V> createCollection(Collection<? extends V> col) {
        return (col != null) ? new TreeSet<V>(col) : new TreeSet<V>();
    }

    @Override
    protected AbstractHashMapMultiMap<K, V, Set<V>> newInstance(Map<K, Set<V>> map) {
        return new HashMapTreeSetMultiMap<K, V>(map);
    }

    @Override
    protected Set<V> emptyCollection() {
        return Collections.emptySet();
    }
}
