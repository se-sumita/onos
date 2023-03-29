package org.onosproject.net.openroadm.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Type of Fiber.
 */
public final class FiberType {
    private static final Map<String, FiberType> INSTANCES = Maps.newLinkedHashMap();

    private final String name;

    /**
     * Constructor.
     * @param name name of fiber type
     */
    private FiberType(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    /**
     * Get or create fiber type object.
     * @param text name of type
     * @return type object
     */
    public static FiberType valueOf(final String text) {
        checkNotNull(text);
        checkArgument(text.length() > 0);
        String key = text.toLowerCase();
        FiberType instance = INSTANCES.get(key);
        if (instance != null) {
            return instance;
        }
        instance = new FiberType(text);
        INSTANCES.put(key, instance);
        return instance;
    }

    /**
     * Get type set.
     * @return type set
     */
    public static Set<String> types() {
        return ImmutableSet.copyOf(INSTANCES.keySet());
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
        if (obj instanceof FiberType) {
            FiberType that = (FiberType) obj;
            return Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final FiberType SMF = FiberType.valueOf("smf");
    public static final FiberType DSF = FiberType.valueOf("dsf");
}
