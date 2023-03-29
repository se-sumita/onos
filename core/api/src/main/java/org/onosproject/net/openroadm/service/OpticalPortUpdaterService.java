package org.onosproject.net.openroadm.service;

import org.onosproject.net.Device;
import org.onosproject.net.device.PortDescription;

/**
 * Port description updater service interface.
 */
public interface OpticalPortUpdaterService {
    void updatePortDescription(Device device, PortDescription newDescription);
}
