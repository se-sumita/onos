package org.onosproject.openroadmprovider.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultCompositeStream;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.RuntimeContext;
import org.onosproject.yang.runtime.YangRuntimeService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility for YANG Runtime.
 */
public class YangRuntimeUtils {
    private static final String JSON_FORMAT = "JSON";

    private static final YangRuntimeService YANG_RUNTIME =
            DefaultServiceDirectory.getService(YangRuntimeService.class);

    /**
     * Convert a JSON node to a data node.
     * @param uri URI
     * @param object object to be converted
     * @return data node
     */
    public ResourceData toDataNode(String uri, JsonNode object) {
        return toDataNode(uri, toInputStream(object));
    }

    /**
     * Convert JSON stream to a data node.
     * @param uri URI
     * @param jsonStream input JSON stream
     * @return data node
     */
    public ResourceData toDataNode(String uri, InputStream jsonStream) {
        RuntimeContext context = new DefaultRuntimeContext.Builder()
                .setDataFormat(JSON_FORMAT)
                .build();

        CompositeStream compositeStream = new DefaultCompositeStream(uri, jsonStream);
        CompositeData compositeData = YANG_RUNTIME.decode(compositeStream, context);
        return compositeData.resourceData();
    }

    /**
     * Convert a data node to a JSON object.
     * @param id resource ID
     * @param dataNode data node to be converted
     * @return JSON object
     */
    public ObjectNode toObjectNode(ResourceId id, DataNode dataNode) {
        RuntimeContext context = new DefaultRuntimeContext.Builder()
                .setDataFormat(JSON_FORMAT)
                .build();
        ResourceData resourceData = DefaultResourceData.builder()
                .addDataNode(dataNode)
                .resourceId(id)
                .build();
        CompositeData compositeData = DefaultCompositeData.builder()
                .resourceData(resourceData)
                .build();
        ObjectNode rootNode = null;
        try {
            CompositeStream compositeStream = YANG_RUNTIME.encode(compositeData, context);
            rootNode = toObjectNode(compositeStream.resourceData());
        } catch (Exception ex) {
            // through
        }
        return rootNode;
    }

    /**
     * Concatenate resource IDs.
     * @param prefix prefix
     * @param path path
     * @return Concatenated resource ID
     */
    ResourceId concatResourceId(ResourceId prefix, ResourceId path) {
        try {
            return prefix.copyBuilder().append(path).build();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException("Could not copy " + path, e);
        }
    }

    private InputStream toInputStream(JsonNode object) {
        return IOUtils.toInputStream(object.toString(), StandardCharsets.UTF_8);
    }

    private ObjectNode toObjectNode(InputStream inputStream) {
        ObjectNode rootNode = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            rootNode = (ObjectNode) mapper.readTree(inputStream);
        } catch (IOException e) {
            // through
        }
        return rootNode;
    }
}
