package com.bazaarvoice.core.environment;

/**
 * Exception to indicate that the application failed to be initialized.
 */
public class InitializationFailedException extends RuntimeException {

    /**
     * Creates a InitializationFailedException
     */
    public InitializationFailedException() {
        super();
    }

    /**
     * Creates a InitializationFailedException with the specified detail message.
     *
     * @param s The error message
     */
    public InitializationFailedException(String s) {
        super(s);
    }

    /**
     * Creates a InitializationFailedException with the specified detail message
     * and underlying root Exception
     *
     * @param s     The error message
     * @param cause The underlying root Exception
     */
    public InitializationFailedException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Creates a InitializationFailedException with the
     * underlying root Exception
     *
     * @param cause The underlying root exception
     */
    public InitializationFailedException(Throwable cause) {
        super(cause);
    }
}
