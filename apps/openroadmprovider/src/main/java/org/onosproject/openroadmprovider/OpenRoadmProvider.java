package org.onosproject.openroadmprovider;

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.event.ListenerTracker;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.openroadm.model.AmpType;
import org.onosproject.net.openroadm.model.FiberType;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.net.openroadm.model.WdmPath;
import org.onosproject.openroadmprovider.api.AmpPair;
import org.onosproject.openroadmprovider.api.Fiber;
import org.onosproject.openroadmprovider.api.OmsLinkConfig;
import org.onosproject.openroadmprovider.util.NetworkModelUtils;
import org.onosproject.openroadmprovider.util.ServiceModelUtils;
import org.onosproject.opticalpathstore.WavelengthPathEvent;
import org.onosproject.opticalpathstore.WavelengthPathEventListener;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.opticalpathstore.WdmPathEvent;
import org.onosproject.opticalpathstore.WdmPathEventListener;
import org.onosproject.opticalpathstore.WdmPathStore;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20171215.orgopenroadmcommontypes.amplifiertypes.AmplifierTypesEnum;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20171215.orgopenroadmcommontypes.modulationformat.ModulationFormatEnum;
import org.onosproject.yang.gen.v1.orgopenroadmlink.rev20171215.orgopenroadmlink.spanattributes.linkconcatenation.FiberTypeEnum;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.config.basics.SubjectFactories.LINK_SUBJECT_FACTORY;
import static org.onosproject.openroadmprovider.OsgiPropertyConstants.DEFAULT_HANDLE_WDM_PATH_SERVICE;
import static org.onosproject.openroadmprovider.OsgiPropertyConstants.HANDLE_WDM_PATH_SERVICE;

@Component(
        immediate = true,
        property = {
                HANDLE_WDM_PATH_SERVICE + ":Boolean=" + DEFAULT_HANDLE_WDM_PATH_SERVICE
        }
)
public class OpenRoadmProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private WdmPathStore wdmPathStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private WavelengthPathStore wavelengthPathStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigRegistry netConfigRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigService netCfgService;

    /** Save WDM paths to the DynamicConfigStore (service-list model). */
    private boolean handleWdmPathService;

    private ExecutorService executor;
    private ExecutorService executorForWdmPath;

    private long workerThreadId = 0;

    private ListenerTracker listenerTracker = new ListenerTracker();

    private final ConfigFactory<LinkKey, OmsLinkConfig> configFactory =
            new ConfigFactory<>(LINK_SUBJECT_FACTORY,
                                OmsLinkConfig.class, OmsLinkConfig.CONFIG_KEY) {
                @Override
                public OmsLinkConfig createConfig() {
                    return new OmsLinkConfig();
                }
            };

    @Activate
    protected void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "openroadm-worker");
            workerThreadId = thread.getId();
            return thread;
        });
        executorForWdmPath = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "openroadm-wdm-worker")
        );

        // Import OpenROADM definitions
        for (AmplifierTypesEnum type : AmplifierTypesEnum.values()) {
            log.debug("Defined amplifier-type: {}", AmpType.valueOf(type.toString()));
        }

        for (FiberTypeEnum type : FiberTypeEnum.values()) {
            log.debug("Defined fiber-type: {}", FiberType.valueOf(type.toString()));
        }

        for (ModulationFormatEnum type : ModulationFormatEnum.values()) {
            log.debug("Defined modulation-format: {}", ModulationFormat.valueOf(type.toString()));
        }

        // Create the root node.
        // If data exists, it is cleared and regenerated because there will be
        // data differences with each store depending on the timing of the activate.
        try {
            NetworkModelUtils.clearNetworkTree();
            ServiceModelUtils.clearServiceListTree();
            // Reflecting device information
            deviceService.getDevices().forEach(this::updateNetworkDevice);

        } catch (Exception ex) {
            log.warn("Exception occurred in network/service model building", ex);
        }

        // If there are unprocessed oms-link, they will be processed here.
        // * Therefore, the device information is reflected first.
        netConfigRegistry.registerConfigFactory(configFactory);

        // Registration of listeners
        listenerTracker
                .addListener(deviceService, new InnerDeviceListener())
                .addListener(linkService, new InnerLinkListener())
                .addListener(netCfgService, new InnerNetworkConfigEventListener())
                .addListener(wdmPathStore, new InnerWdmPathEventListener())
                .addListener(wavelengthPathStore, new InnerWavelengthPathEventListener());

        try {
            // Reflecting link information
            linkService.getActiveLinks().forEach(this::updateNetworkLink);

            // Reflecting service information
            //  WDM path
            replaceWdmPath(wdmPathStore.getPaths(), Collections.emptyList());
            //  Wavelength path
            Map<Long, Collection<WavelengthPath>> paths = wavelengthPathStore.getGroupMap();
            paths.values().forEach(this::updateWavelengthPathGroup);
        } catch (Exception ex) {
            log.warn("Exception occurred in link/path model building", ex);
        }

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        listenerTracker.removeListeners();

        netConfigRegistry.unregisterConfigFactory(configFactory);

        executor.shutdown();
        executorForWdmPath.shutdown();

        cfgService.unregisterProperties(getClass(), false);

        // Model data will not be deleted.
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        handleWdmPathService = Tools.isPropertyEnabled(
                properties, "handleWdmPathService"
        );
    }

    //-------------------------------------------------------------//
    // Network Model: Device Information
    //-------------------------------------------------------------//
    private void updateNetworkDevice(Device device) {
        runOnWorkerThread(() -> {
            log.debug("Add/Update device: id[{}]", device.id());
            NetworkModelUtils.updateNetworkNode(device);
        });
    }

    private void removeNetworkDevice(Device device) {
        runOnWorkerThread(() -> {
            log.debug("Remove device: id[{}]", device.id());
            NetworkModelUtils.removeNetworkNode(device);
        });
    }

    private void updateNetworkDevicePort(Device device, Port port) {
        runOnWorkerThread(() -> {
            log.debug("Add/Update port: id[{}/{}]", device.id(), port.number());
            NetworkModelUtils.updateNetworkDevicePort(device, port);
        });
    }

    private void removeNetworkDevicePort(Device device, Port port) {
        runOnWorkerThread(() -> {
            log.debug("Remove port: id[{}/{}]", device.id(), port.number());
            NetworkModelUtils.removeNetworkDevicePort(device, port);
        });
    }

    //-------------------------------------------------------------//
    // Network Model: Link Information
    //-------------------------------------------------------------//
    private void updateNetworkLink(Link link) {
        updateNetworkLink(LinkKey.linkKey(link));
    }

    private void updateNetworkLink(LinkKey linkKey) {
        runOnWorkerThread(() -> {
            log.debug("Add/Update link: src[{}] dst[{}]", linkKey.src(), linkKey.dst());
            try {
                updateNetworkLinkInWorker(linkKey);
            } catch (NullPointerException ex) {
                // Exception by checkNotNull
                log.debug("Link is already removed: src[{}] dst[{}]", linkKey.src(), linkKey.dst());
            }
        });
    }


    private void updateNetworkLinkInWorker(LinkKey linkKey) {
        Device aEndDevice = deviceService.getDevice(linkKey.src().deviceId());
        Device bEndDevice = deviceService.getDevice(linkKey.dst().deviceId());
        checkNotNull(aEndDevice, linkKey.src().deviceId());
        checkNotNull(bEndDevice, linkKey.dst().deviceId());

        Port aEndPort = deviceService.getPort(linkKey.src());
        Port bEndPort = deviceService.getPort(linkKey.dst());
        checkNotNull(aEndPort, linkKey.src());
        checkNotNull(bEndPort, linkKey.dst());

        AmpPair ampPair = null;
        Fiber fiber = null;
        if (OmsLinkConfig.isRelevant(aEndDevice, aEndPort, bEndDevice, bEndPort)) {
            OmsLinkConfig config = netCfgService.getConfig(linkKey, OmsLinkConfig.class);
            if (config != null) {
                ampPair = config.getAToBAmps();
                fiber = config.getFiber();
            } else { // reverse direction
                config = netCfgService.getConfig(
                        LinkKey.linkKey(linkKey.dst(), linkKey.src()), OmsLinkConfig.class);
                if (config != null) {
                    log.debug("Use reverse link key. LINK[{}]",
                              OmsLinkConfig.toLinkName(bEndDevice, bEndPort, aEndDevice, aEndPort));
                    ampPair = config.getBToAAmps().reverse();
                    fiber = config.getFiber(true);
                } else {
                    log.warn("Missing 'oms-link' in LINK[{}]",
                             OmsLinkConfig.toLinkName(aEndDevice, aEndPort, bEndDevice, bEndPort));
                }
            }
        }
        NetworkModelUtils.updateNetworkLink(aEndDevice, aEndPort, bEndDevice, bEndPort, ampPair, fiber);
    }

    private void removeNetworkLink(Link link) {
        netCfgService.removeConfig(LinkKey.linkKey(link), OmsLinkConfig.class);
        runOnWorkerThread(() -> {
            log.debug("Remove link: src[{}] dst[{}]", link.src(), link.dst());
            NetworkModelUtils.removeNetworkLink(link.src(), link.dst());
        });
    }

    //-------------------------------------------------------------//
    // Service Model: WDM Path Information
    //-------------------------------------------------------------//
    private void replaceWdmPath(Collection<WdmPath> pathsAdded, Collection<WdmPath> pathsRemoved) {
        if (handleWdmPathService) {
            executorForWdmPath.execute(() -> {
                // When a large number of results are submitted, the listener's
                // time limit will be exceeded and interrupted, so it should be
                // executed in a separate thread.
                log.debug("Replace WDM path: removed[{}] added[{}]",
                          pathsRemoved.size(), pathsAdded.size());
                ServiceModelUtils.replaceWdmPaths(pathsAdded, pathsRemoved);
            });
        }
    }

    //-------------------------------------------------------------//
    // Service Model: Wavelength Path Information
    //-------------------------------------------------------------//

    private void updateWavelengthPath(WavelengthPath wavelengthPath, Set<Long> coupledServices) {
        runOnWorkerThread(() -> {
            log.debug("Update wavelength path: id[{}] submit id[{}]",
                    wavelengthPath.id(), wavelengthPath.groupId());
            ServiceModelUtils.updateWavelengthPath(wavelengthPath, coupledServices);

            // Update the resource information of the transit OMS port.
            wavelengthPath.path().links().stream()
                    .flatMap(l -> Stream.of(l.src(), l.dst()))
                    .map(deviceService::getPort)
                    .filter(Objects::nonNull)
                    .filter(p -> p.type() == Port.Type.OMS)
                    .distinct()
                    .forEach(p -> updateNetworkDevicePort((Device) p.element(), p));
        });
    }

    private void removeWavelengthPath(WavelengthPath wavelengthPath) {
        runOnWorkerThread(() -> {
            log.debug("Remove wavelength path: id[{}] submit id[{}]",
                    wavelengthPath.id(), wavelengthPath.groupId());
            ServiceModelUtils.removeWavelengthPath(wavelengthPath);

            // Update the resource information of the transit OMS port.
            wavelengthPath.path().links().stream()
                    .flatMap(l -> Stream.of(l.src(), l.dst()))
                    .map(deviceService::getPort)
                    .filter(Objects::nonNull)
                    .filter(p -> p.type() == Port.Type.OMS)
                    .distinct()
                    .forEach(p -> updateNetworkDevicePort((Device) p.element(), p));
        });
    }

    private void updateWavelengthPathGroup(Collection<WavelengthPath> group) {
        runOnWorkerThread(() -> {
            Set<Long> coupledServices = group.stream().map(WavelengthPath::id).collect(Collectors.toSet());
            group.forEach((wavelengthPath) -> {
                log.debug("Add wavelength path: id[{}] submit id[{}]",
                        wavelengthPath.id(), wavelengthPath.groupId());
                ServiceModelUtils.updateWavelengthPath(wavelengthPath, coupledServices);
            });
        });
    }

    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            // log.debug("Device event: id[{}] type[{}]", event.subject().id(), event.type());
            try {
                switch (event.type()) {
                    case DEVICE_ADDED:
                    case DEVICE_UPDATED:
                        updateNetworkDevice(event.subject());
                        break;

                    case DEVICE_REMOVED:
                    case DEVICE_SUSPENDED:
                        removeNetworkDevice(event.subject());
                        break;

                    case PORT_ADDED:
                    case PORT_UPDATED:
                        updateNetworkDevicePort(event.subject(), event.port());
                        break;

                    case PORT_REMOVED:
                        removeNetworkDevicePort(event.subject(), event.port());

                    default:
                        break;
                }
            } catch (Exception ex) {
                log.error("Error on device listener", ex);
            }
        }
    }

    private class InnerLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            log.debug("Link event: src[{}] dst[{}] type[{}]",
                    event.subject().src(), event.subject().dst(), event.type());
            try {
                switch (event.type()) {
                    case LINK_ADDED:
                    case LINK_UPDATED:
                        updateNetworkLink(event.subject());
                        break;

                    case LINK_REMOVED:
                        removeNetworkLink(event.subject());
                        break;

                    default:
                        break;
                }
            } catch (Exception ex) {
                log.error("Error on link listener", ex);
            }
        }
    }

    private class InnerNetworkConfigEventListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            LinkKey linkKey = (LinkKey) event.subject();
            BasicLinkConfig basic = netCfgService.getConfig(linkKey, BasicLinkConfig.class);

            boolean allowed = basic == null || basic.isAllowed();
            boolean bidirectional = basic == null || basic.isBidirectional();

            log.debug("Network config event: src[{}] dst[{}] type[{}] allowed[{}] bidirectional[{}]",
                      linkKey.src(), linkKey.dst(), event.type(),
                      allowed, bidirectional);
            if (allowed) {
                // Switch threads at this point to process bi-directional
                // operations in sequentially.
                runOnWorkerThread(() -> {
                    updateNetworkLink(linkKey);
                    if (bidirectional) {
                        updateNetworkLink(LinkKey.linkKey(linkKey.dst(), linkKey.src()));
                    }
                });
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            // The timing of Link add and NetCfg add overlap, but there are cases
            // where they are not simultaneous, such as adding only oms-link after
            // registering a link, so listen to both and update them.
            return ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED) ||
                    (event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) ||
                    (event.type() == NetworkConfigEvent.Type.CONFIG_REMOVED)) &&
                    event.configClass() == OmsLinkConfig.class;
        }
    }

    private class InnerWdmPathEventListener implements WdmPathEventListener {
        @Override
        public void event(WdmPathEvent event) {
            log.debug("WDM path event: type[{}] count[{}]",
                    event.type(), event.subject().size());
            try {
                switch (event.type()) {
                    case PATHS_REPLACED:
                    case PATHS_CLEARED:
                        replaceWdmPath(event.subject(), event.getRemoved());
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                log.error("Error on WDM path listener", ex);
            }
        }
    }

    private class InnerWavelengthPathEventListener implements WavelengthPathEventListener {
        @Override
        public void event(WavelengthPathEvent event) {
            log.debug("Wavelength path event: type[{}] id[{}]",
                    event.type(), event.subject().id());
            try {
                switch (event.type()) {
                    case PATH_ADDED:
                    case PATH_UPDATED:
                        updateWavelengthPath(event.subject(), event.coupledServices());
                        break;
                    case PATH_REMOVED:
                        removeWavelengthPath(event.subject());
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                log.error("Error on Wavelength path listener", ex);
            }
        }
    }

    private void runOnWorkerThread(Runnable runnable) {
        if (workerThreadId == Thread.currentThread().getId()) {
            try {
                runnable.run();
            } catch (Exception ex) {
                log.error("Exception occurred", ex);
            }
        } else {
            executor.submit(() -> {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    log.error("Exception occurred", ex);
                }
            });
        }
    }
}
