package org.onosproject.openroadmprovider.util;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.Filter;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * DynamicConfig access utility.
 */
public class DynamicConfigUtils {
    private static final Logger log = LoggerFactory.getLogger(DynamicConfigUtils.class);
    private final DynamicConfigService dynamicConfig =
            DefaultServiceDirectory.getService(DynamicConfigService.class);

    /**
     * Create a data node on DynamicConfig.
     * @param resourceData the data to be created
     */
    public void create(ResourceData resourceData) {
        ResourceId rid = resourceData.resourceId();
        DataNode dataNode = resourceData.dataNodes().get(0);

        if (rid == null) {
            rid = ResourceId.builder().addBranchPointSchema("/", null).build();
            dataNode = removeTopNode(dataNode);
        }

        dynamicConfig.createNode(rid, dataNode);
    }

    /**
     * Update a data node on DynamicConfig.
     * @param resourceData the data to be updated
     */
    public void update(ResourceData resourceData) {
        ResourceId rid = resourceData.resourceId();
        DataNode dataNode = resourceData.dataNodes().get(0);

        if (rid == null) {
            rid = ResourceId.builder().addBranchPointSchema("/", null).build();
            dataNode = removeTopNode(dataNode);
        }

        dynamicConfig.updateNode(rid, dataNode);
    }

    /**
     * Delete a data node on DynamicConfig.
     * @param id the ID of the data to be deleted
     * @return whether it was deleted or not
     */
    public boolean deleteIfExist(ResourceId id) {
        if (dynamicConfig.nodeExist(id)) {
            dynamicConfig.deleteNode(id);
            return true;
        }
        return false;
    }

    /**
     * Check if the data exists in DynamicConfig.
     * @param id the ID of the data
     * @return true if it exists, false if otherwise
     */
    public boolean exists(ResourceId id) {
        return dynamicConfig.nodeExist(id);
    }

    /**
     * Create or update data in DynamicConfig.
     * @param id the ID of the data
     * @param resourceData the data for create or update
     */
    public void createOrUpdate(ResourceId id, ResourceData resourceData) {
        if (dynamicConfig.nodeExist(id)
                || dynamicConfig.nodeExist(resourceData.resourceId())) {
            update(resourceData);
        } else {
            create(resourceData);
        }
    }

    /**
     * Create or replace data in DynamicConfig.
     * @param id the ID of the data
     * @param resourceData the data for create or replace
     */
    public void createOrReplace(ResourceId id, ResourceData resourceData) {
        deleteIfExist(id);
        if (dynamicConfig.nodeExist(resourceData.resourceId())) {
            update(resourceData);
        } else {
            create(resourceData);
        }
    }

    /**
     * Read data nodes from DynamicConfig.
     * @param id the ID of the data
     * @return data retrieved, or null if it doesn't exist
     */
    public DataNode read(ResourceId id) {
        if (dynamicConfig.nodeExist(id)) {
            return dynamicConfig.readNode(id, Filter.builder().build());
        }
        return null;
    }

    private DataNode removeTopNode(DataNode dataNode) {
        if (dataNode instanceof InnerNode && dataNode.key().schemaId().name().equals("/")) {
            Map.Entry<NodeKey, DataNode> entry = ((InnerNode) dataNode).childNodes().entrySet().iterator().next();
            dataNode = entry.getValue();
        }
        return dataNode;
    }
}
