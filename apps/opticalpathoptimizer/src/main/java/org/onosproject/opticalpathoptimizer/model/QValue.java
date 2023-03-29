package org.onosproject.opticalpathoptimizer.model;

/**
 * Q-value and lower threshold of Q-value.
 */
public final class QValue {
    private double qValue;
    private double qThreshold;

    /**
     * Constructor.
     * @param qValue Q-value
     * @param qThreshold lower threshold of Q-value
     */
    private QValue(double qValue, double qThreshold) {
        this.qValue = qValue;
        this.qThreshold = qThreshold;
    }

    public double qValue() {
        return qValue;
    }

    public double qThreshold() {
        return qThreshold;
    }

    public static QValue of(double qValue, double qThreshold) {
        return new QValue(qValue, qThreshold);
    }
}
