package org.onosproject.net.openroadm.behaviors;

import org.onosproject.net.Port;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.openroadm.model.OchState;

import java.util.Optional;

/**
 * Behaviour to get parameters of the OCh port.
 */
public interface OchPortQuery extends HandlerBehaviour {
    /**
     * Get parameters of the OCh port.
     * @param port OCh port
     * @return Parameters of the OCh port
     */
    Optional<OchState> queryOchState(Port port);
}
