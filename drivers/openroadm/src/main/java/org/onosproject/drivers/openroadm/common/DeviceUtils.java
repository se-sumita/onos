package org.onosproject.drivers.openroadm.common;

import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.osgi.DefaultServiceDirectory.getService;

/**
 * Utility for retrieving information from the device service.
 */
public final class DeviceUtils {
    /**
     * Get the port list of the device with the specified device ID.
     * @param deviceId device ID
     * @return list of ports
     */
    public static List<PortDescription> getPortsByDeviceId(DeviceId deviceId) {
        DeviceService deviceService = checkNotNull(getService(DeviceService.class));
        List<Port> ports = deviceService.getPorts(deviceId);
        return ports.stream()
            .map(x -> DefaultPortDescription.builder()
                 .withPortNumber(x.number())
                 .isEnabled(x.isEnabled())
                 .type(x.type())
                 .portSpeed(x.portSpeed())
                 .annotations(DefaultAnnotations.builder().putAll(x.annotations()).build())
                 .build())
            .collect(Collectors.toList());
    }

    private DeviceUtils() {}
}
