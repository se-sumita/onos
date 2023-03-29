package org.onosproject.opticalpathoptimizer.cli;

import com.google.common.base.Strings;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.opticalpathoptimizer.OpticalPathOptimizer;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.cli.AbstractShellCommand.get;

final class CliUtils {
    private CliUtils() {}

    /**
     * Parse a device connect point from a string.
     * The connect point should be in the format "deviceUri/portName".
     *
     * @param string string to parse
     * @return a ConnectPoint based on the information in the string.
     */
    public static ConnectPoint readConnectPointWithName(String string) {
        DeviceService deviceService = get(DeviceService.class);
        checkNotNull(string);
        String[] splitted = string.split("/", 2);
        DeviceId deviceId = DeviceId.deviceId(splitted[0]);

        if (splitted.length < 2) {
            return null;
        }

        try {
            return new ConnectPoint(deviceId, PortNumber.fromString(splitted[1]));
        } catch (IllegalArgumentException ex) {
            // through
        }

        List<Port> ports = deviceService.getPorts(deviceId);
        PortNumber portNumber = null;
        for (Port port : ports) {
            String str = port.annotations().value(AnnotationKeys.PORT_NAME);
            if (!Strings.isNullOrEmpty(str) && str.equals(splitted[1])) {
                portNumber = port.number();
                break;
            }
        }
        if (portNumber == null) {
            return null;
        }
        return new ConnectPoint(deviceId, portNumber);
    }

    public static ConnectPoint reloadConnectPort(DeviceService deviceService, ConnectPoint point) {
        Port p = deviceService.getPort(point);
        if (p == null) {
            return point;
        }
        return new ConnectPoint(point.deviceId(), p.number());
    }

    public static ConnectPoint reloadConnectPort(ConnectPoint point) {
        return reloadConnectPort(get(DeviceService.class), point);
    }

    public static ConnectPoint checkConnectPointWithName(String pointStr) {
        ConnectPoint point = readConnectPointWithName(pointStr);
        if (point == null) {
            throw new IllegalArgumentException(
                    "Connect point must be in \"deviceUri/{portName|id}\" format.("
                            + pointStr + ")\nor specified port does not exist.");
        }
        return point;
    }

    public static ConnectPoint checkOmsAddDropPort(String pointStr) {
        ConnectPoint point = checkConnectPointWithName(pointStr);
        DeviceService deviceService = get(DeviceService.class);
        OpticalPathOptimizer.checkOmsAddDropPort(deviceService, point,
                "Connect point must be OMS Add/Drop port. (" + pointStr + ")");
        return point;
    }

    public static ConnectPoint checkOchPoint(String pointStr) {
        ConnectPoint point = checkConnectPointWithName(pointStr);
        DeviceService deviceService = get(DeviceService.class);
        OpticalPathOptimizer.checkOchPort(deviceService, point,
                "Connect point must be OCh port. (" + pointStr + ")");
        return point;
    }
}
