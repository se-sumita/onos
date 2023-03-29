package org.onosproject.opticalpathstore;

import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WavelengthPath;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wavelength path store.
 */
public interface WavelengthPathStore extends PathStore<Long, WavelengthPath>,
        ListenerService<WavelengthPathEvent, WavelengthPathEventListener> {

    /**
     * Build wavelength path. (Do not add to the store.)
     * @param groupId group ID
     * @param ingressEdge edge(link) of Ingress
     * @param egressEdge edge(link) of Egress
     * @param path path to be added
     * @param frequencyId frequency ID
     * @param signal signal information
     * @param rate rate
     * @param modulationFormat modulation-format
     * @param qValue Q value
     * @param qThreshold Q threshold
     * @param name path name
     * @return new wavelength path
     */
    WavelengthPath build(
            long groupId,
            Link ingressEdge, Link egressEdge, Path path,
            int frequencyId, OchSignal signal,
            Rate rate, ModulationFormat modulationFormat,
            double qValue, double qThreshold,
            String name);

    /**
     * Add wavelength paths to the store.
     * @param paths wavelength path to be added which belong to same group.
     */
    void addAll(List<WavelengthPath> paths);

    /**
     * Update wavelength path in the store.
     * @param path wavelength path to be updated
     * @return wavelength path ID
     */
    long update(WavelengthPath path);

    /**
     * Remove wavelength path from the store.
     * @param id wavelength path id to be removed
     */
    void remove(long id);

    /**
     * Remove paths which have specified ingress/egress port.
     * @param ingress ingress port
     * @param egress egress port
     */
    void remove(ConnectPoint ingress, ConnectPoint egress);

    /**
     * Remove wavelength path from the store.
     * @param groupId wavelength path group ID to be removed
     */
    void removeAll(long groupId);

    /**
     * Remove wavelength path from the store.
     * @param paths wavelength paths to be removed
     */
    void removeAll(List<WavelengthPath> paths);

    /**
     * Find wavelength path by path group ID.
     * @param groupId path group ID
     * @return wavelength path list which belong to specified group
     */
    List<WavelengthPath> findByGroupId(long groupId);

    /**
     * Find wavelength path by OMS port and lambda.
     * @param omsPort Connect point of OMS port
     * @param signal lambda
     * @return wavelength path
     */
    WavelengthPath findByOmsPortAndLambda(ConnectPoint omsPort, OchSignal signal);

    /**
     * Get wavelength paths grouped by GroupID.
     * @return wavelength path list grouped by GroupID
     */
    Map<Long, Collection<WavelengthPath>> getGroupMap();

    long issueGroupId();

    void releaseGroupIdIfPossible(long groupId);

    int size();
}
