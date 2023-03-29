package org.onosproject.opticalpathoptimizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.openroadm.model.LinkType;
import org.onosproject.opticalpathoptimizer.model.Element;
import org.onosproject.opticalpathoptimizer.model.OpenRoadmModelException;
import org.onosproject.opticalpathoptimizer.model.OpenRoadmModelHelper;
import org.onosproject.opticalpathoptimizer.model.OpenRoadmModelLink;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.LeafNode;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.RuntimeContext;
import org.onosproject.yang.runtime.YangRuntimeService;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

import static org.onosproject.cli.AbstractShellCommand.get;
import static org.onosproject.openroadmprovider.util.NetworkModelUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Helper to use for DynamicConfig storing OpenROADM.
 */
public class OpenRoadmConfigHelper {

    private final Logger log = getLogger(getClass());

    /**
     * Data format required by YangRuntime Service.
     */
    private static final String JSON_FORMAT = "JSON";

    private YangRuntimeService yangRuntime;

    private DynamicConfigService dynamicConfigService;

    /**
     * Create an instance of this helper.
     * @return the instance
     */
    public static OpenRoadmConfigHelper create() {
        YangRuntimeService yangRuntime = get(YangRuntimeService.class);
        DynamicConfigService configService = get(DynamicConfigService.class);
        return new OpenRoadmConfigHelper(yangRuntime, configService);
    }

    protected OpenRoadmConfigHelper(YangRuntimeService yangRuntime, DynamicConfigService dynamicConfig) {
        this.yangRuntime = yangRuntime;
        this.dynamicConfigService = dynamicConfig;
    }

    /**
     * Generate ResourceID for "vendor".
     * @param deviceId device ID
     * @return resource ID
     */
    private ResourceId buildVendorResourceId(String deviceId) {
        return ResourceId.builder()
                .addBranchPointSchema("/", null)
                .addBranchPointSchema("network", NS_IETF_NETWORK)
                .addKeyLeaf("network-id", NS_IETF_NETWORK, NETWORK_ID_OPENROADM)
                .addBranchPointSchema("node", NS_IETF_NETWORK)
                .addKeyLeaf("node-id", NS_IETF_NETWORK, deviceId)
                .addBranchPointSchema("vendor", NS_OPENROADM_NETWORK)
                .build();
    }

    /**
     * Generate ResourceID for "amplified".
     * @param linkId link ID
     * @return resource ID
     */
    private static ResourceId buildAmplifiedResourceId(String linkId) {
        return ResourceId.builder()
                .addBranchPointSchema("/", null)
                .addBranchPointSchema("network", NS_IETF_NETWORK)
                .addKeyLeaf("network-id", NS_IETF_NETWORK, NETWORK_ID_OPENROADM)
                .addBranchPointSchema("link", NS_IETF_NETWORK_TOPOLOGY)
                .addKeyLeaf("link-id", NS_IETF_NETWORK_TOPOLOGY, linkId)
                .addBranchPointSchema("amplified", NS_OPENROADM_NETWORK_TOPOLOGY)
                .build();
    }

    /**
     * Generate ResourceID for "link-type".
     * @param linkId link ID
     * @return resource ID
     */
    private static ResourceId buildLinkTypeResourceId(String linkId) {
        return ResourceId.builder()
                .addBranchPointSchema("/", null)
                .addBranchPointSchema("network", NS_IETF_NETWORK)
                .addKeyLeaf("network-id", NS_IETF_NETWORK, NETWORK_ID_OPENROADM)
                .addBranchPointSchema("link", NS_IETF_NETWORK_TOPOLOGY)
                .addKeyLeaf("link-id", NS_IETF_NETWORK_TOPOLOGY, linkId)
                .addBranchPointSchema("link-type", NS_OPENROADM_NETWORK_TOPOLOGY)
                .build();
    }

    /**
     * Generate ResourceID for Amplified-link's "span".
     * @param linkId link ID
     * @return resource ID
     */
    private static ResourceId buildSpanResourceId(String linkId) {
        return ResourceId.builder()
                .addBranchPointSchema("/", null)
                .addBranchPointSchema("network", NS_IETF_NETWORK)
                .addKeyLeaf("network-id", NS_IETF_NETWORK, NETWORK_ID_OPENROADM)
                .addBranchPointSchema("link", NS_IETF_NETWORK_TOPOLOGY)
                .addKeyLeaf("link-id", NS_IETF_NETWORK_TOPOLOGY, linkId)
                .addBranchPointSchema("OMS-attributes", NS_OPENROADM_NETWORK_TOPOLOGY)
                .addBranchPointSchema("span", NS_OPENROADM_NETWORK_TOPOLOGY)
                .build();
    }

    /**
     * Generate ResourceID for "amplified-link".
     * @param linkId link ID
     * @return resource ID
     */
    private static ResourceId buildAmplifiedLinkResourceId(String linkId) {
        return ResourceId.builder()
                .addBranchPointSchema("/", null)
                .addBranchPointSchema("network", NS_IETF_NETWORK)
                .addKeyLeaf("network-id", NS_IETF_NETWORK, NETWORK_ID_OPENROADM)
                .addBranchPointSchema("link", NS_IETF_NETWORK_TOPOLOGY)
                .addKeyLeaf("link-id", NS_IETF_NETWORK_TOPOLOGY, linkId)
                .addBranchPointSchema("OMS-attributes", NS_OPENROADM_NETWORK_TOPOLOGY)
                .addBranchPointSchema("amplified-link", NS_OPENROADM_NETWORK_TOPOLOGY)
                .build();
    }

    /**
     * Get the data node.
     * @param rid resource ID
     * @return data node
     */
    private DataNode getDataNode(ResourceId rid) throws OpenRoadmModelException {
        DataNode dataNode;
        Filter filter = Filter.builder().build();
        try {
            if (!dynamicConfigService.nodeExist(rid)) {
                throw new OpenRoadmModelException("resourceID not exist: " + rid);
            }
            dataNode = dynamicConfigService.readNode(rid, filter);
        } catch (FailedException e) {
            throw new OpenRoadmModelException("Failed to access by dynamic config service: " + rid, e);
        }
        return dataNode;
    }

    /**
     * Get string value.
     * @param rid resource ID
     * @return value (as String)
     */
    private String getStringValue(ResourceId rid) throws OpenRoadmModelException {
        DataNode dataNode = getDataNode(rid);

        if (dataNode.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
            return ((LeafNode) dataNode).asString();
        } else {
            throw new OpenRoadmModelException("DataNode type mismatched. " + rid);
        }
    }

    /**
     * Get boolean value.
     * @param rid resource ID
     * @return value (as Boolean)
     */
    private Boolean getBooleanValue(ResourceId rid) throws OpenRoadmModelException {
        return Boolean.parseBoolean(getStringValue(rid));
    }

    /**
     * Read link information from DynamicConfig.
     * @param linkId link ID
     * @return link information
     * @throws OpenRoadmModelException when a read error occurs
     */
    public OpenRoadmModelLink getLinkDetail(String linkId) throws OpenRoadmModelException {
        log.trace("get link detail({}).", linkId);
        OpenRoadmModelLink ret = new OpenRoadmModelLink();
        Boolean amplified = getBooleanValue(buildAmplifiedResourceId(linkId));
        LinkType linkType = LinkType.fromString(getStringValue(buildLinkTypeResourceId(linkId)));
        if (linkType != LinkType.ROADM_TO_ROADM) {
            return ret;
        }

        if (amplified) {
            ResourceId rid = buildAmplifiedLinkResourceId(linkId);
            ObjectNode root = convertDataNodeToJson(rid, getDataNode(rid));

            if (root != null) {
                JsonNode r = root.get("org-openroadm-network-topology:amplified-link");
                boolean lastAmp = false;
                for (JsonNode s : r.get("amplified-link")) {
                    JsonNode element = s.get("section-element");
                    if (element.has("ila")) {
                        ret.addElement(
                                OpenRoadmModelHelper.parseJsonToAmp(element.get("ila"))
                        );
                        lastAmp = true;
                    } else if (element.has("span")) {
                        ret.addElement(
                                OpenRoadmModelHelper.parseJsonToFiber(element.get("span"))
                        );
                        lastAmp = false;
                    }
                }
                if (lastAmp) {
                    ret.addElement(OpenRoadmModelHelper.createPreAmpFiber());
                }
            }
        } else {
            ResourceId rid = buildSpanResourceId(linkId);
            ObjectNode root = convertDataNodeToJson(rid, getDataNode(rid));
            if (root != null) {
                ret.addElement(OpenRoadmModelHelper.parseJsonToFiber(root.get("org-openroadm-network-topology:span")));
            }
        }
        if (log.isTraceEnabled()) {
            for (Element e : ret.getSectionElements()) {
                log.trace(e.toString());
            }
        }
        return ret;
    }

    /**
     * Get the link information in DynamicConfig.
     * @param src source connect point
     * @param dst destination connect point
     * @return link information
     */
    public OpenRoadmModelLink getLinkDetail(ConnectPoint src, ConnectPoint dst) {
        String linkId = OpenRoadmModelHelper.linkId(src, dst);

        OpenRoadmModelLink link;
        try {
            link = getLinkDetail(linkId);
        } catch (OpenRoadmModelException ex) {
            // Retry with a different ID format
            linkId = OpenRoadmModelHelper.linkWithPortNameId(src, dst);
            try {
                link = getLinkDetail(linkId);
            } catch (OpenRoadmModelException ex2) {
                log.warn("Not found openroadm link information", ex);
                return new OpenRoadmModelLink();
            }
        }
        return link;
    }

    // Copy from RestconfUtils. ------------------------------------------------

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
