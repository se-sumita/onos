package org.onosproject.net.openroadm.codec;


import org.onosproject.codec.CodecService;
import org.onosproject.net.openroadm.flow.instructions.TransponderInstruction;
import org.onosproject.net.openroadm.intent.WavelengthPathIntent;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for Wavelength path.
 */
@Component(immediate = true)
public class WavelengthPathCodecRegistrator {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    @Activate
    public void activate() {
        codecService.registerCodec(WavelengthPath.class, new WavelengthPathCodec());
        codecService.registerCodec(WavelengthPathIntent.class, new WavelengthPathIntentCodec());
        codecService.registerCodec(TransponderInstruction.class, new TransponderInstructionCodec());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        codecService.unregisterCodec(WavelengthPath.class);
        codecService.unregisterCodec(WavelengthPathIntent.class);
        codecService.unregisterCodec(TransponderInstruction.class);

        log.info("Stopped");
    }
}
