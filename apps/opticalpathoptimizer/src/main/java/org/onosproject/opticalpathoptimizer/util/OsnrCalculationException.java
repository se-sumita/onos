package org.onosproject.opticalpathoptimizer.util;

/**
 * Exception in OSNR calculation.
 */
public class OsnrCalculationException extends Exception {

    public OsnrCalculationException(String message) {
        super(message);
    }

    public OsnrCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
