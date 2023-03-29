package org.onosproject.net.openroadm.behaviors;

import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.List;

/**
 * Behaviour to get the port information of ROADM.
 */
public interface RoadmPortQuery extends HandlerBehaviour {
    /**
     * Get list of OMS-Add/Drop ports.
     * @return List of OMS-Add/Drop ports
     */
    List<Port> getOmsAddDropPorts();

    /**
     * Get whether the port is OMS-Add/Drop port.
     * @param portNumber Port
     * @return true if port is OMS-Add/Drop port, false otherwise
     */
    boolean isOmsAddDropPort(PortNumber portNumber);
}