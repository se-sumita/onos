package org.onosproject.net.openroadm.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WavelengthPath;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Wavelength path JSON codec
 */
public class WavelengthPathCodec extends JsonCodec<WavelengthPath> {
    private static final String ID = "id";
    private static final String GROUP_ID = "groupId";
    private static final String FREQUENCY_ID = "frequencyId";
    private static final String SIGNAL = "signal";
    private static final String INGRESS_EDGE = "ingressEdge";
    private static final String EGRESS_EDGE = "egressEdge";
    private static final String PATH = "path";
    private static final String RATE = "rate";
    private static final String MODULATION_FORMAT = "modulationFormat";
    private static final String Q_VALUE = "qValue";
    private static final String Q_THRESHOLD = "qThreshold";
    private static final String NAME = "name";
    private static final String SUBMITTED = "submitted";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in wavelength path";

    @Override
    public ObjectNode encode(WavelengthPath path, CodecContext context) {
        checkNotNull(path, "Path cannot be null");

        final JsonCodec<OchSignal> ochSignalCodec = context.codec(OchSignal.class);
        final JsonCodec<Link> linkCodec = context.codec(Link.class);
        final JsonCodec<Path> pathCodec = context.codec(Path.class);

        final ObjectNode result = context.mapper().createObjectNode()
                .put(ID, path.id())
                .put(GROUP_ID, path.groupId())
                .put(FREQUENCY_ID, path.frequencyId());
        result.set(SIGNAL, ochSignalCodec.encode(path.signal(), context));
        result.set(INGRESS_EDGE, linkCodec.encode(path.ingressEdge(), context));
        result.set(EGRESS_EDGE, linkCodec.encode(path.egressEdge(), context));
        result.set(PATH, pathCodec.encode(path.path(), context));
        result.put(RATE, path.rate().name());
        result.put(MODULATION_FORMAT, path.modulationFormat().name());
        result.put(Q_VALUE, path.qValue())
                .put(Q_THRESHOLD, path.qThreshold())
                .put(NAME, path.name())
                .put(SUBMITTED, path.isSubmitted());
        return result;
    }

    @Override
    public WavelengthPath decode(ObjectNode json, CodecContext context) {
        final JsonCodec<OchSignal> ochSignalCodec = context.codec(OchSignal.class);
        final JsonCodec<Link> linkCodec = context.codec(Link.class);
        final JsonCodec<Path> pathCodec = context.codec(Path.class);

        long id = nullIsIllegal(json.get(ID),
                ID + MISSING_MEMBER_MESSAGE).asLong();
        long groupId = nullIsIllegal(json.get(GROUP_ID),
                GROUP_ID + MISSING_MEMBER_MESSAGE).asLong();
        int frequencyId = nullIsIllegal(json.get(FREQUENCY_ID),
                FREQUENCY_ID + MISSING_MEMBER_MESSAGE).asInt();
        OchSignal signal = ochSignalCodec.decode(get(json, SIGNAL), context);
        Link ingressEdge = linkCodec.decode(get(json, INGRESS_EDGE), context);
        Link egressEdge = linkCodec.decode(get(json, EGRESS_EDGE), context);
        Path path = pathCodec.decode(get(json, PATH), context);
        Rate rate = Rate.valueOf(
                nullIsIllegal(json.get(RATE),
                        RATE + MISSING_MEMBER_MESSAGE).asText());
        ModulationFormat modFormat = ModulationFormat.valueOf(
                nullIsIllegal(json.get(MODULATION_FORMAT),
                        MODULATION_FORMAT + MISSING_MEMBER_MESSAGE).asText());
        double qValue = nullIsIllegal(json.get(Q_VALUE),
                Q_VALUE + MISSING_MEMBER_MESSAGE).asDouble();
        double qThreshold = nullIsIllegal(json.get(Q_THRESHOLD),
                Q_THRESHOLD + MISSING_MEMBER_MESSAGE).asDouble();
        String name = json.get(NAME).asText();
        boolean submitted = nullIsIllegal(json.get(SUBMITTED),
                SUBMITTED + MISSING_MEMBER_MESSAGE).asBoolean();

        return WavelengthPath.create(
                id, groupId, frequencyId, signal,
                ingressEdge, egressEdge, path,
                rate, modFormat, qValue, qThreshold, name, submitted);
    }
}
