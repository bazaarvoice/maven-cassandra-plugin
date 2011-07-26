package com.bazaarvoice.core.logging.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initializes the log4j MDC to predictable values when new threads are created.
 * Without this, the MDC values will be cloned from the thread that happens to
 * trigger creation of a new thread in the thread pool.
 */
public class LoggingContextThreadFactory implements ThreadFactory {
    public enum Type { NORMAL, DAEMON };

    private final ThreadGroup _group;
    private final AtomicInteger _threadNumber = new AtomicInteger(0);
    private final Type _type;
    private final String _activity;

    public LoggingContextThreadFactory(String activity) {
        this(activity, Type.NORMAL);
    }

    public LoggingContextThreadFactory(String activity, Type type) {
        SecurityManager s = System.getSecurityManager();
        _group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        _type = type;
        _activity = activity;
    }

    @Override
    public Thread newThread(final Runnable task) {
        String name = _activity + " [Thread-" + _threadNumber.incrementAndGet() + "]";
        Thread thread = new Thread(_group, new Runnable() {
            @Override
            public void run() {
                LoggingContextUtils.clear();
                LoggingContextUtils.setActivity(_activity);

                task.run();
            }
        }, name);
        thread.setDaemon(_type == Type.DAEMON);
        return thread;
    }
}
