package com.bazaarvoice.core.logging.util;

import org.apache.log4j.MDC;

import java.util.Hashtable;

public abstract class LoggingContextUtils {

    // Constants for the Log4j Mapped Diagnostic Context used to set strings that can appear in each log entry.
    // We use the MDC instead of the NDC since (a) the MDC does not leak memory if threads are frequently created and
    // destroyed plus (b) the MDC uses an InheritableThreadLocal so jobs that spawn threads will inherit the context.
    public static final String MDC_ACTIVITY = "activity";
    public static final String MDC_ON_BEHALF_OF = "on-behalf-of";
    public static final String MDC_REQUEST_DETAIL = "request-detail";

    /**
     * Sets a name describing what activity is being performed.  Usually this is filename from
     * the url, but could be other things such as a JMS queue name.
     */
    public static void setActivity(LoggingContextData activity) {
        set(MDC_ACTIVITY, activity);
    }
    public static void setActivity(String activity) {
        set(MDC_ACTIVITY, activity);
    }

    /**
     * Sets a name describing who the activity is on behalf of.  Usually this is a display code
     * or a client name.
     */
    public static void setOnBehalfOf(LoggingContextData onBehalfOf) {
        set(MDC_ON_BEHALF_OF, onBehalfOf);
    }

    public static void setOnBehalfOf(String onBehalfOf) {
        set(MDC_ON_BEHALF_OF, onBehalfOf);
    }
    public static void setOnBehalfOfIfNull(String onBehalfOf) {
        if (!has(MDC_ON_BEHALF_OF) && onBehalfOf != null) {
            set(MDC_ON_BEHALF_OF, onBehalfOf);
        }
    }

    /**
     * Sets a detailed description of the request.  This is usually too long and verbose for log
     * files, but is included in log4j e-mails.
     */
    public static void setRequestDetail(LoggingContextData requestDetail) {
        set(MDC_REQUEST_DETAIL, requestDetail);
    }


    /**
     * All setting of MDC values should go through this method which enforces use of
     * LoggingContextData which sanitizes special characters in string values.
     */
    private static void set(String key, LoggingContextData value) {
        if (value != null) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }
    private static void set(String key, String string) {
        set(key, string != null ? new StringData(string) : null);
    }

    private static boolean has(String key) {
        return MDC.get(key) != null;
    }

    public static void clear() {
        Hashtable context = MDC.getContext();
        if (context != null) {
            context.remove(MDC_ACTIVITY);
            context.remove(MDC_ON_BEHALF_OF);
            context.remove(MDC_REQUEST_DETAIL);
        }
    }

    private static class StringData extends LoggingContextData {
        private final String _activity;

        public StringData(String activity) {
            _activity = activity;
        }

        @Override
        protected String constructString() {
            return _activity;
        }
    }
}