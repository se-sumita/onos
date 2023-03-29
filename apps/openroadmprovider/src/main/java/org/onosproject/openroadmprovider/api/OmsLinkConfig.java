package org.onosproject.openroadmprovider.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.config.Config;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.openroadm.behaviors.OmsPortQuery;
import org.onosproject.net.openroadm.model.AmpType;
import org.onosproject.net.openroadm.model.FiberType;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 'oms-link' configuration for link configuration.
 */
public class OmsLinkConfig extends Config<LinkKey> {
    public static final String CONFIG_KEY = "oms-link";

    private static final String KEY_A_TO_B = "A-to-B";
    private static final String KEY_B_TO_A = "B-to-A";
    private static final String KEY_FIBER = "fiber";

    private static final String KEY_A_END_AMP = "A-end-amp";
    private static final String KEY_B_END_AMP = "B-end-amp";

    private static final String KEY_AMP_TYPE = "amp-type";
    private static final String KEY_AMP_GAIN = "gain";

    private static final String KEY_FIBER_TYPE = "fiber-type";
    private static final String KEY_FIBER_SPANLOSS = "spanloss";
    private static final String KEY_FIBER_LENGTH = "SRLG-length";

    private static final DeviceService DEVICE_SERVICE = DefaultServiceDirectory.getService(DeviceService.class);

    @Override
    public boolean isValid() {
        checkNotNull(subject);
        checkNotNull(subject.src());
        checkNotNull(subject.dst());

        // Getting device information
        Device aEndDevice = DEVICE_SERVICE.getDevice(subject.src().deviceId());
        Device bEndDevice = DEVICE_SERVICE.getDevice(subject.dst().deviceId());
        checkArgument(aEndDevice != null, "Device not found: " + subject.src().deviceId());
        checkArgument(bEndDevice != null, "Device not found: " + subject.dst().deviceId());

        // Getting port information
        Port aEndPort = DEVICE_SERVICE.getPort(subject.src());
        Port bEndPort = DEVICE_SERVICE.getPort(subject.dst());
        checkArgument(aEndPort != null, "Port not found: " + subject.src());
        checkArgument(bEndPort != null, "Port not found: " + subject.dst());

        String linkName = "LINK[" + toLinkName(aEndDevice, aEndPort, bEndDevice, bEndPort) + "]";

        if (!isRelevant(aEndDevice, aEndPort, bEndDevice, bEndPort)) {
            throw new IllegalArgumentException(
                    "Invalid key [oms-link]. This link is not inter-ROADM/AMP " + linkName);
        }

        // Check that all mandatory keys are present
        List<String> mandatoryKeys = ImmutableList.of(KEY_A_TO_B, KEY_B_TO_A, KEY_FIBER);
        mandatoryKeys.forEach(configKey -> {
            if (!object.has(configKey)) {
                throw new IllegalArgumentException("Missing key:" + configKey + ". " + linkName);
            }
            if (!object.get(configKey).isObject()) {
                throw new IllegalArgumentException(configKey + " is not object. " + linkName);
            }
        });

        // A-to-B ----------------------------------------------------------------
        ObjectNode path = (ObjectNode) object.get(KEY_A_TO_B);

        // A-to-B/A-end-amp
        ObjectNode aEndAmp = checkMandatoryObject(path, KEY_A_END_AMP, KEY_A_TO_B, linkName);
        checkAmp(aEndAmp, concatKeyName(KEY_A_TO_B, KEY_A_END_AMP), linkName);

        // A-to-B/B-end-amp
        if (bEndDevice.type() == Device.Type.OPTICAL_AMPLIFIER) {
            // not allowed
            if (path.has(KEY_B_END_AMP)) {
                throw new IllegalArgumentException(
                        "B-end is AMP. B-end-amp key is not allowed in "
                        + KEY_A_TO_B + ". " + linkName);
            }
        } else {
            ObjectNode bEndAmp = checkMandatoryObject(path, KEY_B_END_AMP, KEY_A_TO_B, linkName);
            checkAmp(bEndAmp, concatKeyName(KEY_A_TO_B, KEY_B_END_AMP), linkName);
        }

        // B-to-A ----------------------------------------------------------------
        path = (ObjectNode) object.get(KEY_B_TO_A);

        // B-to-A/B-end-amp
        ObjectNode bEndAmp = checkMandatoryObject(path, KEY_B_END_AMP, KEY_B_TO_A, linkName);
        checkAmp(bEndAmp, concatKeyName(KEY_B_TO_A, KEY_B_END_AMP), linkName);

        // B-to-A/A-end-amp
        if (aEndDevice.type() == Device.Type.OPTICAL_AMPLIFIER) {
            // not allowed
            if (path.has(KEY_A_END_AMP)) {
                throw new IllegalArgumentException(
                        "A-end is AMP. A-end-amp key is not allowed in "
                        + KEY_B_TO_A + ". " + linkName);
            }
        } else {
            aEndAmp = checkMandatoryObject(path, KEY_A_END_AMP, KEY_B_TO_A, linkName);
            checkAmp(aEndAmp, concatKeyName(KEY_B_TO_A, KEY_A_END_AMP), linkName);
        }

        // Fiber ----------------------------------------------------------------
        ObjectNode fiber = (ObjectNode) object.get(KEY_FIBER);
        checkFiber(fiber, KEY_FIBER, linkName, aEndDevice, aEndPort, bEndDevice, bEndPort);

        return true;
    }

    public AmpPair getAToBAmps() {
        return getAmpPair(KEY_A_TO_B);
    }

    public AmpPair getBToAAmps() {
        return getAmpPair(KEY_B_TO_A);
    }

    private AmpPair getAmpPair(String keyName) {
        ObjectNode aEndAmpObject = (ObjectNode) object.get(keyName).get(KEY_A_END_AMP);
        Amp aEndAmp = null;
        if (aEndAmpObject != null) {
            aEndAmp = new Amp(
                    AmpType.valueOf(aEndAmpObject.get(KEY_AMP_TYPE).asText()),
                    aEndAmpObject.get(KEY_AMP_GAIN).asDouble()
            );
        }

        ObjectNode bEndAmpObject = (ObjectNode) object.get(keyName).get(KEY_B_END_AMP);
        Amp bEndAmp = null;
        if (bEndAmpObject != null) {
            bEndAmp = new Amp(
                    AmpType.valueOf(bEndAmpObject.get(KEY_AMP_TYPE).asText()),
                    bEndAmpObject.get(KEY_AMP_GAIN).asDouble()
            );
        }
        return new AmpPair(aEndAmp, bEndAmp);
    }

    public Fiber getFiber() {
        return getFiber(false);
    }

    public Fiber getFiber(boolean reverse) {
        ObjectNode fiberObject = (ObjectNode) object.get(KEY_FIBER);

        Device aEndDevice;
        Device bEndDevice;
        Port aEndPort;
        Port bEndPort;

        if (!reverse) {
            aEndDevice = DEVICE_SERVICE.getDevice(subject.src().deviceId());
            bEndDevice = DEVICE_SERVICE.getDevice(subject.dst().deviceId());
            aEndPort = DEVICE_SERVICE.getPort(subject.src());
            bEndPort = DEVICE_SERVICE.getPort(subject.dst());
        } else { // opposite direction
            aEndDevice = DEVICE_SERVICE.getDevice(subject.dst().deviceId());
            bEndDevice = DEVICE_SERVICE.getDevice(subject.src().deviceId());
            aEndPort = DEVICE_SERVICE.getPort(subject.dst());
            bEndPort = DEVICE_SERVICE.getPort(subject.src());
        }

        FiberType fiberType;
        if (!fiberObject.has(KEY_FIBER_TYPE)) {
            // Obtain from devices
            fiberType = getFiberTypeFromDevice(aEndDevice, aEndPort);
            if (fiberType == null) {
                fiberType = getFiberTypeFromDevice(bEndDevice, bEndPort);
            }
        } else {
            fiberType = FiberType.valueOf(fiberObject.get(KEY_FIBER_TYPE).asText("smf"));
        }

        double spanloss;
        if (!fiberObject.has(KEY_FIBER_SPANLOSS)) {
            spanloss = getFiberSpanlossFromDevice(aEndDevice, aEndPort);
            if (Double.isNaN(spanloss)) {
                spanloss = getFiberSpanlossFromDevice(bEndDevice, bEndPort);
            }
        } else {
            spanloss = fiberObject.get(KEY_FIBER_SPANLOSS).asDouble(0);
        }

        int length = fiberObject.get(KEY_FIBER_LENGTH).asInt(0);

        return new Fiber(fiberType, spanloss, length);
    }

    private ObjectNode checkMandatoryObject(ObjectNode node, String mandatoryKey,
                                            String parentKeyName, String linkName) {
        if (!node.has(mandatoryKey)) {
            throw new IllegalArgumentException("Missing key: " +
                    concatKeyName(parentKeyName, mandatoryKey) + " " + linkName);
        }
        if (!node.get(mandatoryKey).isObject()) {
            throw new IllegalArgumentException(
                    concatKeyName(parentKeyName, mandatoryKey) +
                    " is not object. " + linkName);
        }
        return (ObjectNode) node.get(mandatoryKey);
    }

    private void checkAmp(ObjectNode ampObject, String parentKeyName, String linkName) {
        if (!ampObject.has(KEY_AMP_TYPE)) {
            throw new IllegalArgumentException("Missing key: " +
                    concatKeyName(parentKeyName, KEY_AMP_TYPE) + " " + linkName);
        }
        String ampTypeString = ampObject.get(KEY_AMP_TYPE).asText("");
        if (Strings.isNullOrEmpty(ampTypeString) || !AmpType.contains(ampTypeString)) {
            throw new IllegalArgumentException("Invalid value: " +
                    concatKeyName(parentKeyName, KEY_AMP_TYPE) + " " + linkName);
        }

        checkDouble(ampObject, KEY_AMP_GAIN, parentKeyName, linkName);
    }

    private void checkFiber(ObjectNode fiberObject, String parentKeyName, String linkName,
                            Device aEndDevice, Port aEndPort, Device bEndDevice, Port bEndPort) {

        if (!fiberObject.has(KEY_FIBER_TYPE)) {
            // Obtain from devices
            FiberType type = getFiberTypeFromDevice(aEndDevice, aEndPort);
            if (type != null) {
                type = getFiberTypeFromDevice(bEndDevice, bEndPort);
            }
            if (type == null) {
                throw new IllegalArgumentException("Missing key: " +
                        concatKeyName(parentKeyName, KEY_FIBER_TYPE) +
                        " and could not retrieve from device. " + linkName);
            }
        } else {
            String fiberType = fiberObject.get(KEY_FIBER_TYPE).asText("");
            if (Strings.isNullOrEmpty(fiberType) || !FiberType.types().contains(fiberType)) {
                throw new IllegalArgumentException("Invalid value: " +
                        concatKeyName(parentKeyName, KEY_FIBER_TYPE) + " " + linkName);
            }
        }

        if (!fiberObject.has(KEY_FIBER_SPANLOSS)) {
            double spanloss = getFiberSpanlossFromDevice(aEndDevice, aEndPort);
            if (Double.isNaN(spanloss)) {
                spanloss = getFiberSpanlossFromDevice(bEndDevice, bEndPort);
            }
            if (Double.isNaN(spanloss)) {
                throw new IllegalArgumentException("Missing key: " +
                        concatKeyName(parentKeyName, KEY_FIBER_SPANLOSS) +
                        " and could not retrieve from device. " + linkName);
            }
        } else {
            if (!fiberObject.get(KEY_FIBER_SPANLOSS).isNumber()) {
                throw new IllegalArgumentException("Invalid value: " +
                        concatKeyName(parentKeyName, KEY_FIBER_SPANLOSS) + " " + linkName);
            }
        }

        checkDouble(fiberObject, KEY_FIBER_LENGTH, parentKeyName, linkName);
    }

    private FiberType getFiberTypeFromDevice(Device device, Port port) {
        if (device.is(OmsPortQuery.class)) {
            OmsPortQuery query = device.as(OmsPortQuery.class);
            return query.queryFiberType(port);
        }
        return null;
    }

    private double getFiberSpanlossFromDevice(Device device, Port port) {
        if (device.is(OmsPortQuery.class)) {
            OmsPortQuery query = device.as(OmsPortQuery.class);
            return query.querySpanloss(port);
        }
        return Double.NaN;
    }

    private void checkDouble(ObjectNode obj, String keyName, String parentKeyName, String linkName) {
        if (!obj.has(keyName)) {
            throw new IllegalArgumentException("Missing key: " +
                    concatKeyName(parentKeyName, keyName) + " " + linkName);
        }
        if (!obj.get(keyName).isNumber()) {
            throw new IllegalArgumentException("Invalid value: " +
                    concatKeyName(parentKeyName, keyName) + " " + linkName);
        }
    }

    private String concatKeyName(String parent, String name) {
        if (Strings.isNullOrEmpty(parent)) {
            return name;
        }
        return parent + "/" + name;
    }

    private static boolean isOmsPort(Device device, Port port) {
        if (device.type() == Device.Type.ROADM || device.type() == Device.Type.ROADM_OTN) {
            return (port.type() == Port.Type.OMS);
        }
        if (device.type() == Device.Type.OPTICAL_AMPLIFIER) {
            return (port.type() == Port.Type.FIBER);
        }
        return false;
    }

    public static boolean isRelevant(Device aEndDevice, Port aEndPort, Device bEndDevice, Port bEndPort) {
        return isOmsPort(aEndDevice, aEndPort) && isOmsPort(bEndDevice, bEndPort);
    }

    /**
     * Make a link name.
     * @param aEndDevice a-end device
     * @param aEndPort a-end port
     * @param bEndDevice b-end device
     * @param bEndPort b-end port
     * @return "device1:port1=device2:port2"
     */
    public static String toLinkName(Device aEndDevice, Port aEndPort, Device bEndDevice, Port bEndPort) {
        return aEndDevice.id() + ":" + aEndPort.annotations().value(AnnotationKeys.PORT_NAME)
                + "="
                + bEndDevice.id() + ":" + bEndPort.annotations().value(AnnotationKeys.PORT_NAME);
    }
}
