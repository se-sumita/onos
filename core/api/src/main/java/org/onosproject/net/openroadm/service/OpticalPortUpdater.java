package org.onosproject.net.openroadm.service;

import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceStore;
import org.onosproject.net.device.PortDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Port description updater service.
 */
@Component(immediate = true)
public class OpticalPortUpdater implements OpticalPortUpdaterService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    public void updatePortDescription(Device device, PortDescription newDescription) {
        checkNotNull(device);
        DeviceEvent event = store.updatePortStatus(device.providerId(), device.id(), newDescription);
        if (event != null) {
            eventDispatcher.post(event);
        }
    }
}
