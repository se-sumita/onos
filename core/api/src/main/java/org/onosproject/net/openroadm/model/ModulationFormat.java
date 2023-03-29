package org.onosproject.net.openroadm.model;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Type of Modulation format.
 */
public final class ModulationFormat {
    private static final Map<String, ModulationFormat> INSTANCES = Maps.newLinkedHashMap();

    private final String name;

    /**
     * Constructor.
     * @param name name of modulation format
     */
    private ModulationFormat(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /**
     * Get or create modulation format type object.
     * @param text name of type
     * @return type object
     */
    public static ModulationFormat valueOf(String text) {
        checkNotNull(text);
        checkArgument(text.length() > 0);
        String key = text.toLowerCase();
        ModulationFormat instance = INSTANCES.get(key);
        if (instance != null) {
            return instance;
        }
        instance = new ModulationFormat(text);
        INSTANCES.put(key, instance);
        return instance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ModulationFormat) {
            ModulationFormat that = (ModulationFormat) obj;
            return Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final ModulationFormat DP_QPSK = ModulationFormat.valueOf("dp-qpsk");
    public static final ModulationFormat DP_QAM8 = ModulationFormat.valueOf("dp-qam8");
    public static final ModulationFormat DP_QAM16 = ModulationFormat.valueOf("dp-qam16");
}
