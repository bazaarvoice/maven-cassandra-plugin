package com.bazaarvoice.core.jms.util;

/**
 * @author Ivan Luzyanin
 */
public interface PausableServiceController {
    boolean isStarted();

    void start();

    void stop();
}
