package org.onosproject.opticalpathoptimizer.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.TopoJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Collection;

/**
 * Wavelength-path Topology-Overlay message handler.
 */
public class WavelengthPathTopovMessageHandler extends UiMessageHandler {

    private static final String WLPATH_TOPOV_DISPLAY_START = "wlpathTopovDisplayStart";
    private static final String WLPATH_TOPOV_DISPLAY_UPDATE = "wlpathTopovDisplayUpdate";
    private static final String WLPATH_TOPOV_DISPLAY_STOP = "wlpathTopovDisplayStop";

    private static final String WPID0 = "wpid0";
    private static final String WPID1 = "wpid1";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private WavelengthPathStore pathStore;
    private DecimalFormat frequencyFormat = new DecimalFormat("#.00#");

    // ===============-=-=-=-=-=-======================-=-=-=-=-=-=-================================


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        pathStore = directory.get(WavelengthPathStore.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DisplayStartHandler(),
                new DisplayUpdateHandler(),
                new DisplayStopHandler()
        );
    }

    // === -------------------------
    // === Handler classes

    private final class DisplayStartHandler extends RequestHandler {
        public DisplayStartHandler() {
            super(WLPATH_TOPOV_DISPLAY_START);
        }

        @Override
        public void process(ObjectNode payload) {
            String wpid0 = string(payload, WPID0);
            String wpid1 = string(payload, WPID1);

            long wpid0l = toLong(wpid0, 0);
            long wpid1l = toLong(wpid1, 0);

            log.info("Start Display: wpid0 [{}] wpid1 [{}]", wpid0l, wpid1l);

            sendLinkData(wpid0l, wpid1l);
        }
    }

    private long toLong(String string, long defaultValue) {
        try {
            return Long.valueOf(string);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private final class DisplayUpdateHandler extends RequestHandler {
        public DisplayUpdateHandler() {
            super(WLPATH_TOPOV_DISPLAY_UPDATE);
        }

        @Override
        public void process(ObjectNode payload) {
            log.info("Update Display");
        }
    }

    private final class DisplayStopHandler extends RequestHandler {
        public DisplayStopHandler() {
            super(WLPATH_TOPOV_DISPLAY_STOP);
        }

        @Override
        public void process(ObjectNode payload) {
            log.info("Stop Display");
            clearState();
        }
    }

    // === ------------

    private void clearState() {
        sendHighlights(new Highlights());
    }

    private void sendHighlights(Highlights highlights) {
        sendMessage(TopoJson.highlightsMessage(highlights));
    }

    private void sendLinkData(long wpid0, long wpid1) {
        log.debug("Send link data {}, {}", wpid0, wpid1);
        Highlights highlights = new Highlights();

        if (wpid0 > 0) {
            WavelengthPath path = pathStore.get(wpid0);
            if (path != null) {
                WavelengthPathLinkMap linkMap = new WavelengthPathLinkMap();
                path.links().forEach(linkMap::add);
                for (WavelengthPathLink link : linkMap.biLinks()) {
                    String frequency = getFrequencyText(path);
                    link.setLabel(frequency)
                        .makeMaster();
                    highlights.add(link.highlight(null));
                }
            }
        }

        if (wpid1 > 0) {
            WavelengthPath path = pathStore.get(wpid1);
            if (path != null) {
                WavelengthPathLinkMap linkMap = new WavelengthPathLinkMap();
                path.links().forEach(linkMap::add);
                for (WavelengthPathLink link : linkMap.biLinks()) {
                    String frequency = getFrequencyText(path);
                    link.setLabel(frequency);
                    highlights.add(link.highlight(null));
                }
            }
        }

        sendHighlights(highlights);
    }

    private String getFrequencyText(WavelengthPath path) {
        return String.format("[%d] %sTHz",
                             path.frequencyId(),
                             frequencyFormat.format(path.signal().centralFrequency().asTHz()));
    }

}
