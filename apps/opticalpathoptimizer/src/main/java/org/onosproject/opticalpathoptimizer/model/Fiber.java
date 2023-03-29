package org.onosproject.opticalpathoptimizer.model;

import com.google.common.base.MoreObjects;
import org.onosproject.net.openroadm.model.FiberType;

/**
 * Fiber model for quality calculation.
 */
public class Fiber extends Element {
    private final double spanLoss;
    private final double srlgLen;
    private final FiberType fiberType;

    /**
     * Constructor.
     * @param spanLoss Span loss [dB]
     * @param fiberType Fiber type
     * @param srlgLen SRLG-length [m]
     */
    public Fiber(double spanLoss, FiberType fiberType, double srlgLen) {
        this.spanLoss = spanLoss;
        this.srlgLen = srlgLen;
        this.fiberType = fiberType;
    }

    /**
     * Get fiber type.
     * @return Fiber type
     */
    public FiberType getFiberType() {
        return this.fiberType;
    }

    /**
     * Get span loss.
     * @return Span loss [dB]
     */
    public double getSpanLoss() {
        return this.spanLoss;
    }

    /**
     * Get SRLG length.
     * @return SRLG length [m]
     */
    public double getSrlgLen() {
        return this.srlgLen;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("spanLoss", spanLoss)
                .add("SRLGLen", srlgLen)
                .add("fiberType", fiberType)
                .toString();
    }
}
