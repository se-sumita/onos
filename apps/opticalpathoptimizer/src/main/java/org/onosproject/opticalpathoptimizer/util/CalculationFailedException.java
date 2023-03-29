package org.onosproject.opticalpathoptimizer.util;

public class CalculationFailedException extends RuntimeException {

    public CalculationFailedException(String message) {
        super(message);
    }

    public CalculationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
