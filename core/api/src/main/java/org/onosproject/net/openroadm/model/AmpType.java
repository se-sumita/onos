package org.onosproject.net.openroadm.model;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Type of Amp.
 */
public final class AmpType {
    private static final Map<String, AmpType> INSTANCES = Maps.newLinkedHashMap();

    private final String name;

    /**
     * Constructor.
     * @param name name of amp type
     */
    private AmpType(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    /**
     * Get or create amp type object.
     * @param text name of type
     * @return type object
     */
    public static AmpType valueOf(final String text) {
        checkNotNull(text);
        checkArgument(text.length() > 0);
        String key = text.toLowerCase();
        AmpType instance = INSTANCES.get(key);
        if (instance != null) {
            return instance;
        }
        instance = new AmpType(text);
        INSTANCES.put(key, instance);
        return instance;
    }

    /**
     * Get whether is specified name contained in the type definition.
     * @param text name of type
     * @return true or false
     */
    public static boolean contains(final String text) {
        return INSTANCES.containsKey(text.toLowerCase());
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
        if (obj instanceof AmpType) {
            AmpType that = (AmpType) obj;
            return Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final AmpType LOW_GAIN_AMP = AmpType.valueOf("LowGainAmp");
    public static final AmpType HIGH_GAIN_AMP = AmpType.valueOf("HighGainAmp");
}
