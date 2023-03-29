package org.onosproject.net.openroadm.behaviors;

import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;

import java.util.Set;

/**
 * Behaviour to get supported transmission rates and modulation format in the transponder.
 */
public interface TransponderCapabilityQuery extends HandlerBehaviour {
    /**
     * Get supported transmission rates.
     * @return List of supported transmission rates
     */
    Set<Rate> getSupportRates();

    /**
     * Get supported modulation format.
     * @return List of supported modulation format
     */
    Set<ModulationFormat> getSupportedModulationFormats();
}
