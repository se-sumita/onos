package org.onosproject.opticalpathoptimizer.gui;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.ui.topo.BiLink;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;
import org.onosproject.ui.topo.Mod;

/**
 * Wavelength path link for highlighting.
 */
public class WavelengthPathLink extends BiLink {

    private String label = null;

    private boolean isMaster = false;

    /* CSS class name of link line */
    private static final Mod MOD_MASTER = new Mod("master");
    private static final Mod MOD_RECOVERY = new Mod("recovery");

    public WavelengthPathLink(LinkKey key, Link link) {
        super(key, link);
    }

    public WavelengthPathLink makeMaster() {
        isMaster = true;
        return this;
    }

    public WavelengthPathLink setLabel(String label) {
        this.label = label;
        return this;
    }

    @Override
    public LinkHighlight highlight(Enum<?> anEnum) {
        Flavor flavor;
        Mod pathType;
        if (isMaster) {
            flavor = Flavor.PRIMARY_HIGHLIGHT;
            pathType = MOD_MASTER;
        } else {
            flavor = Flavor.SECONDARY_HIGHLIGHT;
            pathType = MOD_RECOVERY;
        }

        return new LinkHighlight(this.linkId(), flavor)
                .addMod(LinkHighlight.MOD_ANIMATED)
                .setLabel(label)
                .addMod(pathType);
    }
}
