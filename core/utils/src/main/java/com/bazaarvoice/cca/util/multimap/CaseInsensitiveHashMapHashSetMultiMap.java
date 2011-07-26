package com.bazaarvoice.cca.util.multimap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CaseInsensitiveHashMapHashSetMultiMap<V> extends HashMapHashSetMultiMap<String, V> {

    public CaseInsensitiveHashMapHashSetMultiMap() {
    }

    public CaseInsensitiveHashMapHashSetMultiMap(Map<String, ? extends Collection<? extends V>> map) {
        super(map);
    }

    @Override
    protected Map<String, Set<V>> createMap(int capacity) {
        return new TreeMap<String, Set<V>>(String.CASE_INSENSITIVE_ORDER);
    }
}
