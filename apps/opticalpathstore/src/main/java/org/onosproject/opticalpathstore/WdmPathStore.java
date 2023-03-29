package org.onosproject.opticalpathstore;

import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.openroadm.model.WdmPath;
import org.onosproject.net.openroadm.model.WdmPathKey;

import java.util.Collection;

/**
 * WDM path store.
 */
public interface WdmPathStore extends PathStore<WdmPathKey, WdmPath>,
        ListenerService<WdmPathEvent, WdmPathEventListener> {
    /**
     * Replace WDM path.
     * @param ingress ingress port
     * @param egress egress port
     * @param paths paths to be added
     */
    void replace(ConnectPoint ingress, ConnectPoint egress, Collection<WdmPath> paths);

    /**
     * Get reverse path.
     * @param path forward path
     * @return reverse path of specified path
     */
    WdmPath getReversePath(WdmPath path);
}
