package com.bazaarvoice.core.logging.util;

/**
 * Object placed in the log4j Mapped Diagnostic Context (MDC) that delays construction
 * and sanitization of the actual string data until it's actually used.
 */
public abstract class LoggingContextData {

    private String _string;

    protected abstract String constructString();

    @Override
    public final String toString() {
        if (_string == null) {
            try {
                _string = sanitize(constructString());
            } catch (Throwable t) {
                _string = "<error-generating-log-string>";
            }
        }
        return _string;
    }

    private static String sanitize(final String string) {
        if (string == null) {
            return null;
        }
        // sanitize control characters that could screw up the log file formatting.  but error on the
        // side of leaving unicode characters as-is so urls can be copy and pasted from log4j e-mails.
        int len = string.length();
        StringBuilder buf = null;
        for (int i = 0; i < len; i++) {
            // convert control characters (everything < 32 on the ascii chart) to space characters
            if (string.charAt(i) < 32) {
                if (buf == null) {
                    buf = new StringBuilder(string);  // delay allocating the buffer until we need it...
                }
                buf.setCharAt(i, ' ');
            }
        }
        return (buf != null) ? buf.toString() : string;
    }
}
