package org.onosproject.openroadmprovider.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.net.openroadm.model.WdmPath;
import org.onosproject.openroadmprovider.OpenRoadmProvider;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.openroadmprovider.OpenRoadmModel.*;

/**
 * Utility for registering OpenROADM service model.
 */
public final class ServiceModelUtils {
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmProvider.class);

    private static final DeviceService DEVICE_SERVICE =
            DefaultServiceDirectory.getService(DeviceService.class);

    private static final ResourceId SERVICE_LIST_ROOT_ID = ResourceId.builder()
            .addBranchPointSchema("/", null)
            .addBranchPointSchema("service-list", NS_OPENROADM_SERVICE)
            .build();

    private static final DynamicConfigUtils DC = new DynamicConfigUtils();

    private static final YangRuntimeUtils YANG = new YangRuntimeUtils();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ServiceModelUtils() {}

    /**
     * Clear the service list tree.
     */
    public static void clearServiceListTree() {
        DC.deleteIfExist(SERVICE_LIST_ROOT_ID);
        // JSON --> DataNode
        InputStream jsonStream = ServiceModelUtils.class.getResourceAsStream("/service-root.json");
        ResourceData resourceData = YANG.toDataNode("/", jsonStream);
        DC.create(resourceData);
    }

    /**
     * Replace WDM paths.
     * @param pathsAdded List of added WDM paths
     * @param pathsRemoved List of removed WDM paths
     */
    public static void replaceWdmPaths(Collection<WdmPath> pathsAdded, Collection<WdmPath> pathsRemoved) {
        // Clear the existing path
        LOG.info("[DC:Service] Remove WDM paths [{}]", pathsRemoved.size());
        pathsRemoved.stream()
                .map(WdmPath::getId)
                .map(ServiceModelUtils::toWdmServiceName)
                // .peek(id -> LOG.debug("Remove WDM path \"{}\"", id))
                .map(ServiceModelUtils::getPathResourceId)
                .forEach(DC::deleteIfExist);

        LOG.info("[DC:Service] Finished to remove WDM paths [{}]", pathsRemoved.size());

        if (pathsAdded.size() > 0) {
            LOG.info("[DC:Service] Start to add WDM paths [{}]", pathsAdded.size());

            List<WdmPath> all = Lists.newArrayList(pathsAdded);
            int partitionSize = 100;
            int count = 0;
            List<List<WdmPath>> partitioned = Lists.partition(all, partitionSize);
            for (List<WdmPath> paths : partitioned) {
                ObjectNode serviceList = MAPPER.createObjectNode();
                ArrayNode services = serviceList.putObject("org-openroadm-service:service-list")
                        .putArray("services");

                LOG.debug("[DC:Service] Build WDM paths [{}]", paths.size());
                Map<Path, ArrayNode> pathCache = Maps.newHashMap();
                paths.stream()
                        .map(p -> buildWdmPath(p, toWdmServiceName(p.getId()), pathCache))
                        .forEach(services::add);

                int index = count * partitionSize;
                LOG.info("[DC:Service] Add WDM paths [{}-{}/{}]",
                        index + 1, index + paths.size(),
                        all.size());

                ResourceData resourceData = YANG.toDataNode("/", serviceList);
                DC.update(resourceData);
                count++;
            }
            LOG.info("[DC:Service] Finished to add WDM paths [{}]", all.size());
        }
    }

    private static ObjectNode buildWdmPath(WdmPath wdmPath, String serviceName, Map<Path, ArrayNode> pathCache) {
        ObjectNode service = MAPPER.createObjectNode();
        // { "org-openroadm-service:services": [{"org-openroadm-service:service-name": "SERVICE_NAME", ...}]}

        service
            .put("service-name", serviceName)
            .put("connection-type", "wdm-path")
            .put("lifecycle-state", "planned")
            .put("service-layer", "wdm");

        ArrayNode links = service.putObject("topology").putArray("aToZ");

        // OMS-Add -> OMS-Line
        putInternalLink(links.addObject(), wdmPath.src(), wdmPath.path().src());

        ArrayNode pathLinks;
        if (pathCache.containsKey(wdmPath.path())) {
            pathLinks = pathCache.get(wdmPath.path());
            // LOG.debug("Use link cache: {}", serviceName);
        } else {
            pathLinks = MAPPER.createArrayNode();
            ConnectPoint lastDstPort = null;
            for (Link link : wdmPath.path().links()) {
                if (lastDstPort != null) {
                    putInternalLink(pathLinks.addObject(), lastDstPort, link.src());
                }
                putExternalLink(pathLinks.addObject(), link.src(), link.dst());
                lastDstPort = link.dst();
            }
            pathCache.put(wdmPath.path(), pathLinks);
        }

        links.addAll(pathLinks);

        // OMS-Line -> OMS-Drop
        putInternalLink(links.addObject(), wdmPath.path().dst(), wdmPath.dst());

        return service;
    }

    /**
     * Update the wavelength path.
     * @param wavelengthPath wavelength path
     * @param coupledServices coupled service IDs
     */
    public static void updateWavelengthPath(WavelengthPath wavelengthPath, Set<Long> coupledServices) {
        LOG.info("[DC:Service] Add/Update wavelength path [{}]", wavelengthPath.id());

        String serviceName = toWavelengthServiceName(wavelengthPath.id());
        ObjectNode serviceList = buildWavelengthPath(wavelengthPath, serviceName, coupledServices);

        ResourceId id = getPathResourceId(serviceName);
        ResourceData resourceData = YANG.toDataNode("org-openroadm-service:service-list", serviceList);
        DC.createOrUpdate(id, resourceData);
    }

    private static ObjectNode buildWavelengthPath(WavelengthPath wavelengthPath, String serviceName,
                                                  Set<Long> coupledServices) {
        ObjectNode serviceList = MAPPER.createObjectNode();
        ArrayNode services = serviceList.putArray("org-openroadm-service:services");
        ObjectNode service = services.addObject();

        service
                .put("service-name", serviceName)
                .put("connection-type", "wavelength-path")
                .put("lifecycle-state",
                        wavelengthPath.isSubmitted() ? "deployed" : "planned")
                .put("service-layer", "wavelength");

        ObjectNode pathInfo = service.putObject("wavelength-path-info");

        ArrayNode coupled = pathInfo.putArray("coupled-services");
        for (Long serviceId : coupledServices) {
            if (serviceId == wavelengthPath.id()) {
                continue;
            }
            coupled.add(toWavelengthServiceName(serviceId));
        }

        pathInfo.put("q-value", wavelengthPath.qValue())
                .put("q-lower-limit", wavelengthPath.qThreshold())
                .put("name", wavelengthPath.name());

        // topology/aToZ
        ObjectNode topology = service.putObject("topology");
        ArrayNode links = topology.putArray("aToZ");

        ConnectPoint lastDstPort = null;
        for (Link link : wavelengthPath.links()) {
            if (lastDstPort != null) {
                putInternalLink(links.addObject(), lastDstPort, link.src());
            }
            putExternalLink(links.addObject(), link.src(), link.dst());
            lastDstPort = link.dst();
        }

        // topology/zToA
        links = topology.putArray("zToA");
        lastDstPort = null;
        for (Link link : Lists.reverse(wavelengthPath.links())) {
            if (lastDstPort != null) {
                putInternalLink(links.addObject(), lastDstPort, link.dst());
            }
            putExternalLink(links.addObject(), link.dst(), link.src());
            lastDstPort = link.src();
        }

        return serviceList;
    }

    /**
     * Remove the wavelength path.
     * @param wavelengthPath wavelength path
     */
    public static void removeWavelengthPath(WavelengthPath wavelengthPath) {
        LOG.info("[DC:Service] Remove wavelength path [{}]", wavelengthPath.id());

        String serviceName = toWavelengthServiceName(wavelengthPath.id());
        ResourceId id = getPathResourceId(serviceName);
        DC.deleteIfExist(id);
    }

    private static void putInternalLink(ObjectNode link, ConnectPoint inPort, ConnectPoint outPort) {
        link.put("id", NetworkModelIdUtils.toLinkId(inPort, outPort))
                .put("hop-type", "node-internal")
                .putObject("device")
                    .put("node-id", NetworkModelIdUtils.toNodeId(inPort.deviceId()));
        link.putObject("resource")
                .put("internal-link-name", toLinkName(inPort, outPort));
        link.putObject("resourceType")
                .put("type", "internal-link");
    }

    private static void putExternalLink(ObjectNode link, ConnectPoint inPort, ConnectPoint outPort) {
        link.put("id", NetworkModelIdUtils.toLinkId(inPort, outPort))
                .put("hop-type", "node-external");
        link.putObject("resource")
                .put("physical-link-name", toLinkName(inPort, outPort));
        link.putObject("resourceType")
                .put("type", "physical-link");
    }

    private static ResourceId getPathResourceId(String serviceName) {
        return YANG.concatResourceId(SERVICE_LIST_ROOT_ID,
                ResourceId.builder()
                        .addBranchPointSchema("services", NS_OPENROADM_SERVICE)
                        .addKeyLeaf("service-name", NS_OPENROADM_SERVICE, serviceName)
                        .build());
    }

    private static String toWdmServiceName(String id) {
        return WDM_PATH_PREFIX + id.replaceAll("[/:]", "_");
    }

    private static String toWavelengthServiceName(long id) {
        return WAVELENGTH_PATH_PREFIX + id;
    }

    public static String toLinkName(ConnectPoint src, ConnectPoint dst) {
        String srcDevice = toDeviceName(src.deviceId());
        String dstDevice = src.deviceId().equals(dst.deviceId()) ? srcDevice : toDeviceName(dst.deviceId());

        String srcPort = toPortName(src);
        String dstPort = toPortName(dst);

        return srcDevice + "|" + srcPort + "=>" + dstDevice + "|" + dstPort;
    }

    private static String toDeviceName(DeviceId deviceId) {
        Device device = DEVICE_SERVICE.getDevice(deviceId);
        if (device == null) {
            return deviceId.toString();
        }

        String name = device.annotations().value(AnnotationKeys.NAME);
        if (Strings.isNullOrEmpty(name)) {
            return deviceId.toString();
        }

        return name;
    }

    private static String toPortName(ConnectPoint port) {
        Port p = DEVICE_SERVICE.getPort(port);
        if (p == null) {
            return port.port().hasName() ? port.port().name() : port.port().toString();
        }
        return p.annotations().value(AnnotationKeys.PORT_NAME);
    }
}
