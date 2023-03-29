package org.onosproject.net.openroadm.model;

import org.onosproject.net.OchSignal;

/**
 * Configuration for OCh port.
 */
public class OchState {
    private final OchSignal lambda;
    private final Rate rate;
    private final ModulationFormat modulationFormat;

    /**
     * Constructor.
     * @param lambda lambda
     */
    public OchState(OchSignal lambda) {
        this.lambda = lambda;
        this.rate = null;
        this.modulationFormat = null;
    }

    /**
     * Constructor.
     * @param lambda lambda
     * @param rate rate
     * @param modulationFormat modulation-format
     */
    public OchState(OchSignal lambda, Rate rate, ModulationFormat modulationFormat) {
        this.lambda = lambda;
        this.rate = rate;
        this.modulationFormat = modulationFormat;
    }

    public OchSignal lambda() {
        return lambda;
    }

    public Rate rate() {
        return rate;
    }

    public ModulationFormat modulationFormat() {
        return modulationFormat;
    }

    public boolean hasRatAndModulationFormat() {
        return rate != null && modulationFormat != null;
    }
}
