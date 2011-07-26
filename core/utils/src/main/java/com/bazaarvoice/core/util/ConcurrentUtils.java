package com.bazaarvoice.core.util;

import com.bazaarvoice.core.logging.util.LoggingContextThreadFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ConcurrentUtils {

    public static ScheduledExecutorService newDaemonScheduledThreadPool(int corePoolSize, String activityName) {
        return Executors.newScheduledThreadPool(corePoolSize, new LoggingContextThreadFactory(activityName, LoggingContextThreadFactory.Type.DAEMON));
    }

    public static ExecutorService newSingleThreadExecutor(String activityName) {
        return newFixedThreadPool(1, activityName);
    }

    /**
     * Returns a thread pool that is very similar to Executors.newFixedThreadPool() plus it integrates
     * with the log4j functionality integrated with {@link com.bazaarvoice.core.logging.util.LoggingContextUtils}.
     */
    public static ExecutorService newFixedThreadPool(int numThreads, String activityName) {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                numThreads, numThreads,
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(/*unbounded*/),
                new LoggingContextThreadFactory(activityName));
        threadPool.allowCoreThreadTimeOut(true);  // protect from non-daemon threads keeping the app alive
        return threadPool;
    }

    /**
     * Returns a thread pool that is very similar to Executors.newFixedThreadPool() except that, once the
     * number of queued items grows larger than 'maxPending', the tasks get executed by the caller thread
     * instead of a worker thread.  This has the effect of slowing down the producer thread so that the
     * producer can't get ahead of the consumer thread by an unbounded amount, potentially using all memory.
     */
    public static ExecutorService newBoundedThreadPool(int maxThreads, int maxPending, String activityName) {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                maxThreads, maxThreads,
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(maxPending),
                new LoggingContextThreadFactory(activityName),
                new ThreadPoolExecutor.CallerRunsPolicy());
        threadPool.allowCoreThreadTimeOut(true);  // protect from non-daemon threads keeping the app alive
        return threadPool;
    }

    /**
     * Maps a key to the specified value, unless key the key is already mapped to
     * a value.
     * <p/>
     * Equivalent to the following operation, except performed atomically:
     * <pre>
     *   V oldValue = map.get(key);
     *   if (oldValue != null) {
     *      return oldValue;
     *   } else {
     *      map.put(key, newValue);
     *      return newValue;
     *   }
     * </pre>
     *
     * @return returns the value the key is mapped to in the map after the operation.
     */
    public static <K, V> V safePut(ConcurrentMap<K, V> map, K key, V newValue) {
        V oldValue = map.putIfAbsent(key, newValue);
        return (oldValue != null) ? oldValue : newValue;
    }
}
