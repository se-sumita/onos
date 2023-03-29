package org.onosproject.openroadmprovider.util;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

/**
 * Utility for generating IDs for OpenROADM network models.
 */
public final class NetworkModelIdUtils {
    private NetworkModelIdUtils() {}

    /**
     * Get node ID from device ID.
     * @param deviceId device ID
     * @return node ID
     */
    public static String toNodeId(DeviceId deviceId) {
        return deviceId.toString();
    }

    /**
     * Get node ID from device.
     * @param device device
     * @return node ID
     */
    public static String toNodeId(Device device) {
        return toNodeId(device.id());
    }

    /**
     * Get link ID from both endpoints.
     * @param src source port
     * @param dst destination port
     * @return link ID
     */
    public static String toLinkId(ConnectPoint src, ConnectPoint dst) {
        return toLinkId(src.deviceId(), src.port(), dst.deviceId(), dst.port());
    }

    /**
     * Get link ID from both endpoints.
     * @param aEndDevice a-end device
     * @param aEndPort a-end port
     * @param bEndDevice b-end device
     * @param bEndPort b-end port
     * @return link ID
     */
    public static String toLinkId(Device aEndDevice, Port aEndPort, Device bEndDevice, Port bEndPort) {
        return toLinkId(aEndDevice.id(), aEndPort.number(), bEndDevice.id(), bEndPort.number());
    }

    /**
     * Get link ID from both endpoints.
     * @param aEndDevice a-end device
     * @param aEndPort a-end port
     * @param bEndDevice b-end device
     * @param bEndPort b-end port
     * @return link ID
     */
    public static String toLinkId(DeviceId aEndDevice, PortNumber aEndPort,
                                  DeviceId bEndDevice, PortNumber bEndPort) {
        // When putting into DynamicConfig, using the "/" symbol will cause
        // an error, use a different symbol.
        return toNodeId(aEndDevice) + "_" + aEndPort.toLong() + "-"
                + toNodeId(bEndDevice) + "_" + bEndPort.toLong();
    }
}
