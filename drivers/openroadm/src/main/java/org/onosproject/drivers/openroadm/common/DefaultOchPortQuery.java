package org.onosproject.drivers.openroadm.common;

import com.google.common.base.Strings;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.openroadm.behaviors.OchPortQuery;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.OchState;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OpticalDevice;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link OchPortQuery}.
 */
public class DefaultOchPortQuery extends AbstractHandlerBehaviour implements OchPortQuery {
    @Override
    public Optional<OchState> queryOchState(Port port) {
        checkNotNull(port);
        if (port.type() != Port.Type.OCH) {
            return Optional.empty();
        }

        DeviceService service = handler().get(DeviceService.class);
        Device device = service.getDevice(data().deviceId());
        if (!device.is(OpticalDevice.class)) {
            return Optional.empty();
        }
        port = service.getPort(data().deviceId(), port.number());
        if (port == null) {
            return Optional.empty();
        }

        OpticalDevice opticalDevice = device.as(OpticalDevice.class);
        if (!opticalDevice.portIs(port, OchPort.class)) {
            return Optional.empty();
        }

        OchPort ochPort = opticalDevice.portAs(port, OchPort.class).orElse(null);
        if (ochPort == null) {
            return Optional.empty();
        }

        // Not configured
        if (ochPort.lambda().centralFrequency().asHz() == 0) {
            return Optional.empty();
        }

        String rate = ochPort.annotations().value(Annotation.KEY_RATE);
        String modulationFormat = ochPort.annotations().value(Annotation.KEY_MODULATION_FORMAT);
        if (Strings.isNullOrEmpty(rate) || Strings.isNullOrEmpty(modulationFormat)) {
            return Optional.of(new OchState(ochPort.lambda()));
        } else {
            return Optional.of(new OchState(
                    ochPort.lambda(),
                    Rate.valueOf(rate),
                    ModulationFormat.valueOf(modulationFormat)));
        }
    }
}
