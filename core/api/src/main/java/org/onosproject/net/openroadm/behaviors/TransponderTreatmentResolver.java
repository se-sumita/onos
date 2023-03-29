package org.onosproject.net.openroadm.behaviors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.ExtensionTreatmentCodec;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.openroadm.flow.instructions.TransponderInstruction;

/**
 * Behaviour for handling extension treatment of transponder option.
 */
public class TransponderTreatmentResolver extends AbstractHandlerBehaviour
        implements ExtensionTreatmentResolver, ExtensionTreatmentCodec {
    @Override
    public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
        if (type.equals(TransponderInstruction.TREATMENT_TRANSPONDER_TYPE)) {
            return new TransponderInstruction();
        }
        throw new UnsupportedOperationException("Driver does not support extension type " + type.toString());
    }

    @Override
    public ObjectNode encode(ExtensionTreatment extensionTreatment, CodecContext context) {
        if (extensionTreatment instanceof TransponderInstruction) {
            CodecService codecService = DefaultServiceDirectory.getService(CodecService.class);
            JsonCodec<TransponderInstruction> codec = codecService.getCodec(TransponderInstruction.class);
            return codec.encode((TransponderInstruction) extensionTreatment, context);
        }
        throw new UnsupportedOperationException("Extension type is not supported");
    }

    @Override
    public ExtensionTreatment decode(ObjectNode objectNode, CodecContext context) {
        CodecService codecService = DefaultServiceDirectory.getService(CodecService.class);
        JsonCodec<TransponderInstruction> codec = codecService.getCodec(TransponderInstruction.class);
        return codec.decode(objectNode, context);
    }
}
