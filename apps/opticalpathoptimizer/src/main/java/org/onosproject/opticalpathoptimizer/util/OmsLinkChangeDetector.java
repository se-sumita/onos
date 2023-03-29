package org.onosproject.opticalpathoptimizer.util;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.event.ListenerTracker;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detector of link changes between ROADM/AMP.
 */
public class OmsLinkChangeDetector {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);

    private ListenerTracker listenerTracker = null;

    private volatile boolean isChangedLink = false;

    /**
     * Get whether the topology (OMS-Line/FIBER link) has changed.
     * @return true if changed, otherwise false
     */
    public boolean isChanged() {
        return isChangedLink;
    }

    /**
     * Reset the flag of whether it has changed or not to the state of no change.
     */
    public void reset() {
        isChangedLink = false;
    }

    /**
     * Activate.
     */
    public void activate() {
        listenerTracker = new ListenerTracker();

        NetworkConfigService netCfgService = DefaultServiceDirectory.getService(NetworkConfigService.class);
        LinkService linkService = DefaultServiceDirectory.getService(LinkService.class);

        listenerTracker
                .addListener(netCfgService, new InnerNetworkConfigListener())
                .addListener(linkService, new InnerLinkListener())
                .addListener(deviceService, new InnerDeviceListener());
    }

    /**
     * Deactivate.
     */
    public void deactivate() {
        if (listenerTracker != null) {
            listenerTracker.removeListeners();
            listenerTracker = null;
        }
    }

    private void detectOmsLink(ConnectPoint src, ConnectPoint dst) {
        if (isOmsOrFiberPort(src) || isOmsOrFiberPort(dst)) {
            log.info("Detect a OMS/FIBER link changed. Link {} => {}", src, dst);
            isChangedLink = true;
        }
    }

    private boolean isOmsOrFiberPort(ConnectPoint point) {
        log.debug("Check OMS/FIBER port {}", point);
        Port port = deviceService.getPort(point);
        if (port == null) {
            log.debug("Could not check a port type, already removed: {}", point);
            return false;
        }
        return port.type() == Port.Type.OMS || port.type() == Port.Type.FIBER;
    }

    private class InnerNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (isChangedLink) {
                // No need to perform detection process as it has already been detected.
                return;
            }
            BasicLinkConfig config = (BasicLinkConfig) event.config().orElse(null);
            if (config != null) {
                LinkKey key = config.subject();
                detectOmsLink(key.src(), key.dst());
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass() == BasicLinkConfig.class;
        }
    }

    private class InnerLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            if (isChangedLink) {
                // No need to perform detection process as it has already been detected.
                return;
            }
            Link link = event.subject();
            detectOmsLink(link.src(), link.dst());
        }

        @Override
        public boolean isRelevant(LinkEvent event) {
            switch (event.type()) {
                case LINK_ADDED:
                case LINK_UPDATED:
                case LINK_REMOVED:
                    return true;
                default:
                    return false;
            }
        }
    }

    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (isChangedLink) {
                // No need to perform detection process as it has already been detected.
                return;
            }
            Device device = event.subject();
            switch (device.type()) {
                case ROADM:
                case ROADM_OTN:
                case OPTICAL_AMPLIFIER:
                    log.info("Detect a ROADM/AMP device removed. Device {}", device.id());
                    isChangedLink = true;
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type() == DeviceEvent.Type.DEVICE_REMOVED;
        }
    }
}
