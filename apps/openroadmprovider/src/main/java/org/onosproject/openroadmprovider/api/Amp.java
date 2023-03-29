package org.onosproject.openroadmprovider.api;

import com.google.common.base.MoreObjects;
import org.onosproject.net.openroadm.model.AmpType;

/**
 * Amp information.
 */
public class Amp {
    private final AmpType ampType;
    private final double gain;

    /**
     * Constructor.
     * @param ampType type of amp
     * @param gain gain
     */
    public Amp(AmpType ampType, double gain) {
        this.ampType = ampType;
        this.gain = gain;
    }

    public AmpType ampType() {
        return ampType;
    }

    public double gain() {
        return gain;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Amp.class)
                .add("ampType", ampType.name())
                .add("gain", gain)
                .toString();
    }
}
