package org.onosproject.opticalpathoptimizer.gui;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.ui.topo.BiLinkMap;

/**
 * Map for wavelength path link.
 */
public class WavelengthPathLinkMap extends BiLinkMap<WavelengthPathLink> {
    @Override
    protected WavelengthPathLink create(LinkKey linkKey, Link link) {
        return new WavelengthPathLink(linkKey, link);
    }
}
