package org.onosproject.net.openroadm.codec;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.openroadm.intent.WavelengthPathIntent;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WavelengthPath;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Wavelength path intent JSON codec.
 */
public class WavelengthPathIntentCodec extends JsonCodec<WavelengthPathIntent> {
    private static final String PATHS = "paths";
    private static final String SIGNAL_TYPE = "signalType";
    private static final String RATE = "rate";
    private static final String MODULATION_FORMAT = "modulationFormat";
    private static final String IS_BIDIRECTIONAL = "isBidirectional";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in wavelength path intent";

    @Override
    public ObjectNode encode(WavelengthPathIntent intent, CodecContext context) {
        checkNotNull(intent, "Wavelength path intent cannot be null");

        final JsonCodec<WavelengthPath> wavelengthPathCodec =
                context.codec(WavelengthPath.class);

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode paths = result.putArray(PATHS);
        for (Long pathId : intent.pathIds()) {
            paths.add(pathId);
        }
        result.put(SIGNAL_TYPE, intent.signalType().toString())
                .put(RATE, intent.rate().toString())
                .put(MODULATION_FORMAT, intent.modulationFormat().name())
                .put(IS_BIDIRECTIONAL, intent.isBidirectional());

        return result;
    }

    @Override
    public WavelengthPathIntent decode(ObjectNode json, CodecContext context) {
        final JsonCodec<WavelengthPath> wavelengthPathCodec =
                context.codec(WavelengthPath.class);

        WavelengthPathIntent.Builder builder = WavelengthPathIntent.builder();
        IntentCodecUtils.intentAttributes(json, context, builder);

        ArrayNode arrayNode = (ArrayNode) nullIsIllegal(json.get(PATHS),
                PATHS + MISSING_MEMBER_MESSAGE);
        Rate rate = Rate.valueOf(nullIsIllegal(json.get(RATE), RATE + MISSING_MEMBER_MESSAGE).asText());
        ModulationFormat modulationFormat = ModulationFormat.valueOf(
                nullIsIllegal(json.get(MODULATION_FORMAT),
                        MODULATION_FORMAT + MISSING_MEMBER_MESSAGE).asText());
        boolean isBidirectional = nullIsIllegal(json.get(IS_BIDIRECTIONAL),
                IS_BIDIRECTIONAL + MISSING_MEMBER_MESSAGE).asBoolean();
        builder.addWavelengthPaths(wavelengthPathCodec.decode(arrayNode, context))
                .rate(rate)
                .modulationFormat(modulationFormat)
                .isBidirectional(isBidirectional);

        return builder.build();
    }
}
