package org.onosproject.opticalpathstore;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.openroadm.model.WavelengthPath;

import java.util.Collections;
import java.util.Set;

/**
 * Wavelength path event.
 */
public class WavelengthPathEvent extends AbstractEvent<WavelengthPathEvent.Type, WavelengthPath> {

    public enum Type {
        PATH_ADDED,
        PATH_UPDATED,
        PATH_REMOVED
    }

    private final Set<Long> coupledServices;

    public WavelengthPathEvent(Type type, WavelengthPath subject, Set<Long> coupledServices) {
        super(type, subject);
        this.coupledServices = coupledServices;
    }

    public WavelengthPathEvent(Type type, WavelengthPath subject) {
        super(type, subject);
        this.coupledServices = Collections.emptySet();
    }

    public Set<Long> coupledServices() {
        return coupledServices;
    }
}
