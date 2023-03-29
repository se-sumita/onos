package org.onosproject.drivers.openroadm.common;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.openroadm.behaviors.RoadmPortQuery;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.openroadm.Annotation.OPTICAL_INTERFACE_TYPE;
import static org.onosproject.net.openroadm.Annotation.TYPE_OMS_ADD_DROP;

/**
 * Default implementation of {@link RoadmPortQuery}.
 */
public class DefaultRoadmPortQuery extends AbstractHandlerBehaviour implements RoadmPortQuery {

    /**
     * Get a list of OMS Add/Drop ports.
     * @return list of ports
     */
    @Override
    public List<Port> getOmsAddDropPorts() {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        return deviceService.getPorts(deviceId).stream()
                .filter(p -> TYPE_OMS_ADD_DROP.equals(p.annotations().value(OPTICAL_INTERFACE_TYPE)))
                .collect(Collectors.toList());
    }

    /**
     * Check for OMS Add/Drop port.
     * @param portNumber port number
     * @return `true` if specified port is OMS Add/Drop
     */
    @Override
    public boolean isOmsAddDropPort(PortNumber portNumber) {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Port port = deviceService.getPort(deviceId, portNumber);
        if (port == null) {
            return false;
        }
        return TYPE_OMS_ADD_DROP.equals(port.annotations().value(OPTICAL_INTERFACE_TYPE));
    }
}
