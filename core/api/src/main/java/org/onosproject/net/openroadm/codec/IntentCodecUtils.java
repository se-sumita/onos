package org.onosproject.net.openroadm.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.core.CoreService;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Intent JSON codec.
 * Copy from org.onosproject.codec.impl.IntentCodec
 */
public final class IntentCodecUtils {

    private IntentCodecUtils() {}

    protected static final String ID = "id";
    protected static final String KEY = "key";
    protected static final String APP_ID = "appId";
    protected static final String PRIORITY = "priority";
    protected static final String RESOURCE_GROUP = "resourceGroup";
    protected static final String MISSING_MEMBER_MESSAGE =
            " member is required in Intent";
    private static final String E_APP_ID_NOT_FOUND =
            "Application ID is not found";

    /**
     * Extracts base intent specific attributes from a JSON object
     * and adds them to a builder.
     *
     * @param json root JSON object
     * @param context code context
     * @param builder builder to use for storing the attributes
     */
    public static void intentAttributes(ObjectNode json, CodecContext context,
                                    Intent.Builder builder) {
        String appId = nullIsIllegal(json.get(IntentCodecUtils.APP_ID),
                                     IntentCodecUtils.APP_ID + IntentCodecUtils.MISSING_MEMBER_MESSAGE).asText();
        CoreService service = context.getService(CoreService.class);
        builder.appId(nullIsNotFound(service.getAppId(appId), IntentCodecUtils.E_APP_ID_NOT_FOUND));

        JsonNode priorityJson = json.get(IntentCodecUtils.PRIORITY);
        if (priorityJson != null) {
            builder.priority(priorityJson.asInt());
        }

        JsonNode keyJson = json.get(IntentCodecUtils.KEY);
        if (keyJson != null) {
            String keyString = keyJson.asText();
            if (keyString.startsWith("0x")) {
                // The intent uses a LongKey
                keyString = keyString.replaceFirst("0x", "");
                builder.key(Key.of(Long.parseLong(keyString, 16), service.getAppId(appId)));
            } else {
                // The intent uses a StringKey
                builder.key(Key.of(keyString, service.getAppId(appId)));
            }
        }

        JsonNode resourceGroup = json.get(IntentCodecUtils.RESOURCE_GROUP);
        if (resourceGroup != null) {
            String resourceGroupId = resourceGroup.asText();
            builder.resourceGroup(ResourceGroup.of(
                    Long.parseUnsignedLong(resourceGroupId.substring(2), 16)
            ));
        }
    }
}
