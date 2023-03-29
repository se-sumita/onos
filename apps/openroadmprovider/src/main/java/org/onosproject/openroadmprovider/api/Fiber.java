package org.onosproject.openroadmprovider.api;

import com.google.common.base.MoreObjects;
import org.onosproject.net.openroadm.model.FiberType;

/**
 * Fiber information.
 */
public class Fiber {
    private final FiberType fiberType;
    private final double spanloss;
    private final int srlgLength;

    /**
     * Constructor.
     * @param fiberType type of fiber
     * @param spanloss span loss
     * @param srlgLength SRLG length
     */
    public Fiber(FiberType fiberType, double spanloss, int srlgLength) {
        this.fiberType = fiberType;
        this.spanloss = spanloss;
        this.srlgLength = srlgLength;
    }

    public FiberType fiberType() {
        return fiberType;
    }

    public double spanloss() {
        return spanloss;
    }

    public int srlgLength() {
        return srlgLength;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Fiber.class)
                .add("fiberType", fiberType)
                .add("spanloss", spanloss)
                .add("SRLGLength", srlgLength)
                .toString();
    }
}
