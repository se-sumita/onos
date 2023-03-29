package org.onosproject.opticalpathoptimizer.model;

import com.google.common.base.MoreObjects;
import org.onosproject.net.openroadm.model.AmpType;

/**
 * Model of amplifier for quality calculation.
 */
public class Amp extends Element  {
    private final double gain;
    private final AmpType ampType;

    /**
     * Constructor.
     * @param gain Gain
     * @param ampType Amplifier type
     */
    public Amp(double gain, AmpType ampType) {
        this.gain = gain;
        this.ampType = ampType;
    }

    /**
     * Get gain.
     * @return Gain
     */
    public double getGain() {
        return this.gain;
    }

    /**
     * Get amplifier type.
     * @return Amplifier type
     */
    public AmpType getAmpType() {
        return this.ampType;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("gain", gain)
                .add("ampType", ampType)
                .toString();
    }
}
