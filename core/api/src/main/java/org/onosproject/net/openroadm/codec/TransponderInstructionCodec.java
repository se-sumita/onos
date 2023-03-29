package org.onosproject.net.openroadm.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.openroadm.flow.instructions.TransponderInstruction;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Transponder instruction codec.
 */
public class TransponderInstructionCodec extends JsonCodec<TransponderInstruction> {
    private static final String RATE = "rate";
    private static final String MODULATION_FORMAT = "modulationFormat";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in wavelength path";

    @Override
    public ObjectNode encode(TransponderInstruction instruction, CodecContext context) {
        checkNotNull(instruction, "Instruction cannot be null");

        return context.mapper().createObjectNode()
                .put(RATE, instruction.rate().toString())
                .put(MODULATION_FORMAT, instruction.modulationFormat().name());
    }

    @Override
    public TransponderInstruction decode(ObjectNode json, CodecContext context) {
        String r = nullIsIllegal(json.get(RATE),
                                 RATE + MISSING_MEMBER_MESSAGE).asText();
        Rate rate = Rate.valueOf(r);
        String md = nullIsIllegal(json.get(MODULATION_FORMAT),
                                  MODULATION_FORMAT + MISSING_MEMBER_MESSAGE).asText();
        ModulationFormat modulationFormat = ModulationFormat.valueOf(md);

        return TransponderInstruction.of(rate, modulationFormat);
    }
}
