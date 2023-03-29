package org.onosproject.opticalpathoptimizer.model;

/**
 * Exception when accessing the OpenROADM model.
 */
public class OpenRoadmModelException extends Exception {
    /**
     * Constructor.
     * @param message Error message
     */
    public OpenRoadmModelException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param message Error message
     * @param ex Parent exception
     */
    public OpenRoadmModelException(String message, Throwable ex) {
        super(message, ex);
    }
}
