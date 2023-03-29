package org.onosproject.opticalpathoptimizer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.Filter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.net.openroadm.model.OsnrMap;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.openroadmprovider.util.ServiceModelUtils;
import org.onosproject.opticalpathoptimizer.api.QualityCalculationPropertyService;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20171215.orgopenroadmcommonservicetypes.ConnectionType;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20171215.orgopenroadmcommonservicetypes.ServiceLayerType;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20171215.orgopenroadmcommonservicetypes.connectiontype.ConnectionTypeEnum;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20171215.orgopenroadmcommonservicetypes.service.DefaultTopology;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20171215.orgopenroadmcommonservicetypes.service.Topology;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20171215.orgopenroadmcommonservicetypes.servicelayertype.ServiceLayerTypeEnum;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20171215.orgopenroadmcommontypes.LifecycleState;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20171215.orgopenroadmcommontypes.lifecyclestate.LifecycleStateEnum;
import org.onosproject.yang.gen.v1.orgopenroadmresource.rev20171215.orgopenroadmresource.resource.DefaultResource;
import org.onosproject.yang.gen.v1.orgopenroadmresource.rev20171215.orgopenroadmresource.resource.DefaultResourceType;
import org.onosproject.yang.gen.v1.orgopenroadmresource.rev20171215.orgopenroadmresource.resource.resource.resource.DefaultPhysicalLink;
import org.onosproject.yang.gen.v1.orgopenroadmresourcetypes.rev20171215.orgopenroadmresourcetypes.ResourceTypeEnum;
import org.onosproject.yang.gen.v1.orgopenroadmresourcetypes.rev20171215.orgopenroadmresourcetypes.resourcetypeenum.ResourceTypeEnumEnum;
import org.onosproject.yang.gen.v1.orgopenroadmservice.rev20171215.orgopenroadmservice.DefaultServiceList;
import org.onosproject.yang.gen.v1.orgopenroadmservice.rev20171215.orgopenroadmservice.servicelist.DefaultServices;
import org.onosproject.yang.gen.v1.orgopenroadmtopology.rev20171215.orgopenroadmtopology.hop.HopTypeEnum;
import org.onosproject.yang.gen.v1.orgopenroadmtopology.rev20171215.orgopenroadmtopology.topology.Atoz;
import org.onosproject.yang.gen.v1.orgopenroadmtopology.rev20171215.orgopenroadmtopology.topology.DefaultAtoz;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.RuntimeContext;
import org.onosproject.yang.runtime.YangRuntimeService;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.onosproject.openroadmprovider.util.NetworkModelUtils.NETWORK_ID_OPENROADM;
import static org.onosproject.openroadmprovider.util.NetworkModelUtils.NS_IETF_NETWORK;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Calculate OSNR values by executing an external program.
 */
public class ExternalOsnrCalculationInvoker {
    private final Logger log = getLogger(getClass());

    /*------------------------------------------------------------------------*
     * Constants
     *------------------------------------------------------------------------*/

    private static final String JSON_FORMAT = "JSON";

    private static final String OUTPUT_NETWORK_FILE = "network.json";
    private static final String OUTPUT_SERVICES_FILE = "services.json";
    private static final String OUTPUT_PARAMETERS_FILE = "parameters.json";

    /*------------------------------------------------------------------------*
     * ONOS Services
     *------------------------------------------------------------------------*/

    private final YangRuntimeService yangRuntime;

    private final DynamicConfigService dynamicConfigService;

    private final DeviceService deviceService;

    private final ModelConverter modelConverter;

    /*------------------------------------------------------------------------*
     * Misc.
     *------------------------------------------------------------------------*/

    private ExecutorService executor = Executors.newFixedThreadPool(2);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor.
     */
    public ExternalOsnrCalculationInvoker() {
        yangRuntime = DefaultServiceDirectory.getService(YangRuntimeService.class);
        dynamicConfigService = DefaultServiceDirectory.getService(DynamicConfigService.class);
        deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        modelConverter = DefaultServiceDirectory.getService(ModelConverter.class);
    }

    /**
     * Execute an external program and get the OSNR value.
     * @param paths Target to store the path and the retrieved OSNR value
     * @param workingDirectory Working directory and file output directory
     * @param command Command string
     * @param calculateProperties Parameters for calculation
     * @throws OsnrCalculationException Execution error of OSNR calculation program
     */
    public void invoke(
            List<PathOsnrMap> paths,
            String workingDirectory, String command,
            QualityCalculationPropertyService calculateProperties) throws OsnrCalculationException {
        // Create the output directory
        File dir = new File(workingDirectory);
        dir.mkdirs();

        // Output the network model
        Path networkModelFile = Paths.get(workingDirectory, OUTPUT_NETWORK_FILE);
        outputNetworkModel(networkModelFile);

        // Output the service model
        Map<String, PathOsnrMap> outputPaths = Maps.newLinkedHashMap();
        int count = 0;
        for (PathOsnrMap path : paths) {
            // Assign IDs for path mapping to external commands
            outputPaths.put("path-" + ++count, path);
        }

        Path serviceModelFile = Paths.get(workingDirectory, OUTPUT_SERVICES_FILE);
        outputServiceModel(serviceModelFile, outputPaths);

        // Parameter setting
        Path parameterFile = Paths.get(workingDirectory, OUTPUT_PARAMETERS_FILE);
        outputParameters(parameterFile, calculateProperties);

        String commandLine = command +
                " " + OUTPUT_NETWORK_FILE +
                " " + OUTPUT_SERVICES_FILE +
                " " + OUTPUT_PARAMETERS_FILE;

        Runtime runtime = Runtime.getRuntime();
        String output;

        try {
            log.info("Run command: {}", commandLine);
            Process process = runtime.exec(commandLine, null, dir);
            Future<String> futureOutput = executor.submit(
                    () -> IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8)
            );
            Future<String> futureError = executor.submit(
                    () -> IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8)
            );

            int ret = process.waitFor();

            output = futureOutput.get().trim();
            String error = futureError.get().trim();

            if (ret == 0) {
                // Success
                log.info("Command finished");
                if (!"".equals(output)) {
                    log.debug(output);
                }

                if (!"".equals(error)) {
                    log.warn("Standard Error: -----------------");
                    log.warn(error);
                    log.warn("---------------------------------");
                }

            } else  {
                log.warn("Command returned error. ret={}", ret);
                if (!"".equals(output)) {
                    log.warn("Standard Output: -----------------");
                    log.warn(output);
                    log.warn("---------------------------------");
                }
                if (!"".equals(error)) {
                    log.warn("Standard Error: -----------------");
                    log.warn(error);
                    log.warn("---------------------------------");
                }
                // Error occurred
                throw new OsnrCalculationException("Failed to execute OSNR calculation command. ret=" + ret);
            }

        } catch (IOException e) {
            throw new OsnrCalculationException(
                    "Failed to execute command: " + commandLine, e
            );
        } catch (InterruptedException e) {
            throw new OsnrCalculationException(
                    "Waiting for the process to complete was interrupted: " + commandLine, e
            );
        } catch (ExecutionException e) {
            throw new OsnrCalculationException(
                    "Failed to retrieve standard output/error streams", e
            );
        }

        // Read JSON
        try {
            //{
            //  "services": {
            //    "path-1": {
            //      "R100G/dp-qpsk": 1.0,
            //      "R150G/dp-qam8": 2.0,
            //      "R200G/dp-qam16": 3.0
            //    },
            //    "path-2": {
            //      "R100G/dp-qpsk": 4.0,
            //      "R150G/dp-qam8": 5.0,
            //      "R200G/dp-qam16": 6.0
            //    }
            //  }, ...
            //}
            JsonNode root = objectMapper.readTree(output);
            JsonNode services = root.get("services");
            if (!services.isObject()) {
                throw new OsnrCalculationException("Invalid OSNR data structure");
            }

            // Supported rates/modulation formats
            Set<String> rateModSet = calculateProperties.getRateModFormatPatternList().stream()
                    .map(p -> p.rate().name() + "/" + p.modulationFormat().name())
                    .collect(Collectors.toSet());

            // Reflect JSON content in path information
            Streams.stream(services.fieldNames())
                    .filter(outputPaths::containsKey)
                    .forEach(pathName -> {
                        PathOsnrMap path = outputPaths.get(pathName);
                        OsnrMap osnrMap = path.getOsnrMap();
                        JsonNode service = services.get(pathName);
                        Streams.stream(service.fieldNames())
                                .filter(rateModSet::contains) // Filter by supported rates/modulation formats
                                .forEach(e -> {
                                    String[] s = e.split("/", 2);
                                    osnrMap.put(
                                            OchParam.of(Rate.valueOf(s[0]), ModulationFormat.valueOf(s[1])),
                                            service.get(e).asDouble(Double.NaN)
                                    );
                                });
                    });

        } catch (IOException e) {
            throw new OsnrCalculationException("Invalid JSON data", e);
        }
    }

    /**
     * Output a network model JSON file.
     * @param outputFilePath Output file path
     * @throws OsnrCalculationException Output error
     */
    private void outputNetworkModel(Path outputFilePath) throws OsnrCalculationException {
        ResourceId rid = ResourceId.builder()
                .addBranchPointSchema("/", null)
                .addBranchPointSchema("network", NS_IETF_NETWORK)
                .addKeyLeaf("network-id", NS_IETF_NETWORK, NETWORK_ID_OPENROADM)
                .build();

        if (!dynamicConfigService.nodeExist(rid)) {
            throw new OsnrCalculationException("Not found network model in Dynamic Config");
        }

        DataNode node = dynamicConfigService.readNode(rid, Filter.builder().build());
        ObjectNode jsonNode = convertDataNodeToJson(rid, node);
        if (jsonNode == null) {
            throw new OsnrCalculationException("Failed to construct network model");
        }

        String json;

        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            log.debug(json);
        } catch (JsonProcessingException e) {
            throw new OsnrCalculationException("Failed to build service model JSON", e);
        }

        try (BufferedWriter writer = Files.newWriter(outputFilePath.toFile(), StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (IOException e) {
            throw new OsnrCalculationException("Failed to write network model JSON file", e);
        }
    }

    /**
     * Output a service model JSON file.
     * @param outputFilePath Output file path
     * @param outputPaths Output paths
     * @throws OsnrCalculationException Output error
     */
    private void outputServiceModel(
            Path outputFilePath, Map<String, PathOsnrMap> outputPaths) throws OsnrCalculationException {
        DefaultServiceList serviceList = new DefaultServiceList();

        // Generate the structure of a path as a single service
        for (Map.Entry<String, PathOsnrMap> pathEntry : outputPaths.entrySet()) {
            DefaultServices service = new DefaultServices();

            // service-name
            service.serviceName(pathEntry.getKey());

            // connection-type
            service.connectionType(ConnectionType.of(ConnectionTypeEnum.ROADM_LINE));

            // lifecycle-state
            service.lifecycleState(LifecycleState.of(LifecycleStateEnum.PLANNED));

            // service-layer
            service.serviceLayer(ServiceLayerType.of(ServiceLayerTypeEnum.WDM));

            Topology topology = new DefaultTopology();

            org.onosproject.net.Path path = pathEntry.getValue().getPath();
            // forward direction
            for (Link link : path.links()) {
                ConnectPoint src = link.src();
                ConnectPoint dst = link.dst();

                Atoz aToZ = new DefaultAtoz();

                // id
                aToZ.id(toLinkId(src, dst));
                // hop-type
                aToZ.hopType(HopTypeEnum.NODE_EXTERNAL);

                // resource -> physical-link-name
                DefaultResource resource = new DefaultResource();
                DefaultPhysicalLink physicalLink = new DefaultPhysicalLink();
                physicalLink.physicalLinkName(ServiceModelUtils.toLinkName(src, dst));
                resource.resource(physicalLink);
                aToZ.resource(resource);

                // resourceType -> physical-link
                DefaultResourceType type = new DefaultResourceType();
                type.type(ResourceTypeEnum.of(ResourceTypeEnumEnum.PHYSICAL_LINK));
                aToZ.resourceType(type);

                topology.addToAtoz(aToZ);
            }

//            // reverse direction
//            // * Output all to AtoZ
//            for (Link link : Lists.reverse(path.links())) {
//                ConnectPoint src = link.dst();  // reverse
//                ConnectPoint dst = link.src();
//
//                Ztoa zToA = new DefaultZtoa();
//
//                // id
//                zToA.id(toLinkId(src, dst));
//                // hop-type
//                zToA.hopType(HopTypeEnum.NODE_EXTERNAL);
//
//                // resource -> physical-link-name
//                DefaultResource resource = new DefaultResource();
//                DefaultPhysicalLink physicalLink = new DefaultPhysicalLink();
//                physicalLink.physicalLinkName(toLinkName(src, dst));
//                resource.resource(physicalLink);
//                zToA.resource(resource);
//
//                // resourceType -> physical-link
//                DefaultResourceType type = new DefaultResourceType();
//                type.type(ResourceTypeEnum.of(ResourceTypeEnumEnum.PHYSICAL_LINK));
//                zToA.resourceType(type);
//
//                topology.addToZtoa(zToA);
//            }

            service.topology(topology);

            serviceList.addToServices(service);
        }

        ModelObjectData data = DefaultModelObjectData.builder()
                .addModelObject(serviceList)
                .build();
        ResourceData resourceData = modelConverter.createDataNode(data);

        JsonNode jsonNode = convertDataNodeToJson(resourceData);
        if (jsonNode == null) {
            throw new OsnrCalculationException("Failed to construct service model");
        }

        String json;

        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            log.info(json);
        } catch (JsonProcessingException e) {
            throw new OsnrCalculationException("Failed to build service model JSON", e);
        }

        // Output file
        try (BufferedWriter writer = Files.newWriter(outputFilePath.toFile(), StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (IOException e) {
            throw new OsnrCalculationException("Failed to write service model JSON file", e);
        }
    }

    /**
     * Output a JSON file with parameters..
     * @param outputFilePath Output file path
     * @param properties Parameters to be output
     * @throws OsnrCalculationException Output error
     */
    private void outputParameters(Path outputFilePath,
                                  QualityCalculationPropertyService properties)
            throws OsnrCalculationException {
        Map<String, Object> object = Maps.newLinkedHashMap();

        //-------------------------------------------
        // Amplifier type and characteristic values
        //-------------------------------------------
        // Noise figure for each amplifier type. [dB]
        object.put("noiseFigures", properties.getNoiseFigureMap());

        //-------------------------------------------
        // Fiber type and characteristic values
        //-------------------------------------------
        // Effective area(cross section) for each fiber type. [um^2]
        object.put("Aeff", properties.getAeffMap());

        // Nonlinear refractive index for each fiber type. [m^2/W]
        object.put("N2", properties.getN2Map());

        // Wavelength dispersion for each fiber type. [ps/nm/km]
        object.put("CD", properties.getCdMap());

        // Input power for each fiber type (Pout). [dBm]
        object.put("Pout", properties.getPoutMap());

        // Pre-Amp's input power (Pout). [dBm]
        object.put("preAmpPout", properties.getPreAmpPout());

        //-------------------------------------------
        // Physical constants
        //-------------------------------------------
        // Planck constant. [Ws]
        object.put("planckConstant", properties.getPlanckConstant());

        // Speed of light. [m/s]
        object.put("SoL", properties.getSpeedOfLight());

        //-------------------------------------------
        // Quality calculation constants
        //-------------------------------------------
        // Noise bandwidth for P_ASE. [GHz]
        object.put("deltaF", properties.getDeltaF().asGHz());

        // Signal's frequency for calculation total OSNR. [THz]
        object.put("userFrequency", properties.getUserFrequency().asTHz());

        // Rate/Modulation-format pairs for OSNR/Q calculation.
        // "{rate}/{modulation-format}"
        object.put(
                "rateModFormatPattern",
                properties.getRateModFormatPatternList().stream()
                        .map(o -> o.rate().name() + "/" + o.modulationFormat().name())
                        .collect(Collectors.toList())
        );

        // Q-value calculate parameter for each vendor/rate/modulation-format.
        object.put("osnrQConstantsMap", properties.getOsnrQConstantsMap());

        // Lower thresholds of Q-value.
        object.put("qThresholdMap", properties.getQThresholdMap());

        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            log.debug("parameter: {}", json);
        } catch (JsonProcessingException e) {
            throw new OsnrCalculationException("Failed to build parameter JSON", e);
        }

        // Output file
        try (BufferedWriter writer = Files.newWriter(outputFilePath.toFile(), StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (IOException e) {
            throw new OsnrCalculationException("Failed to write parameter JSON file", e);
        }
    }

    // Generate node/port/link IDs --------------------------------------------------
    private String toNodeId(DeviceId deviceId) {
        return deviceId.toString();
    }

    private String toLinkId(ConnectPoint src, ConnectPoint dst) {
        return toLinkId(src.deviceId(), src.port(), dst.deviceId(), dst.port());
    }

    private String toLinkId(DeviceId aEndDevice, PortNumber aEndPort,
                            DeviceId bEndDevice, PortNumber bEndPort) {
        return toNodeId(aEndDevice) + "/" + aEndPort.toLong() + "-"
                + toNodeId(bEndDevice) + "/" + bEndPort.toLong();
    }

    // Copy from RestconfUtils. ------------------------------------------------------

    /**
     * Convert Resource Id and Data Node to Json ObjectNode.
     *
     * @param rid      resource identifier
     * @param dataNode represents type of node in data store
     * @return JSON representation of the data resource
     */
    private ObjectNode convertDataNodeToJson(ResourceId rid, DataNode dataNode) {
        RuntimeContext.Builder runtimeContextBuilder = DefaultRuntimeContext.builder();
        runtimeContextBuilder.setDataFormat(JSON_FORMAT);
        RuntimeContext context = runtimeContextBuilder.build();
        DefaultResourceData.Builder resourceDataBuilder = DefaultResourceData.builder();
        resourceDataBuilder.addDataNode(dataNode);
        resourceDataBuilder.resourceId(rid);
        ResourceData resourceData = resourceDataBuilder.build();
        DefaultCompositeData.Builder compositeDataBuilder = DefaultCompositeData.builder();
        compositeDataBuilder.resourceData(resourceData);
        CompositeData compositeData = compositeDataBuilder.build();
        ObjectNode rootNode = null;
        try {
            // CompositeData --- YangRuntimeService ---> CompositeStream.
            CompositeStream compositeStream = yangRuntime.encode(compositeData, context);
            InputStream inputStream = compositeStream.resourceData();
            rootNode = convertInputStreamToObjectNode(inputStream);
        } catch (Exception ex) {
            log.error("convertInputStreamToObjectNode failure: {}", ex.getMessage());
            log.debug("convertInputStreamToObjectNode failure", ex);
        }
        if (rootNode == null) {
            return null;
        }
        return rootNode;
    }

    /**
     * Convert Resource Id and Data Node to Json ObjectNode.
     *
     * @param resourceData resource data
     * @return JSON representation of the data resource
     */
    private ObjectNode convertDataNodeToJson(ResourceData resourceData) {
        RuntimeContext.Builder runtimeContextBuilder = DefaultRuntimeContext.builder();
        runtimeContextBuilder.setDataFormat(JSON_FORMAT);
        RuntimeContext context = runtimeContextBuilder.build();
        DefaultCompositeData.Builder compositeDataBuilder = DefaultCompositeData.builder();
        compositeDataBuilder.resourceData(resourceData);
        CompositeData compositeData = compositeDataBuilder.build();
        ObjectNode rootNode = null;
        try {
            // CompositeData --- YangRuntimeService ---> CompositeStream.
            CompositeStream compositeStream = yangRuntime.encode(compositeData, context);
            InputStream inputStream = compositeStream.resourceData();
            rootNode = convertInputStreamToObjectNode(inputStream);
        } catch (Exception ex) {
            log.error("convertInputStreamToObjectNode failure: {}", ex.getMessage());
            log.debug("convertInputStreamToObjectNode failure", ex);
        }
        if (rootNode == null) {
            return null;
        }
        return rootNode;
    }

    /**
     * Converts an input stream to JSON objectNode.
     *
     * @param inputStream the InputStream from Resource Data
     * @return JSON representation of the data resource
     */
    private ObjectNode convertInputStreamToObjectNode(InputStream inputStream) {
        ObjectNode rootNode;
        ObjectMapper mapper = new ObjectMapper();
        try {
            rootNode = (ObjectNode) mapper.readTree(inputStream);
        } catch (IOException e) {
            return null;
        }
        return rootNode;
    }
}
