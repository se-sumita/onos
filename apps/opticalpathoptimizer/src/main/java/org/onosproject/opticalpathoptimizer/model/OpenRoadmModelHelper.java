package org.onosproject.opticalpathoptimizer.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.openroadm.model.AmpType;
import org.onosproject.net.openroadm.model.FiberType;
import org.onosproject.openroadmprovider.util.NetworkModelIdUtils;

/**
 * OpenROADM model access helper.
 */
public final class OpenRoadmModelHelper {

    private OpenRoadmModelHelper() {}

    /**
     * Get a string in link ID format.
     * @param src Source port
     * @param dst Destination port
     * @return Link ID
     */
    public static String linkId(ConnectPoint src, ConnectPoint dst) {
        return NetworkModelIdUtils.toLinkId(src, dst);
    }

    /**
     * Get link ID (device1:portName1=device2:portName2 format).
     * @param src Source port
     * @param dst Destination port
     * @return Link ID
     */
    public static String linkWithPortNameId(ConnectPoint src, ConnectPoint dst) {
        DeviceService service = DefaultServiceDirectory.getService(DeviceService.class);
        return src.deviceId() + ":" + getPortName(service, src)
                + "=" + dst.deviceId() + ":" + getPortName(service, dst);
    }

    /**
     * Get port name.
     * @param service Device service
     * @param point Connect point
     * @return Port name
     */
    private static String getPortName(DeviceService service, ConnectPoint point) {
        Port port = service.getPort(point);
        if (port == null) {
            return point.port().toString();
        }
        return port.number().hasName() ? port.number().name() : port.annotations().value(AnnotationKeys.PORT_NAME);
    }

    /**
     * Parsing JSON containing fiber information.
     * @param span JSON string
     * @return Fiber information
     */
    public static Fiber parseJsonToFiber(JsonNode span) {
        double spanLoss = span.findValue("spanloss-base").asDouble();
        double srlgLen = 0.0;
        FiberType fiberType = null;

        for (JsonNode s : span.get("link-concatenation")) {
            if (fiberType == null) {
                // Use the first definition that is correctly specified
                fiberType = FiberType.valueOf(s.findValue("fiber-type").asText("unknown"));
            }
            srlgLen += s.findValue("SRLG-length").asDouble(0.0);
        }
        if (fiberType == null) {
            fiberType = FiberType.SMF;
        }
        return new Fiber(spanLoss, fiberType, srlgLen);
    }

    /**
     * Parsing JSON containing amp(ila) information..
     * @param ila JSON string
     * @return Amplifier information
     */
    public static Amp parseJsonToAmp(JsonNode ila) {
        AmpType ampType = AmpType.valueOf(ila.findValue("amp-type").asText("standard"));
        double gain = ila.findValue("gain").asDouble(Double.NaN);

        return new Amp(gain, ampType);
    }

    /**
     * Create Pre-Amp model.
     * @return Pre-Amp model instance
     */
    public static Fiber createPreAmpFiber() {
        return new PreAmpFiber();
    }
}
