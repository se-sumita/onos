package org.onosproject.net.openroadm.behaviors;

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import java.util.Collections;
import java.util.List;


/**
 * Utility for getting port information of ROADM.
 */
public final class RoadmPortUtils {

    private RoadmPortUtils() {}

    /**
     * Get the RoadmPortQuery behaviour.
     * @param device Device
     * @return RoadmPortQuery behaviour (null if the driver is not supported)
     */
    private static RoadmPortQuery getRoadmPortQuery(Device device) {
        if (device != null && device.is(RoadmPortQuery.class)) {
            return device.as(RoadmPortQuery.class);
        }
        return null;
    }

    /**
     * Get list of OMS-Add/Drop ports.
     * @param device Device
     * @return List of OMS-Add/Drop ports
     */
    public static List<Port> getOmsAddDropPorts(Device device) {
        RoadmPortQuery query = getRoadmPortQuery(device);
        if (query != null) {
            return query.getOmsAddDropPorts();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get whether the port is OMS-Add/Drop port.
     * @param device Device
     * @param port Port
     * @return true if port is OMS-Add/Drop port, false otherwise
     */
    public static boolean isOmsAddDropPort(Device device, PortNumber port) {
        RoadmPortQuery query = getRoadmPortQuery(device);
        if (query != null) {
            return query.isOmsAddDropPort(port);
        } else {
            return false;
        }
    }
}
