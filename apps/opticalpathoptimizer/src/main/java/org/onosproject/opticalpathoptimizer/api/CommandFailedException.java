package org.onosproject.opticalpathoptimizer.api;

/**
 * Exception class for command execution failed.
 */
public class CommandFailedException extends RuntimeException {
    /**
     * Constructor.
     * @param message Error message
     */
    public CommandFailedException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param message Error message
     * @param parent Parent exception
     */
    public CommandFailedException(String message, Throwable parent) {
        super(message, parent);
    }
}
