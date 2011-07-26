package com.bazaarvoice.core.jms.service;

/**
 * Chameleon runtime exception used to reconstruct server-side exception chains on the
 * client-side of a remote method invocation.  The toString() and printStackTrace()
 * methods are implemented so that client-side error reporting includes full details
 * of the server-side messages and stack traces.  
 */
public class RemoteMessageException extends RuntimeException {

    private final String _className;

    public RemoteMessageException(String className, String message, StackTraceElement[] stackTraceElements, Throwable cause) {
        super(message, cause);
        _className = className;
        setStackTrace(stackTraceElements);
    }

    @Override
    public Throwable fillInStackTrace() {
        // do nothing since we'll replace the stack trace in the constructor
        return this;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("[remote] ").append(_className);
        String message = getLocalizedMessage();
        if (message != null) {
            buf.append(": ").append(message);
        }
        return buf.toString();
    }
}
