package com.bazaarvoice.cca.util.multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 */
public class HashMapLinkedListMultiMap<K, V> extends AbstractHashMapMultiMap<K, V, List<V>> implements ListMultiMap<K, V> {

    public HashMapLinkedListMultiMap() {
    }

    public HashMapLinkedListMultiMap(Map<? extends K, ? extends Collection<? extends V>> map) {
        super(map);
    }

    @Override
    protected List<V> createCollection(Collection<? extends V> col) {
        return (col != null) ? new LinkedList<V>(col) : new LinkedList<V>();
    }

    @Override
    protected AbstractHashMapMultiMap<K, V, List<V>> newInstance(Map<K, List<V>> map) {
        return new HashMapLinkedListMultiMap<K, V>(map);
    }

    @Override
    protected List<V> emptyCollection() {
        return Collections.emptyList();
    }
}
