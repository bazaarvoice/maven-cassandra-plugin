package com.bazaarvoice.cca.util.multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HashMapArrayListMultiMap<K, V> extends AbstractHashMapMultiMap<K, V, List<V>> implements ListMultiMap<K, V> {

    public HashMapArrayListMultiMap() {
    }

    public HashMapArrayListMultiMap(Map<? extends K, ? extends Collection<? extends V>> map) {
        super(map);
    }

    @Override
    protected List<V> createCollection(Collection<? extends V> col) {
        return (col != null) ? new ArrayList<V>(col) : new ArrayList<V>();
    }

    @Override
    protected List<V> emptyCollection() {
        return Collections.emptyList();
    }

    @Override
    protected AbstractHashMapMultiMap<K, V, List<V>> newInstance(Map<K, List<V>> map) {
        return new HashMapArrayListMultiMap<K, V>(map);
    }

    public void trimToSize() {
        for (List<V> list : values()) {
            ((ArrayList<V>) list).trimToSize();
        }
    }
}
