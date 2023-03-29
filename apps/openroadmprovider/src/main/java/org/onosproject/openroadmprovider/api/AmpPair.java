package org.onosproject.openroadmprovider.api;

import com.google.common.base.MoreObjects;

/**
 * Pair of Amp.
 */
public class AmpPair {
    private final Amp aEndAmp;
    private final Amp bEndAmp;

    /**
     * Constructor.
     * @param aEndAmp a-end amp
     * @param bEndAmp b-end amp
     */
    public AmpPair(Amp aEndAmp, Amp bEndAmp) {
        this.aEndAmp = aEndAmp;
        this.bEndAmp = bEndAmp;
    }

    public Amp aEndAmp() {
        return aEndAmp;
    }

    public boolean hasAEndAmp() {
        return aEndAmp != null;
    }

    public Amp bEndAmp() {
        return bEndAmp;
    }

    public boolean hasBEndAmp() {
        return bEndAmp != null;
    }

    /**
     * Get an Amp with a-end and b-end swapped.
     * @return Amplifier in reverse direction
     */
    public AmpPair reverse() {
        return new AmpPair(bEndAmp, aEndAmp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(AmpPair.class)
                .add("A-end-amp", aEndAmp)
                .add("B-end-amp", bEndAmp)
                .toString();
    }
}
