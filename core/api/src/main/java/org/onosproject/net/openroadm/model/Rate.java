package org.onosproject.net.openroadm.model;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Type of Rate.
 */
public final class Rate {
    private static final Map<String, Rate> INSTANCES = Maps.newLinkedHashMap();

    private final String name;

    /**
     * Constructor.
     * @param name name of rate type
     */
    private Rate(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /**
     * Get or create rate type object.
     * @param text name of type
     * @return type object
     */
    public static Rate valueOf(String text) {
        checkNotNull(text);
        checkArgument(text.length() > 0);
        String key = text.toUpperCase();
        Rate instance = INSTANCES.get(key);
        if (instance != null) {
            return instance;
        }
        instance = new Rate(text);
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
        if (obj instanceof Rate) {
            Rate that = (Rate) obj;
            return Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final Rate R100G = Rate.valueOf("R100G");
    public static final Rate R150G = Rate.valueOf("R150G");
    public static final Rate R200G = Rate.valueOf("R200G");
}
