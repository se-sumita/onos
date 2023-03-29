package org.onosproject.opticalpathstore;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.openroadm.model.WdmPath;

import java.util.Collection;
import java.util.Collections;

/**
 * WDM path event.
 */
public class WdmPathEvent extends AbstractEvent<WdmPathEvent.Type, Collection<WdmPath>> {

    public enum Type {
        PATHS_REPLACED,
        PATHS_CLEARED
    }

    private final Collection<WdmPath> removed;

    public WdmPathEvent(Type type, Collection<WdmPath> added, Collection<WdmPath> removed) {
        super(type, added);
        this.removed = removed;
    }

    public WdmPathEvent(Type type) {
        super(type, Collections.emptyList());
        removed = Collections.emptyList();
    }

    public Collection<WdmPath> getRemoved() {
        return removed;
    }
}
