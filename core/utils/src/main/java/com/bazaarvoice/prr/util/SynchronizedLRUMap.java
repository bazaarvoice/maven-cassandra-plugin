package com.bazaarvoice.prr.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A map that will replace the least recently used items.
 * <p>
 * Do NOT use this to implement a cache.  Intead, use
 * {@link com.bazaarvoice.cca.util.CacheMapStatisticsAdapter#newStandardLRUCache()}
 * since it (1) has better concurrency behavior, (2) integrates with the
 * garbage collector to avoid blowing out memory, and (3) tracks cache usage
 * statistics automatically.
 */
public class SynchronizedLRUMap<K,V> extends LinkedHashMap<K, V> {

    /**
     * The maximum allowable size of the map
     */
    private int _maxSize;

    /**
     * Creates a new SynchronizedLRUMap
     *
     * @param maxSize The maximum number of items that will be kept in the cache
     */
    public SynchronizedLRUMap(int maxSize) {
        super(16, 0.75f, true);
        _maxSize = maxSize;
    }

    //javadoc inherited
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > _maxSize;
    }

    //javadoc inherited
    public synchronized V get(Object key) {
        return super.get(key);
    }

    //javadoc inherited
    public synchronized V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }
}
