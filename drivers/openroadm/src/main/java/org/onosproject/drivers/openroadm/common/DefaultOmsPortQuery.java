package org.onosproject.drivers.openroadm.common;

import com.google.common.base.Strings;
import org.onosproject.net.Port;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.openroadm.behaviors.OmsPortQuery;
import org.onosproject.net.openroadm.model.FiberType;

/**
 * Default implementation of {@link OmsPortQuery}.
 */
public class DefaultOmsPortQuery extends AbstractHandlerBehaviour implements OmsPortQuery {

    @Override
    public FiberType queryFiberType(Port port) {
        if (port.type() == Port.Type.OMS || port.type() == Port.Type.FIBER) {
            String fiberType = port.annotations().value(Annotation.KEY_FIBER_TYPE);
            if (!Strings.isNullOrEmpty(fiberType) && FiberType.types().contains(fiberType)) {
                return FiberType.valueOf(fiberType);
            }
        }
        return null;
    }

    @Override
    public double querySpanloss(Port port) {
        if (port.type() == Port.Type.OMS || port.type() == Port.Type.FIBER) {
            String spanloss = port.annotations().value(Annotation.KEY_SPANLOSS);
            if (!Strings.isNullOrEmpty(spanloss)) {
                try {
                    return Double.parseDouble(spanloss);
                } catch (NumberFormatException ex) {
                    // through
                }
            }
        }
        return Double.NaN;
    }
}

