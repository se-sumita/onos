package org.onosproject.opticalpathoptimizer.gui;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.ui.GlyphConstants;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;

import static org.onosproject.ui.topo.TopoConstants.Properties.*;

/**
 * Wavelength path topology overlay.
 */
public class WavelengthPathTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in wavelengthPathTopov.js
    private static final String OVERLAY_ID = "wavelength-path";

    private static final String WAVELENGTH_PATH_TITLE = "Wavelength Path";

    private static final String WAVELENGTH_PATH = "wavelength-path";

    private static final ButtonId WLPATH_BUTTON = new ButtonId("wavelengthpath");

    public WavelengthPathTopovOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void modifySummary(PropertyPanel pp) {
        WavelengthPathStore store = DefaultServiceDirectory.getService(WavelengthPathStore.class);

        pp.title(WAVELENGTH_PATH_TITLE)
                .glyphId(GlyphConstants.FIBER_SWITCH)
                .removeProps(
                        TOPOLOGY_SSCS,
                        TUNNELS,
                        FLOWS,
                        INTENTS,
                        VERSION
                )
                .addProp(WAVELENGTH_PATH, "Wavelength Paths", store.size());
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.removeProps(LATITUDE, LONGITUDE);

        pp.addButton(WLPATH_BUTTON);

        pp.removeButtons(CoreButtons.SHOW_FLOW_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW)
                .removeButtons(CoreButtons.SHOW_METER_VIEW);
    }
}
