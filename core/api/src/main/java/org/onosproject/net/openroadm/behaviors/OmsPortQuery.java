package org.onosproject.net.openroadm.behaviors;

import org.onosproject.net.Port;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.openroadm.model.FiberType;

/**
 * Behaviour to get parameters of the OMS-Line port.
 */
public interface OmsPortQuery extends HandlerBehaviour {
    /**
     * Get fiber type of the port.
     * @param port OMS-Line port
     * @return Fiber type
     */
    FiberType queryFiberType(Port port);

    /**
     * Get span loss of a port.
     * @param port OMS-Line port
     * @return Span loss
     */
    double querySpanloss(Port port);
}
