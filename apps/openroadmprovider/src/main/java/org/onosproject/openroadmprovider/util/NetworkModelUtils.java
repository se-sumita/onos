package org.onosproject.openroadmprovider.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.openroadm.behaviors.OchPortQuery;
import org.onosproject.net.openroadm.behaviors.RoadmPortUtils;
import org.onosproject.net.openroadm.behaviors.TransponderCapabilityQuery;
import org.onosproject.net.openroadm.model.OchState;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.openroadmprovider.OpenRoadmProvider;
import org.onosproject.openroadmprovider.api.Amp;
import org.onosproject.openroadmprovider.api.AmpPair;
import org.onosproject.openroadmprovider.api.Fiber;
import org.onosproject.openroadmprovider.api.OmsLinkConfig;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openroadmprovider.util.NetworkModelIdUtils.toLinkId;
import static org.onosproject.openroadmprovider.util.NetworkModelIdUtils.toNodeId;

/**
 * Utility for registering OpenROADM network models.
 */
public final class NetworkModelUtils {
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmProvider.class);

    public static final String NS_IETF_NETWORK = "urn:ietf:params:xml:ns:yang:ietf-network";
    public static final String NS_IETF_NETWORK_TOPOLOGY = "urn:ietf:params:xml:ns:yang:ietf-network-topology";
    public static final String NS_OPENROADM_NETWORK = "urn:org:openroadm:network";
    public static final String NS_OPENROADM_NETWORK_TOPOLOGY = "urn:org:openroadm:network:topology";

    public static final String NETWORK_ID_OPENROADM = "openroadm";

    private static final ResourceService RESOURCE_SERVICE =
            DefaultServiceDirectory.getService(ResourceService.class);

    private static final DeviceService DEVICE_SERVICE =
            DefaultServiceDirectory.getService(DeviceService.class);

    private static final WavelengthPathStore WAVELENGTH_PATH_STORE =
            DefaultServiceDirectory.getService(WavelengthPathStore.class);

    private static final ResourceId NETWORK_ROOT_ID = ResourceId.builder()
            .addBranchPointSchema("/", null)
            .addBranchPointSchema("network", NS_IETF_NETWORK)
            .addKeyLeaf("network-id", NS_IETF_NETWORK, NETWORK_ID_OPENROADM)
            .build();

    private static final DynamicConfigUtils DC = new DynamicConfigUtils();

    private static final YangRuntimeUtils YANG = new YangRuntimeUtils();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private NetworkModelUtils() {}

    /**
     * Read the network tree.
     * @return network tree
     */
    public static DataNode readNetworkTree() {
        return DC.read(NETWORK_ROOT_ID);
    }

    /**
     * Clear the network tree.
     */
    public static void clearNetworkTree() {
        DC.deleteIfExist(NETWORK_ROOT_ID);
        // JSON --> DataNode
        InputStream jsonStream = NetworkModelUtils.class.getResourceAsStream("/network-root.json");
        ResourceData resourceData = YANG.toDataNode("/", jsonStream);
        DC.create(resourceData);
    }

    /**
     * Update the network node.
     * @param device device information
     */
    public static void updateNetworkNode(Device device) {
        checkNotNull(device);

        String nodeId = toNodeId(device);

        // The type value is a different field depending on the node type,
        // so it should be deleted and registered each time.
        DC.deleteIfExist(getNodeTypeResourceId(nodeId, NS_OPENROADM_NETWORK));
        DC.deleteIfExist(getNodeTypeResourceId(nodeId, NS_OPENROADM_NETWORK_TOPOLOGY));

        ObjectNode node = buildNetworkNode(device);
        ResourceData resourceData = YANG.toDataNode("ietf-network:network=openroadm", node);

        ResourceId id = getNodeResourceId(nodeId);
        LOG.info("[DC:Network] Update node [{}]", nodeId);
        DC.createOrUpdate(id, resourceData);
    }

    /**
     * Remove the network node.
     * @param device device information
     */
    public static void removeNetworkNode(Device device) {
        String nodeId = toNodeId(device);
        ResourceId id = getNodeResourceId(nodeId);
        LOG.info("[DC:Network] Delete node [{}]", nodeId);
        DC.deleteIfExist(id);
    }

    /**
     * Get the escaped port name.
     *
     * When submitting to DynamicConfig, an error occurs if the "/" symbol
     * is included.
     * @param port port
     * @return escaped port name
     */
    public static String getEscapedPortName(Port port) {
        String portName = port.annotations().value(AnnotationKeys.PORT_NAME);
        if (Strings.isNullOrEmpty(portName)) {
            portName = String.valueOf(port.number().toLong());
        }
        return portName.replace("/", "_");
    }

    /**
     * Update the endpoints.
     * @param device device information
     * @param port port information
     */
    public static void updateNetworkDevicePort(Device device, Port port) {
        checkNotNull(device);
        checkNotNull(port);

        String nodeId = toNodeId(device);
        String portName = getEscapedPortName(port);
        ResourceId id = getTerminationPortResourceId(nodeId, portName);

        BuiltTerminationPointObjects result = buildNetworkNodePort(device, port, id);

        // Basic port information
        ResourceData resourceData = YANG.toDataNode(
                "ietf-network:network=openroadm", result.root);
        boolean update = false;
        // Compare the types, replace if different, overwrite if the same.
        if (DC.exists(id)) {
            String type = readTpTypeFromDynamicConfig(nodeId, portName);
            update = Objects.equals(type, result.tpType);
        }
        if (update) {
            LOG.info("[DC:Network] Update port [{}/{}]", nodeId, portName);
            DC.update(resourceData);
        } else {
            LOG.info("[DC:Network] Add port [{}/{}]", nodeId, portName);
            DC.createOrReplace(id, resourceData);
        }

        // Update used wavelength information
        if (result.usedWavelength != null) {
            LOG.debug("[DC:Network] Update used wavelength of port [{}/{}]", nodeId, portName);
            result.usedWavelength.forEach((key, value) -> {
                DC.deleteIfExist(key);
                ResourceData data = YANG.toDataNode(getNodeTpUri(nodeId, portName), value);
                DC.createOrUpdate(key, data);
            });
        }
    }

    /**
     * Update the endpoints.
     * @param device device information
     * @param port port information
     */
    public static void removeNetworkDevicePort(Device device, Port port) {
        String nodeId = toNodeId(device);
        String portName = getEscapedPortName(port);
        ResourceId id = getTerminationPortResourceId(nodeId, portName);
        LOG.info("[DC:Network] Remove port [{}/{}]", nodeId, port.number());
        DC.deleteIfExist(id);
    }

    private static ObjectNode buildNetworkNode(Device device) {
        ObjectNode nodes = new ObjectMapper().createObjectNode();
        // {"ietf-network:node": [{"node-id": "NODE_ID", ...}]}
        ObjectNode node = nodes.putArray("ietf-network:node").addObject();

        String nodeName = device.annotations().value(AnnotationKeys.NAME);
        node.put("node-id", toNodeId(device))
            .put("org-openroadm-clli-network:clli", Strings.nullToEmpty(nodeName));

        // node-type
        String networkNodeTypeField = "org-openroadm-network:node-type";
        String topologyNodeTypeField = "org-openroadm-network-topology:node-type";
        switch (device.type()) {
            case ROADM:
            case ROADM_OTN:
                node.put(networkNodeTypeField, "ROADM");
                break;
            case SWITCH:
            case ROUTER:
                node.put(topologyNodeTypeField, "SWITCH");
                break;
            case OTN:
                node.put(topologyNodeTypeField, "XPONDER");
                node.put(networkNodeTypeField, "XPONDER");
                break;
            default:
                break;
        }

        // vendor
        if (!Strings.isNullOrEmpty(device.manufacturer())) {
            node.put("org-openroadm-network:vendor", device.manufacturer());
        }

        // model
        if (!Strings.isNullOrEmpty(device.hwVersion())) {
            node.put("org-openroadm-network:model", device.hwVersion());
        }

        return nodes;
    }

    private static BuiltTerminationPointObjects buildNetworkNodePort(Device device, Port port, ResourceId baseRid) {
        ObjectNode nodes = MAPPER.createObjectNode();
        ObjectNode node = nodes.putArray("ietf-network:node").addObject();
        node.put("node-id", toNodeId(device));

        // {"ietf-network:node": [{"node-id": "NODE_ID",
        //  "ietf-network-topology:termination-point": [{ "tp-id": "TPNAME", ... }]}]}

        // termination-point
        ObjectNode tp = node.putArray("ietf-network-topology:termination-point").addObject();

        boolean isTransponder = device.is(TransponderCapabilityQuery.class);

        String portName = getEscapedPortName(port);
        tp.put("tp-id", portName);

        // tp-type
        String tpTypeValue = null;
        BuiltTerminationPointObjects built = null;
        Map<String, ObjectNode> usedWavelengths;
        switch (port.type()) {
            case OMS: // OMS-line
                tpTypeValue = "DEGREE-TXRX-TTP";

                ObjectNode tpTxObject = MAPPER.createObjectNode();
                ObjectNode tpRxObject = MAPPER.createObjectNode();
                usedWavelengths = Maps.newHashMap();
                usedWavelengths.put("tx-ttp-attributes", tpTxObject);
                usedWavelengths.put("rx-ttp-attributes", tpRxObject);

                built = createTerminationPointObjects(nodes, tpTypeValue, baseRid, usedWavelengths);

                // tx-ttp-attributes
                DiscreteResource resource = Resources.discrete(device.id(), port.number()).resource();
                ArrayNode txWavelengths = tpTxObject
                        .putObject("org-openroadm-network-topology:tx-ttp-attributes")
                        .putArray("used-wavelengths");
                ArrayNode rxWavelengths = tpRxObject
                        .putObject("org-openroadm-network-topology:rx-ttp-attributes")
                        .putArray("used-wavelengths");
                Collection<ResourceAllocation> allocated =
                        RESOURCE_SERVICE.getResourceAllocations(resource.id(), OchSignal.class);
                List<OchSignal> signals = allocated.stream()
                        .map(a -> a.resource().valueAs(OchSignal.class).orElse(null))
                        .collect(Collectors.toList());
                signals = fromResourceGrid(signals, ChannelSpacing.CHL_50GHZ);
                if (signals.size() > 0) {
                    FrequencyConverter converter = new FrequencyConverter(DEVICE_SERVICE);
                    for (OchSignal signal : signals) {
                        WavelengthPath path = WAVELENGTH_PATH_STORE.findByOmsPortAndLambda(
                                new ConnectPoint(device.id(), port.number()), signal
                        );
                        if (path == null || path.isReserved()) {
                            continue; // do not include reserved paths
                        }
                        ObjectNode txWavelength = txWavelengths.addObject();
                        ObjectNode rxWavelength = rxWavelengths.addObject();
                        Stream.of(txWavelength, rxWavelength)
                                .forEach(wavelength -> wavelength.put("index", converter.channelId(signal))
                                .put("frequency", signal.centralFrequency().asTHz())
                                .put("width", signal.channelSpacing().frequency().asGHz()));
                    }
                }
                break;

            case OCH: // OCh (OMS-Add/Drop or OCh)
                // OMS Add/Drop
                if (isOmsAddDrop(device, port)) {
                    tpTypeValue = "SRG-TXRX-PP";

                    ObjectNode tpObject = MAPPER.createObjectNode();
                    usedWavelengths = Maps.newHashMap();
                    usedWavelengths.put("pp-attributes", tpObject);
                    built = createTerminationPointObjects(nodes, tpTypeValue, baseRid, usedWavelengths);

                    // pp-attributes
                    ArrayNode wavelengths = tpObject
                            .putObject("org-openroadm-network-topology:pp-attributes")
                            .putArray("used-wavelength");
                    if (device.is(OchPortQuery.class)) {
                        OchPortQuery query = device.as(OchPortQuery.class);
                        Optional<OchState> ochState = query.queryOchState(port);
                        if (ochState.isPresent()) {
                            OchState och = ochState.get();
                            FrequencyConverter converter = new FrequencyConverter(DEVICE_SERVICE);
                            OchSignal signal = och.lambda();
                            wavelengths.addObject()
                                    .put("index", converter.channelId(signal))
                                    .put("frequency", signal.centralFrequency().asTHz())
                                    .put("width", signal.channelSpacing().frequency().asGHz());
                        }
                    }

                } else { // OCh
                    tpTypeValue = "XPONDER-NETWORK";

                    ObjectNode tpObject = MAPPER.createObjectNode();
                    usedWavelengths = Maps.newHashMap();
                    usedWavelengths.put("xpdr-network-attributes", tpObject);
                    built = createTerminationPointObjects(nodes, tpTypeValue, baseRid, usedWavelengths);

                    // xpdr-network-attributes
                    ObjectNode attributes = tpObject.putObject(
                            "org-openroadm-network-topology:xpdr-network-attributes"
                    );
                    if (device.is(OchPortQuery.class)) {
                        OchPortQuery query = device.as(OchPortQuery.class);
                        Optional<OchState> ochState = query.queryOchState(port);
                        if (ochState.isPresent()) {
                            OchState och = ochState.get();
                            if (och.hasRatAndModulationFormat()) {
                                attributes
                                        .put("rate", "org-openroadm-common-types:" + och.rate().name())
                                        .put("modulation-format", och.modulationFormat().name());
                            }
                            OchSignal signal = och.lambda();
                            attributes.putObject("wavelength")
                                    .put("frequency", signal.centralFrequency().asTHz())
                                    .put("width", signal.channelSpacing().frequency().asGHz());
                        }
                    }
                }
                break;

            case FIBER: // Fiber
                if (device.type() == Device.Type.OPTICAL_AMPLIFIER) {
                    tpTypeValue = "DEGREE-TXRX-TTP";
                }
                break;

            default:
                if (isTransponder) { // OduClt
                    tpTypeValue = "XPONDER-CLIENT";
                }
        }
        if (tpTypeValue != null) {
            tp.put("org-openroadm-network-topology:tp-type", tpTypeValue);
        }

        if (built == null) {
            built = new BuiltTerminationPointObjects(nodes, tpTypeValue);
        }

        return built;
    }

    /**
     * Update network link.
     * @param aEndDevice a-end device
     * @param aEndPort a-end port
     * @param bEndDevice b-end device
     * @param bEndPort b-end port
     * @param aToBAmps a-to-b Amp
     * @param fiber Fiber
     */
    public static void updateNetworkLink(Device aEndDevice, Port aEndPort, Device bEndDevice, Port bEndPort,
                                         AmpPair aToBAmps, Fiber fiber) {
        checkNotNull(aEndDevice);
        checkNotNull(aEndPort);
        checkNotNull(bEndDevice);
        checkNotNull(bEndPort);

        ObjectNode link = buildNetworkLink(aEndDevice, aEndPort, bEndDevice, bEndPort, aToBAmps, fiber);

        ResourceData forwardLinkData = YANG.toDataNode(
                "ietf-network:network=openroadm", link);
        String linkId = toLinkId(aEndDevice, aEndPort, bEndDevice, bEndPort);
        ResourceId id = getLinkResourceId(linkId);

        LOG.info("[DC:Network] Update link [{}]", linkId);
        DC.createOrReplace(id, forwardLinkData);
    }

    /**
     * Delete network link.
     * @param aEnd a-end connect point
     * @param bEnd b-end connect point
     */
    public static void removeNetworkLink(ConnectPoint aEnd, ConnectPoint bEnd) {
        String linkId = toLinkId(aEnd.deviceId(), aEnd.port(), bEnd.deviceId(), bEnd.port());
        ResourceId id = getLinkResourceId(linkId);

        LOG.info("[DC:Network] Remove link [{}]", linkId);
        DC.deleteIfExist(id);
    }

    private static ObjectNode buildNetworkLink(Device aEndDevice, Port aEndPort, Device bEndDevice, Port bEndPort,
                                               AmpPair amp, Fiber fiber) {
        ObjectNode network = new ObjectMapper().createObjectNode();
        // "{ "ietf-network-topology:link": [{"link-id": "LINK_ID", ...}] }

        ObjectNode link = network.putArray("ietf-network-topology:link").addObject();
        String linkId = toLinkId(aEndDevice, aEndPort, bEndDevice, bEndPort);
        link.put("link-id", linkId);

        link.putObject("source")
                .put("source-node", toNodeId(aEndDevice))
                .put("source-tp", aEndPort.annotations().value(AnnotationKeys.PORT_NAME));

        link.putObject("destination")
                .put("dest-node", toNodeId(bEndDevice))
                .put("dest-tp", bEndPort.annotations().value(AnnotationKeys.PORT_NAME));

        boolean isInterRoadm = OmsLinkConfig.isRelevant(aEndDevice, aEndPort, bEndDevice, bEndPort);
        String linkTypeFieldName = "org-openroadm-network-topology:link-type";
        if (isInterRoadm) {
            link.put(linkTypeFieldName, "ROADM-TO-ROADM");
        } else if (isOmsAddDrop(aEndDevice, aEndPort) && isOchPort(bEndPort)) {
            link.put(linkTypeFieldName, "XPONDER-INPUT");
        } else if (isOchPort(aEndPort) && isOmsAddDrop(bEndDevice, bEndPort)) {
            link.put(linkTypeFieldName, "XPONDER-OUTPUT");
        //} else {
            // skip: not mandatory
        }

        if (!isInterRoadm) {
            return network;
        }

        if (amp == null || fiber == null) {
            LOG.warn("Not found Inter-ROADM/AMP fields. " + linkId);
            return network;
        }

        link.put("org-openroadm-network-topology:amplified", true);
        ObjectNode omsAttributes = link.putObject("org-openroadm-network-topology:OMS-attributes");
        omsAttributes.put("opposite-link", toLinkId(bEndDevice, bEndPort, aEndDevice, aEndPort));

        ArrayNode amplifiedLink = omsAttributes.putObject("amplified-link").putArray("amplified-link");

        int sectionElementNumber = 1;
        if (amp.hasAEndAmp()) { // A-end-amp is always present
            ObjectNode ila = amplifiedLink.addObject()
                    .put("section-elt-number", sectionElementNumber++)
                    .putObject("section-element")
                        .putObject("ila");
            putAmp(ila, amp.aEndAmp(), toNodeId(aEndDevice));
        }

        ObjectNode span = amplifiedLink.addObject()
                .put("section-elt-number", sectionElementNumber++)
                .putObject("section-element")
                    .putObject("span");
        putSpan(span, fiber);

        if (amp.hasBEndAmp()) {
            ObjectNode ila = amplifiedLink.addObject()
                    .put("section-elt-number", sectionElementNumber)
                    .putObject("section-element")
                    .putObject("ila");
            putAmp(ila, amp.bEndAmp(), toNodeId(bEndDevice));
        }

        return network;
    }

    // -------------------------------------------------------------------------

    private static boolean isOmsAddDrop(Device device, Port port) {
        return RoadmPortUtils.isOmsAddDropPort(device, port.number());
    }

    private static boolean isOchPort(Port port) {
        return port.type() == Port.Type.OCH;
    }

    private static void putAmp(ObjectNode object, Amp amp, String nodeId) {
        object.put("amp-type", amp.ampType().name())
                .put("gain", amp.gain())
                .put("node-id", nodeId);
    }

    private static void putSpan(ObjectNode object, Fiber fiber) {
        object.put("spanloss-base", fiber.spanloss())
               .putArray("link-concatenation")
               .addObject()
                   .put("SRLG-Id", 1)
                   .put("fiber-type", fiber.fiberType().name())
                   .put("SRLG-length", fiber.srlgLength());
    }

    private static ResourceId getNodeResourceId(String nodeId) {
        return YANG.concatResourceId(NETWORK_ROOT_ID,
                ResourceId.builder()
                        .addBranchPointSchema("node", NS_IETF_NETWORK)
                        .addKeyLeaf("node-id", NS_IETF_NETWORK, nodeId)
                        .build());
    }

    private static ResourceId getNodeTypeResourceId(String nodeId, String namespace) {
        return YANG.concatResourceId(NETWORK_ROOT_ID,
                ResourceId.builder()
                        .addBranchPointSchema("node", NS_IETF_NETWORK)
                        .addKeyLeaf("node-id", NS_IETF_NETWORK, nodeId)
                        .addBranchPointSchema("node-type", namespace)
                        .build());
    }

    private static ResourceId getTerminationPortResourceId(String nodeId, String portName) {
        return YANG.concatResourceId(getNodeResourceId(nodeId),
                ResourceId.builder()
                        .addBranchPointSchema("termination-point", NS_IETF_NETWORK_TOPOLOGY)
                        .addKeyLeaf("tp-id", NS_IETF_NETWORK_TOPOLOGY, portName)
                        .build());
    }

    private static ResourceId getTerminationPortTypeResourceId(String nodeId, String portName) {
        return YANG.concatResourceId(getNodeResourceId(nodeId),
                ResourceId.builder()
                        .addBranchPointSchema("termination-point", NS_IETF_NETWORK_TOPOLOGY)
                        .addKeyLeaf("tp-id", NS_IETF_NETWORK_TOPOLOGY, portName)
                        .addBranchPointSchema("tp-type", NS_OPENROADM_NETWORK_TOPOLOGY)
                        .build());
    }

    private static ResourceId getLinkResourceId(String linkId) {
        return YANG.concatResourceId(NETWORK_ROOT_ID,
                ResourceId.builder()
                        .addBranchPointSchema("link", NS_IETF_NETWORK_TOPOLOGY)
                        .addKeyLeaf("link-id", NS_IETF_NETWORK_TOPOLOGY, linkId)
                        .build());
    }

    /**
     * Converting flex grid to fixed grid for resource management.
     * @param from List of flex wavelengths
     * @param spacing frequency width
     * @return List of fixed wavelengths
     */
    public static List<OchSignal> fromResourceGrid(List<OchSignal> from, ChannelSpacing spacing) {
        int multiplier = (int) (spacing.frequency().asHz() / ChannelSpacing.CHL_12P5GHZ.frequency().asHz());
        List<OchSignal> results = Lists.newArrayList();
        for (int i = 0; i * multiplier + multiplier <= from.size(); i++) {
            List<OchSignal> sub = from.subList(i * multiplier, i * multiplier + multiplier);
            results.add(OchSignal.toFixedGrid(sub, spacing));
        }
        return results;
    }

    private static String getNodeTpUri(String nodeId, String portName) {
        nodeId = nodeId.replace(":", "%3A").replace("/", "%2F");
        portName = portName.replace(":", "%3A").replace("/", "%2F");
        return "ietf-network:network=openroadm/node="
                + nodeId + "/ietf-network-topology:termination-point="
                + portName + "/";
    }

    private static String readTpTypeFromDynamicConfig(String nodeId, String portName) {
        try {
            ResourceId typeId = getTerminationPortTypeResourceId(nodeId, portName);
            DataNode typeNode = DC.read(typeId);
            if (typeNode != null) {
                ObjectNode typeObject = YANG.toObjectNode(typeId, typeNode);
                return typeObject.fields().next().getValue().asText();
            }
        } catch (Exception ex) {
            LOG.warn("Retrieve failed.", ex);
        }
        return null;
    }

    private static BuiltTerminationPointObjects createTerminationPointObjects(
            ObjectNode root, String tpType, ResourceId baseRid, Map<String, ObjectNode> usedWavelengths) {
        Map<ResourceId, ObjectNode> used = Maps.newHashMap();
        for (Map.Entry<String, ObjectNode> entry : usedWavelengths.entrySet()) {
            used.put(
                    YANG.concatResourceId(baseRid, ResourceId.builder()
                            .addBranchPointSchema(entry.getKey(), NS_OPENROADM_NETWORK_TOPOLOGY)
                            .build()),
                    entry.getValue());
        }
        return new BuiltTerminationPointObjects(root, tpType, used);
    }

    private static class BuiltTerminationPointObjects {
        final ObjectNode root;
        final String tpType;
        final Map<ResourceId, ObjectNode> usedWavelength;

        BuiltTerminationPointObjects(ObjectNode root, String tpType) {
            this.root = root;
            this.tpType = tpType;
            this.usedWavelength = null;
        }

        BuiltTerminationPointObjects(ObjectNode root, String tpType, Map<ResourceId, ObjectNode> usedWavelength) {
            this.root = root;
            this.tpType = tpType;
            this.usedWavelength = usedWavelength;
        }
    }
}
