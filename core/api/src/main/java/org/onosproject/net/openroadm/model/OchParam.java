package org.onosproject.net.openroadm.model;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Parameter structure for OCh.
 */
public class OchParam {
    private final Rate rate;
    private final ModulationFormat modulationFormat;

    /**
     * Constructor.
     * @param rate rate
     * @param modulationFormat modulation-format
     */
    public OchParam(Rate rate, ModulationFormat modulationFormat) {
        this.rate = rate;
        this.modulationFormat = modulationFormat;
    }

    public Rate rate() {
        return rate;
    }

    public ModulationFormat modulationFormat() {
        return modulationFormat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rate, modulationFormat);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OchParam) {
            OchParam that = (OchParam) obj;
            return Objects.equals(rate, that.rate)
                    && Objects.equals(modulationFormat, that.modulationFormat);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("rate", rate)
                .add("modulationFormat", modulationFormat)
                .toString();
    }

    /**
     * Build OchParam object.
     * @param rate rate
     * @param modulationFormat modulation-format
     * @return built OChParam object
     */
    public static OchParam of(Rate rate, ModulationFormat modulationFormat) {
        return new OchParam(rate, modulationFormat);
    }
}
