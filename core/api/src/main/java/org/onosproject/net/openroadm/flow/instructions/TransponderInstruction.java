package org.onosproject.net.openroadm.flow.instructions;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transponder instruction.
 */
public class TransponderInstruction extends AbstractExtension implements ExtensionTreatment {

    public static final int TREATMENT_TYPE_TRANSPONDER_ID = 301;

    /**
     * Extension type for TransponderInstruction.
     */
    public static final ExtensionTreatmentType TREATMENT_TRANSPONDER_TYPE
            = new ExtensionTreatmentType(TREATMENT_TYPE_TRANSPONDER_ID);

    private static final String RATE = "rate";
    private static final String MODULATION_FORMAT = "modulationFormat";

    private final KryoNamespace appKryo = new KryoNamespace.Builder().register(HashMap.class).build();

    private Rate rate;
    private ModulationFormat modulationFormat;

    /**
     * Build TransponderInstruction.
     * @param rate rate
     * @param modulationFormat modulation format
     * @return built TransponderInstruction
     */
    public static TransponderInstruction of(Rate rate, ModulationFormat modulationFormat) {
        return new TransponderInstruction(rate, modulationFormat);
    }

    public Rate rate() {
        return rate;
    }

    public ModulationFormat modulationFormat() {
        return modulationFormat;
    }

    /**
     * Constructor.
     */
    public TransponderInstruction() {
        this.rate = null;
        this.modulationFormat = null;
    }

    /**
     * Constructor.
     * @param rate rate
     * @param modulationFormat modulation format
     */
    public TransponderInstruction(Rate rate, ModulationFormat modulationFormat) {
        this.rate = checkNotNull(rate);
        this.modulationFormat = checkNotNull(modulationFormat);
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
        if (obj instanceof TransponderInstruction) {
            TransponderInstruction that = (TransponderInstruction) obj;
            if (Objects.equals(rate, that.rate) &&
                    Objects.equals(modulationFormat, that.modulationFormat)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("rate", rate)
                .add("modulation-format", modulationFormat)
                .toString();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put(RATE, rate.toString());
        values.put(MODULATION_FORMAT, modulationFormat.name());
        return appKryo.serialize(values);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        rate = Rate.valueOf((String) values.get(RATE));
        modulationFormat = ModulationFormat.valueOf((String) values.get(MODULATION_FORMAT));
    }

    @Override
    public ExtensionTreatmentType type() {
        return TREATMENT_TRANSPONDER_TYPE;
    }
}
