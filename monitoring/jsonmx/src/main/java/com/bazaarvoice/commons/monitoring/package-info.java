/**
 * This package contains the necessary components to output monitored data from JMX in Json format over HTTP.
 * There are two methods available to do so, represented by two different classes.
 * <ol>
 *     <li>{@link com.bazaarvoice.commons.monitoring.ApplicationMonitor ApplicationMonitor}, which starts a standalone HTTP server on a single port.</li>
 *     <li>{@link com.bazaarvoice.commons.monitoring.ApplicationMonitorServlet ApplicationMonitorServlet}, which starts a servlet to be used in a servlet container.</li>
 * </ol>
 */
package com.bazaarvoice.commons.monitoring;