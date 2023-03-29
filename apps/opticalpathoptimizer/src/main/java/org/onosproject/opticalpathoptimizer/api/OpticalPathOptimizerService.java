package org.onosproject.opticalpathoptimizer.api;

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathoptimizer.WavelengthPathCandidate;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.opticalpathstore.WdmPathStore;

import java.util.List;

/**
 * Optical Path Optimizer.
 */
@Beta
public interface OpticalPathOptimizerService {

    /**
     * Perform WDM path calculation.
     *
     * WDM path: Path between OMS-Add/Drop ports.
     * @param ingress Ingress port
     * @param egress  Egress port
     */
    void calculateWdmPaths(ConnectPoint ingress, ConnectPoint egress);

    /**
     * Perform wavelength path calculation.
     *
     * Wavelength path: Path between OCh ports.
     * When multiple paired ports are specified, search for disjoint routes.
     * @param portPairs paired ingress/egress ports
     */
    void calculateWavelengthPaths(List<Pair<ConnectPoint, ConnectPoint>> portPairs);

    /**
     * Reserve a wavelength path.
     * @param index index of the candidate wavelength path
     * @param frequencyIds List of IDs of frequencies
     * @param names List of wavelength path names
     * @return List of reserved wavelength paths
     */
    List<WavelengthPath> reserveWavelengthPath(int index, List<Integer> frequencyIds, List<String> names);

    /**
     * Submit a wavelength path.
     *
     * @param submitId Submit ID of the wavelength path to be submitted
     */
    void submitWavelengthPath(long submitId);

    /**
     * Remove a wavelength path.
     *
     * @param submitId Submit ID of the wavelength path to be removed
     */
    void removeWavelengthPath(long submitId);

    /**
     * Get the WDM path store.
     * @return WDM path store
     */
    WdmPathStore getWdmPathStore();

    /**
     * Get the result of wavelength path calculation (wavelength path candidate).
     * @return List of wavelength path candidates
     */
    List<WavelengthPathCandidate> getWavelengthPathCandidates();

    /**
      * Get the wavelength path store.
     * @return wavelength path store
    */
    WavelengthPathStore getWavelengthPathStore();

    /**
     * Get whether the topology has changed after WDM path calculation.
     *
     * @return true if there is a change, false otherwise
     */
    boolean getWdmCalcNecessary();

}
