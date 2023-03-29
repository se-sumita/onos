package org.onosproject.opticalpathoptimizer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerTracker;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultOchSignalComparator;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.openroadm.behaviors.RoadmPortUtils;
import org.onosproject.net.openroadm.behaviors.TransponderCapabilityQuery;
import org.onosproject.net.openroadm.intent.WavelengthPathIntent;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.net.openroadm.model.OsnrMap;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.net.openroadm.model.WdmPath;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.api.OpticalPathOptimizerService;
import org.onosproject.opticalpathoptimizer.api.QualityCalculationPropertyService;
import org.onosproject.opticalpathoptimizer.model.FiberSpanWeigher;
import org.onosproject.opticalpathoptimizer.model.QValue;
import org.onosproject.opticalpathoptimizer.model.QWithParams;
import org.onosproject.opticalpathoptimizer.util.CalculationFailedException;
import org.onosproject.opticalpathoptimizer.util.ExternalOsnrCalculationInvoker;
import org.onosproject.opticalpathoptimizer.util.FrequencyConverter;
import org.onosproject.opticalpathoptimizer.util.OmsLinkChangeDetector;
import org.onosproject.opticalpathoptimizer.util.OsnrCalculationException;
import org.onosproject.opticalpathoptimizer.util.PathOsnrMap;
import org.onosproject.opticalpathoptimizer.util.PropertyAccessor;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.opticalpathstore.WdmPathStore;
import org.onosproject.store.service.ConsistentMapException;
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
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.*;
import static java.util.Comparator.comparing;
import static org.onosproject.opticalpathoptimizer.OsgiPropertyConstants.*;


/**
 * Implementation of optimal path calculation and quality calculation processes.
 */
@Component(
        immediate = true,
        service = { OpticalPathOptimizerService.class },
        property = {
                K + ":Integer=" + DEFAULT_K,
                LOWEST_FREQ_THZ_FOR_ID + ":Double=" + DEFAULT_LOWEST_FREQ_THZ_FOR_ID,
        }
)
public class OpticalPathOptimizer implements OpticalPathOptimizerService {

    private final Logger log = LoggerFactory.getLogger(OpticalPathOptimizer.class);

    /*------------------------------------------------------------------------*
     * Configuration
     *------------------------------------------------------------------------*/

    /** K for finding shortest path. */
    @SuppressWarnings("checkstyle:MemberName")
    private int K = DEFAULT_K;

    /** Lowest frequency value(THz) for calculating frequency ID. */
    private double lowestFrequencyTHzForCalcId = DEFAULT_LOWEST_FREQ_THZ_FOR_ID;

    /*------------------------------------------------------------------------*
     * Topology & Resource
     *------------------------------------------------------------------------*/
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ResourceService resourceService;

    private ApplicationId appId;

    private ListenerTracker listeners;

    private OmsLinkChangeDetector omsLinkChangeDetector = new OmsLinkChangeDetector();

    /*------------------------------------------------------------------------*
     * Store
     *------------------------------------------------------------------------*/
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private WdmPathStore wdmPathStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private WavelengthPathStore wavelengthPathStore;

    private List<WavelengthPathCandidate> wavelengthPathCandidates = new LinkedList<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected QualityCalculationPropertyService calculateProperties;

    /*------------------------------------------------------------------------*
     * Intent
     *------------------------------------------------------------------------*/
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private IntentService intentService;

    /**
     * Intent Listener.
     *
     * Log output as Intent status changes.
     */
    public class InternalIntentListener implements IntentListener {
        @Override
        public boolean isRelevant(IntentEvent event) {
            return event.subject() instanceof WavelengthPathIntent;
        }

        @Override
        public void event(IntentEvent event) {
            Intent intent = event.subject();
            switch (event.type()) {
                case INSTALL_REQ:
                    log.info("[WLPATH INTENT] intent[{}]: INSTALL REQUESTED", intent.key());
                    break;
                case INSTALLED:
                    log.info("[WLPATH INTENT] intent[{}]: INSTALLED", intent.key());
                    break;
                case WITHDRAW_REQ:
                    log.info("[WLPATH INTENT] intent[{}]: WITHDRAW REQUESTED", intent.key());
                    break;
                case WITHDRAWN:
                    WavelengthPathIntent pathIntent = (WavelengthPathIntent) intent;
                    List<WavelengthPath> paths = pathIntent.pathIds().stream()
                            .map(wavelengthPathStore::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    releaseWavelengthPathGroupResource(paths, false);
                    // Resources are released by IntentManager
                    log.info("[WLPATH INTENT] intent[{}]: WITHDRAWN", intent.key());
                    break;
                case FAILED:
                    log.warn("[WLPATH INTENT] intent[{}]: FAILED", intent.key());
                    break;
                case CORRUPT:
                    log.warn("[WLPATH INTENT] intent[{}]: CORRUPT", intent.key());
                    break;
                case PURGED:
                    log.debug("[WLPATH INTENT] intent[{}]: PURGED", intent.key());
                    break;
                case REALLOCATING:
                    log.info("[WLPATH INTENT] intent[{}]: REALLOCATING", intent.key());
                    break;
                default:
                    break;
            }
        }
    }

    /*------------------------------------------------------------------------*
     * Activate/Deactivate/Modified
     *------------------------------------------------------------------------*/
    @Activate
    protected void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);

        appId = coreService.registerApplication("org.onosproject.opticalpathoptimizer");

        log.info("Started");

        /* Register an IntentListener */
        listeners = new ListenerTracker();
        listeners.addListener(intentService, new InternalIntentListener());

        /* Activate topology change detection */
        omsLinkChangeDetector.activate();

        /* Perform WDM path calculation */
        try {
            calculateWdmPaths(null, null);
        } catch (CommandFailedException ex) {
            log.warn("Calculate WDM path failed. {}", ex.getMessage());
        }
    }

    @Deactivate
    protected void deactivate() {
        listeners.removeListeners();

        wavelengthPathCandidates.clear();

        omsLinkChangeDetector.deactivate();

        cfgService.unregisterProperties(getClass(), false);

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        PropertyAccessor accessor = new PropertyAccessor(context);

        // K parameter for K-shortest path search
        K = Tools.getIntegerProperty(properties, OsgiPropertyConstants.K, DEFAULT_K);
        if (K <= 0) {
            log.error("Parameter 'K' must be >0 ({})", K);
            K = DEFAULT_K;
        }
        log.debug("K: {}", K);

        // Minimum frequency for channel ID calculation
        double lowestFrequency = accessor.getAsDouble(
                LOWEST_FREQ_THZ_FOR_ID,
                DEFAULT_LOWEST_FREQ_THZ_FOR_ID);
        if (MIN_LOWEST_FREQ_THZ_FOR_ID <= lowestFrequency) {
            log.debug("{}: {}", LOWEST_FREQ_THZ_FOR_ID, lowestFrequency);
            lowestFrequencyTHzForCalcId = lowestFrequency;
        } else {
            log.error("Parameter '{}' must be >{}",
                      LOWEST_FREQ_THZ_FOR_ID,
                      MIN_LOWEST_FREQ_THZ_FOR_ID);
            // Restore the modified settings
            cfgService.setProperty(
                    getClass().getCanonicalName(),
                    LOWEST_FREQ_THZ_FOR_ID,
                    String.valueOf(lowestFrequencyTHzForCalcId));
        }
    }

    /*------------------------------------------------------------------------*
     * WDM Path
     *------------------------------------------------------------------------*/

    /**
     * Get whether the topology has changed since the last WDM path calculation.
     * @return true if changed, false otherwise
     */
    public boolean getWdmCalcNecessary() {
        return omsLinkChangeDetector.isChanged();
    }

    /**
     * Get OMS Add/Drop ports.
     * @return Add/Drop ports
     */
    private ImmutableMap<DeviceId, List<ConnectPoint>> getOmsAddDropPorts() {
        /* Get devices whose Device type is ROADM or ROADM_OTN. */
        List<Device> devices = Lists.newArrayList(deviceService.getDevices(Device.Type.ROADM));
        devices.addAll(Lists.newArrayList(deviceService.getDevices(Device.Type.ROADM_OTN)));

        /* Get OMS Add/Drop ports */
        Map<DeviceId, List<ConnectPoint>> portMap = Maps.newLinkedHashMap();
        for (Device device : devices) {
            DeviceId deviceId = device.id();
            portMap.put(deviceId,
                        RoadmPortUtils.getOmsAddDropPorts(device).stream()
                            .map(p -> new ConnectPoint(deviceId, p.number()))
                            .collect(Collectors.toList()));
        }
        return ImmutableMap.copyOf(portMap);
    }

    /**
     * Calculate WDM path (between OMS-Add/Drop ports).
     * @param ingress ingress port
     * @param egress  egress port
     */
    public void calculateWdmPaths(ConnectPoint ingress, ConnectPoint egress) {
        log.info("Start to calculate WDM paths.");

        if (deviceService.getDeviceCount() == 0) {
            throw new CommandFailedException("No devices.");
        }

        /* Get OMS-AddDrop ports */
        ImmutableMap<DeviceId, List<ConnectPoint>> addPorts = getOmsAddDropPorts();
        ImmutableMap<DeviceId, List<ConnectPoint>> dropPorts = addPorts;
        if (addPorts.isEmpty()) {
            throw new CommandFailedException("OMS Add/Drop port not found.");
        }

        /* If ingress is specified and is an OMS-AddDrop port, replace addPorts with the given one. */
        if (ingress != null) {
            checkOmsAddDropPort(deviceService, ingress,
                    "Connect point must be OMS Add/Drop port. " + ingress.toString());
            addPorts = ImmutableMap.of(ingress.deviceId(), Lists.newArrayList(ingress));
        }

        /* If egress is specified and is an OMS-AddDrop port, replace dropPorts with the given one. */
        if (egress != null) {
            checkOmsAddDropPort(deviceService, egress,
                    "Connect point must be OMS Add/Drop port. " + egress.toString());
            dropPorts = ImmutableMap.of(egress.deviceId(), Lists.newArrayList(egress));
        }

        List<WdmPath> wdmPaths = calculateWdmPathsInternal(addPorts, dropPorts);
        log.debug("WDM path [{}]", wdmPaths.size());

        /* Save wdmPath to the store */
        try {
            wdmPathStore.replace(ingress, egress, wdmPaths);
        } catch (ConsistentMapException.Timeout ex) {
            throw new CommandFailedException(
                    "Timeout occurred in accessing WDM path to the store. Please try again later.", ex);
        }

        log.info("Finished to calculate WDM paths.");
    }

    private List<WdmPath> calculateWdmPathsInternal(
            ImmutableMap<DeviceId, List<ConnectPoint>> addPorts,
            ImmutableMap<DeviceId, List<ConnectPoint>> dropPorts) {

        /* Reset the change state in the topology at the time of WDM path calculation */
        omsLinkChangeDetector.reset();

        /* Get the rate and modulation format */
        Collection<OchParam> rateModForms = calculateProperties.getRateAndModulationFormatForCalcOsnr();

        List<WdmPath> wdmPaths;
        if (METHOD_GNPY.equals(calculateProperties.getMethod())) {
            // Use external commands to perform quality calculations (OSNR calculations)
            wdmPaths = calculateWdmPathsWithExternalCommand(addPorts, dropPorts, rateModForms);
        } else {
            // Perform quality calculations in the internal implementation
            wdmPaths = calculateWdmPathsWithQualityCalculation(addPorts, dropPorts, rateModForms);
        }

        return wdmPaths;
    }

    private List<WdmPath> calculateWdmPathsWithExternalCommand(
            Map<DeviceId, List<ConnectPoint>> addPorts,
            Map<DeviceId, List<ConnectPoint>> dropPorts,
            Collection<OchParam> rateModForms) {

        OpenRoadmConfigHelper openRoadmConfigHelper = OpenRoadmConfigHelper.create();

        /* Weigher for calculating the span length between ROADMs */
        LinkWeigher weigher = new FiberSpanWeigher(deviceService, openRoadmConfigHelper);
        Topology topology = topologyService.currentTopology();

        // (src device, dst device) => (path, (OSNR of forward direction, OSNR of reverse direction))
        Map<Pair<DeviceId, DeviceId>, List<PathOsnrMap>> pathOsnrMap = Maps.newLinkedHashMap();

        Set<Set<DeviceId>> calculated = Sets.newHashSet();

        /* When calculating one side, the opposite direction is also calculated. */
        for (Map.Entry<DeviceId, List<ConnectPoint>> srcPort : addPorts.entrySet()) {
            for (Map.Entry<DeviceId, List<ConnectPoint>> dstPort : dropPorts.entrySet()) {
                if ((srcPort.getKey().equals(dstPort.getKey()))) {
                    continue;
                }

                DeviceId srcDevice = srcPort.getKey();
                DeviceId dstDevice = dstPort.getKey();

                Set<DeviceId> target = ImmutableSet.of(srcDevice, dstDevice);
                if (calculated.contains(target)) {
                    // To calculate bidirectionally, skip pairs that have
                    // already been calculated.
                    continue;
                }
                calculated.add(target);

                /* Search the K-shortest path between devices */
                List<Path> paths1 = Lists.newArrayList(
                        topologyService.getKShortestPaths(
                                topology, srcDevice, dstDevice, weigher, K)
                );

                // forward direction
                List<PathOsnrMap> paths = paths1.stream()
                        .map(PathOsnrMap::of)
                        .collect(Collectors.toList());
                pathOsnrMap.put(Pair.of(srcDevice, dstDevice), paths);

                // reverse direction
                paths = paths1.stream()
                        .map(this::reversePath)
                        .map(PathOsnrMap::of)
                        .collect(Collectors.toList());
                pathOsnrMap.put(Pair.of(dstDevice, srcDevice), paths);
            }
        }

        // ---------------------------------------------------------------------
        // Invoke an external command
        // ---------------------------------------------------------------------
        List<PathOsnrMap> exportPaths = pathOsnrMap.values()
                .stream().flatMap(List::stream)
                .collect(Collectors.toList());

        ExternalOsnrCalculationInvoker invoker = new ExternalOsnrCalculationInvoker();
        try {
            invoker.invoke(
                    exportPaths,
                    this.calculateProperties.getWorkingDirectory(),
                    this.calculateProperties.getCommand(),
                    calculateProperties
            );
        } catch (OsnrCalculationException e) {
            log.error("Invoke OSNR calculation failed.", e);
            throw new CalculationFailedException("Failed to run OSNR calculation command.", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("=================================================");
            for (PathOsnrMap map : exportPaths) {
                log.debug("{} => {}", map.getPath(), map.getOsnrMap());
            }
        }

        // ---------------------------------------------------------------------
        // Create a WDM path
        // ---------------------------------------------------------------------
        List<WdmPath> wdmPaths = Lists.newArrayList();
        calculated.clear();

        for (Map.Entry<DeviceId, List<ConnectPoint>> srcPort : addPorts.entrySet()) {
            for (Map.Entry<DeviceId, List<ConnectPoint>> dstPort : dropPorts.entrySet()) {
                if ((srcPort.getKey().equals(dstPort.getKey()))) {
                    continue;
                }

                DeviceId srcDevice = srcPort.getKey();
                DeviceId dstDevice = dstPort.getKey();
                Set<DeviceId> devices = ImmutableSet.of(srcDevice, dstDevice);
                if (calculated.contains(devices)) {
                    // To calculate bidirectionally, skip pairs that have
                    // already been calculated.
                    continue;
                }
                calculated.add(devices);

                // forward direction
                Pair<DeviceId, DeviceId> pair = Pair.of(srcDevice, dstDevice);
                List<PathOsnrMap> pathList = pathOsnrMap.get(pair);

                for (PathOsnrMap path : pathList) {
                    // Create and add WDM path objects
                    for (ConnectPoint ap : srcPort.getValue()) {
                        for (ConnectPoint dp : dstPort.getValue()) {
                            log.trace("register wdm-path {}->{}", ap, dp);
                            wdmPaths.add(new WdmPath(ap, dp, path.getPath(), path.getOsnrMap()));
                        }
                    }
                }

                // reverse direction
                pair = Pair.of(dstDevice, srcDevice);
                pathList = pathOsnrMap.get(pair);

                for (PathOsnrMap path : pathList) {
                    // Create and add WDM path objects
                    for (ConnectPoint ap : srcPort.getValue()) {
                        for (ConnectPoint dp : dstPort.getValue()) {
                            log.trace("register wdm-path {}->{}", dp, ap);
                            wdmPaths.add(new WdmPath(dp, ap, path.getPath(), path.getOsnrMap()));
                        }
                    }
                }
            }
        }
        return wdmPaths;
    }

    private List<WdmPath> calculateWdmPathsWithQualityCalculation(
            Map<DeviceId,
                    List<ConnectPoint>> addPorts, Map<DeviceId, List<ConnectPoint>> dropPorts,
            Collection<OchParam> rateModForms) {
        OpenRoadmConfigHelper openRoadmConfigHelper = OpenRoadmConfigHelper.create();
        QualityCalculator qualityCalculator = QualityCalculator.create(openRoadmConfigHelper);

        Set<Set<DeviceId>> calculated = Sets.newHashSet();

        /* Weigher for calculating the span length between ROADMs */
        LinkWeigher weigher = new FiberSpanWeigher(deviceService, openRoadmConfigHelper);
        Topology topology = topologyService.currentTopology();

        List<WdmPath> wdmPaths = Lists.newArrayList();

        /* When calculating one side, the opposite direction is also calculated. */
        for (Map.Entry<DeviceId, List<ConnectPoint>> srcPort : addPorts.entrySet()) {
            for (Map.Entry<DeviceId, List<ConnectPoint>> dstPort : dropPorts.entrySet()) {
                if ((srcPort.getKey().equals(dstPort.getKey()))) {
                    continue;
                }

                DeviceId srcDevice = srcPort.getKey();
                DeviceId dstDevice = dstPort.getKey();
                Set<DeviceId> devices = ImmutableSet.of(srcDevice, dstDevice);
                if (calculated.contains(devices)) {
                    // To calculate bidirectionally, skip pairs that have
                    // already been calculated.
                    continue;
                }
                calculated.add(devices);

                /* Search the K-shortest path between devices */
                List<Path> paths1 = topologyService.getKShortestPaths(
                        topology, srcDevice, dstDevice, weigher)
                        .distinct()
                        .collect(Collectors.toList());

                Map<Set<ConnectPoint>, Integer> pathCount = Maps.newHashMap();
                for (Path path1 : paths1) {
                    List<ConnectPoint> srcPorts = srcPort.getValue();
                    List<ConnectPoint> dstPorts = dstPort.getValue();

                    // Create a reverse path
                    Path path2 = reversePath(path1);

                    // Calculate OSNR
                    OsnrMap osnrMap1 = calcPathOsnr(path1, rateModForms, qualityCalculator);
                    OsnrMap osnrMap2 = calcPathOsnr(path2, rateModForms, qualityCalculator);

                    // Create and add WDM path objects
                    for (ConnectPoint ap : srcPorts) {
                        for (ConnectPoint dp : dstPorts) {
                            // K upper limit check
                            Set<ConnectPoint> pathCountKey = ImmutableSet.of(ap, dp);
                            if (pathCount.getOrDefault(pathCountKey, 0) < K) {
                                // increment
                                pathCount.merge(pathCountKey, 1, Integer::sum);
                                log.trace("register wdm-path {}->{}", ap, dp);
                                wdmPaths.add(new WdmPath(ap, dp, path1, osnrMap1));
                                wdmPaths.add(new WdmPath(dp, ap, path2, osnrMap2));
                            } else {
                                log.trace("ignore K =< {} {}->{}",
                                          pathCount.getOrDefault(pathCountKey, 0), ap, dp);
                            }
                        }
                    }
                }
            }
        }
        return wdmPaths;
    }

    private OsnrMap calcPathOsnr(Path path, Collection<OchParam> rateModForms, QualityCalculator calculator) {
        OsnrMap osnrMap = new OsnrMap();
        for (OchParam param : rateModForms) {
            double osnr = calculator.calcTotalOSNR(path, param.rate(), param.modulationFormat());
            osnrMap.put(param, osnr);
        }
        return osnrMap;
    }

    private Path reversePath(Path path) {
        List<Link> links = Lists.reverse(path.links()).stream()
                .map(l -> DefaultLink.builder()
                        .providerId(l.providerId())
                        .src(l.dst()).dst(l.src())
                        .type(l.type()).state(l.state())
                        .annotations(l.annotations())
                        .build())
                .collect(Collectors.toList());
        return new DefaultPath(path.providerId(), links, path.weight(), path.annotations());
    }

    /*------------------------------------------------------------------------*
     * Wavelength Path
     *------------------------------------------------------------------------*/

    /**
     * Calculate the wavelength path (between ports of OCh).
     * When multiple paired ports are specified, search disjoint paths.
     *
     * @param portPairs list of ingress/egress pairs
     */
    public void calculateWavelengthPaths(List<Pair<ConnectPoint, ConnectPoint>> portPairs) {
        checkNotNull(portPairs);
        checkArgument(portPairs.size() >= 1);
        checkArgument(portPairs.size() <= 2);
        log.info("Start to calculate wavelength paths. ports={}", portPairs);

        // Clear the temporary calculation holding area
        wavelengthPathCandidates.clear();

        Iterator<Pair<ConnectPoint, ConnectPoint>> it = portPairs.iterator();
        Pair<ConnectPoint, ConnectPoint> firstPair = it.next();
        Pair<ConnectPoint, ConnectPoint> secondPair = null;
        if (it.hasNext()) {
            secondPair = it.next();
        }

        Map<Pair<ConnectPoint, ConnectPoint>, List<WavelengthPathCandidate>> candidatePaths
                = Maps.newHashMap();
        for (Pair<ConnectPoint, ConnectPoint> portPair : portPairs) {
            ConnectPoint ingress = portPair.getLeft();
            ConnectPoint egress = portPair.getRight();

            /* Get the port that is connected to the specified port */
            Set<Link> srcLinks = linkService.getEgressLinks(ingress).stream()
                    .filter(p -> p.state() == Link.State.ACTIVE).collect(Collectors.toSet());
            Set<Link> dstLinks = linkService.getIngressLinks(egress).stream()
                    .filter(p -> p.state() == Link.State.ACTIVE).collect(Collectors.toSet());
            if (srcLinks.size() != 1) {
                throw new IllegalArgumentException("specified port does not have a link. " + ingress);
            }
            if (dstLinks.size() != 1) {
                throw new IllegalArgumentException("specified port does not have a link. " + egress);
            }
            Link srcEdge = srcLinks.iterator().next();
            Link dstEdge = dstLinks.iterator().next();

            // Keep a single path candidate
            candidatePaths.put(portPair,
                    calcWavelengthPathSingle(srcEdge, dstEdge));
        }

        // Get the disjoint paths
        List<WavelengthPathCandidate> paths = candidatePaths.get(firstPair);
        if (secondPair != null) {
            List<WavelengthPathCandidate> list = Lists.newArrayList();
            List<WavelengthPathCandidate> pairedPaths = candidatePaths.get(secondPair);
            for (WavelengthPathCandidate main : paths) {
                for (WavelengthPathCandidate trib : pairedPaths) {
                    if (WavelengthPathCandidate.isDisjoint(main.getMainPath(), trib.getMainPath())) {
                        WavelengthPathCandidate c = new WavelengthPathCandidate();
                        c.addPath(main.getMainPath());
                        c.addPath(trib.getMainPath());
                        list.add(c);
                    }
                }
            }
            // Sort by Q-value
            list.sort(
                    Comparator
                            .comparingDouble((WavelengthPathCandidate a) -> a.getMainPath().qValue().qValue())
                            .thenComparingDouble(a -> a.getPaths().get(1).qValue().qValue())
            );
            wavelengthPathCandidates.addAll(list);
        } else {
            // Sort by Q-value
            paths.sort(Comparator.comparingDouble(a -> a.getMainPath().qValue().qValue()));
            wavelengthPathCandidates.addAll(paths);
        }

        // Reset the link change state in case it is executed with a forced flag.
        // (No notification of topology change next time)
        omsLinkChangeDetector.reset();

        log.info("Finished to calculate wavelength paths. ports={}", portPairs);
    }

    private OchParam getHighestRate(Collection<OchParam> params) {
        checkArgument(params.size() > 0);
        List<OchParam> ordered = params.stream()
                .sorted(comparing(e -> calculateProperties.getRateValue(e.rate())))
                .collect(Collectors.toList());
        return Iterables.getLast(ordered);
    }

    private ConnectPoint findConnectedPort(ConnectPoint point, boolean findDestinationSide) {
        if (findDestinationSide) {
            Set<Link> srcLinks = linkService.getEgressLinks(point);
            if (srcLinks.size() != 1) {
                throw new IllegalArgumentException("specified port does not have a link. " + point);
            }
            Link srcEdge = srcLinks.iterator().next();
            return srcEdge.dst();
        } else {
            Set<Link> dstLinks = linkService.getIngressLinks(point);
            if (dstLinks.size() != 1) {
                throw new IllegalArgumentException("specified port does not have a link. " + point);
            }
            Link dstEdge = dstLinks.iterator().next();
            return dstEdge.src();
        }
    }

    /**
     * Wavelength path calculation (without redundant paths).
     * @param srcLink ingress Link
     * @param dstLink egress Link
     * @return true if successful, false otherwise
     */
    private List<WavelengthPathCandidate> calcWavelengthPathSingle(Link srcLink, Link dstLink) {
        List<WavelengthPathCandidate> list = new LinkedList<>();

        ConnectPoint ingressPort = srcLink.dst();
        ConnectPoint egressPort = dstLink.src();

        /* Check resources for the specified OCh and OMS Add/Drop ports */
        List<ConnectPoint> points = Stream.of(srcLink, dstLink)
                .flatMap(l -> Stream.of(l.src(), l.dst()))
                .collect(Collectors.toList());
        for (ConnectPoint point : points) {
            Resource resource = Resources.discrete(point.deviceId(), point.port()).resource();
            if (!resourceService.isAvailable(resource)) {
                Port port = deviceService.getPort(point);
                point = new ConnectPoint(point.deviceId(), port.number());
                throw new IllegalArgumentException(
                        "Specified port is already used. " + point.toString());
            }
        }

        /* Get WDM paths */
        Collection<WdmPath> wdmPaths = wdmPathStore.getPaths(ingressPort, egressPort);

        FrequencyConverter converter = new FrequencyConverter(lowestFrequencyTHzForCalcId);

        QualityCalculator qualityCalculator = QualityCalculator.create();
        for (WdmPath wdmPath : wdmPaths) {
            log.debug("WavelengthPath inner wdmPath = {}", wdmPath.toString());

            // Get the WDM path in the reverse direction
            WdmPath reversePath = wdmPathStore.getReversePath(wdmPath);
            if (reversePath == null) {
                log.warn("Not found reverse path: {}", wdmPath);
                continue;
            }

            /* Get the assignable lambdas */
            Set<OchSignal> lambdas = findCommonLambdas(wdmPath.path());
            if (lambdas.size() == 0) {
                log.warn("Could not find assignable lambda. {}", wdmPath);
                continue;
            }

            List<OchSignal> signals = fromResourceGrid(
                    lambdas.stream().sorted(new DefaultOchSignalComparator()).collect(Collectors.toList()),
                    ChannelSpacing.CHL_50GHZ);
            log.debug("WavelengthPath available lambdas count: {}", signals.size());

            /* Get the OSNR value and calculate the Q-value */
            Device device = deviceService.getDevice(dstLink.dst().deviceId());

            /* Calculate the Q-value based on the rate/modulation format available in the vendor */
            QWithParams qParams = calculateQValues(qualityCalculator, device, wdmPath.osnr());
            if (qParams == null || qParams.isEmpty()) {
                log.warn("Q candidates are empty.");
                continue;
            }
            QWithParams qParams2 = calculateQValues(qualityCalculator, device, reversePath.osnr());
            if (qParams2 == null || qParams2.isEmpty()) {
                log.warn("Reverse Q candidates are empty.");
                continue;
            }
            qParams = mergeQWithParams(qParams, qParams2);

            /* Select the highest rate */
            OchParam highest = getHighestRate(qParams.keySet());

            /* store */
            WavelengthPathCandidate wlc = new WavelengthPathCandidate();
            Map<Integer, OchSignal> signalMap = Maps.newTreeMap();
            for (OchSignal signal : signals) {
                int id = converter.channelId(signal);
                if (id <= 0) {
                    continue;
                }
                signalMap.put(id, signal);
            }
            wlc.addPath(srcLink, dstLink, wdmPath.path(), signalMap, highest, qParams.get(highest));
            log.debug("{}", wlc);
            list.add(wlc);
        }
        return list;
    }

    /**
     * Merge the two Q-value candidates.
     *
     * For rate/modulation format, perform a product operation.
     * For the same rate/modulation format, select a lower Q-value.
     * @param params1 List of Q-value candidates 1
     * @param params2 List of Q-value candidates 2
     * @return merged Q-value candidates
     */
    private QWithParams mergeQWithParams(QWithParams params1, QWithParams params2) {
        Set<OchParam> keys = Sets.newLinkedHashSet();
        keys.addAll(params1.keySet());
        keys.retainAll(params2.keySet());
        return new QWithParams(
            keys.stream()
                    .collect(Collectors.toMap(
                            k -> k,
                            k -> params1.get(k).qValue() < params2.get(k).qValue()
                                    ? params1.get(k) : params2.get(k)
                    ))
        );
    }

    /**
     * Calculate the Q-value and check the threshold value.
     * @param calculator calculator
     * @param device end device
     * @param osnrMap OSNR map
     * @return calculated Q-value if a valid Q-value was calculated, otherwise null
     */
    private QWithParams calculateQValues(QualityCalculator calculator, Device device, OsnrMap osnrMap) {
        checkNotNull(device);
        checkNotNull(osnrMap);

        String vendor = device.manufacturer();

        // Get the rate and modulation format supported by the end device
        if (!device.is(TransponderCapabilityQuery.class)) {
            log.warn("Device [{}] do not have TransponderCapabilityQuery behavior.", device.id());
            return null;
        }
        TransponderCapabilityQuery query = device.as(TransponderCapabilityQuery.class);
        Set<Rate> rates = query.getSupportRates();
        Set<ModulationFormat> modFormats = query.getSupportedModulationFormats();

        QWithParams qValues = new QWithParams();

        // Calculate the Q-value from the OSNR values of the parameters that the device supports
        for (Map.Entry<OchParam, Double> osnr : osnrMap.entrySet()) {
            OchParam param = osnr.getKey();
            Rate rate = param.rate();
            if (!rates.contains(rate)) {
                log.debug("Device [{}] is not support rate[{}]", device.id(), rate);
                continue;
            }
            ModulationFormat modFormat = param.modulationFormat();
            if (!modFormats.contains(modFormat)) {
                log.debug("Device [{}] is not support modulation-format[{}]", device.id(), modFormat);
                continue;
            }

            String parameter = vendor + "/" + rate + "/" + modFormat;

            List<Double> constants = calculateProperties.getOsnrQConstants(vendor, rate, modFormat);
            if (constants == null) {
                log.warn("Not found OSNR -> Q constants. Device [{}] [{}]", device.id(), parameter);
                continue;
            }
            double threshold = calculateProperties.getQThreshold(vendor, rate, modFormat);
            if (Double.isNaN(threshold)) {
                log.warn("Not found Q threshold. Device [{}] [{}]", device.id(), parameter);
                continue;
            }
            double q = calculator.calcQ(constants, osnr.getValue());

            if (q < threshold) {
                log.warn("Q value rejected. Q [{}] < threshold [{}] [{}] Device [{}]",
                        q, threshold, parameter, device.id());
                continue;
            }
            log.debug(
                    "OSNR [" + osnr.getValue() + "] -> Q value [" + q + "] " +
                            "Device [" + device.id() + "] [" + rate + "/" + modFormat + "]");
            qValues.put(param, QValue.of(q, threshold));
        }
        return qValues;
    }

    /**
     * Reserve a wavelength path.
     * @param index index of the candidate wavelength path to be reserved
     * @param frequencyIds List of IDs of frequencies
     * @param names List of wavelength path names
     * @return List of reserved wavelength paths
     */
    public List<WavelengthPath> reserveWavelengthPath(int index, List<Integer> frequencyIds,
                                                      List<String> names) {
        if (wavelengthPathCandidates.size() == 0) {
            throw new IllegalArgumentException("No wavelength-path-calc results");
        }
        checkElementIndex(index, wavelengthPathCandidates.size(), "Invalid index value");
        checkArgument(!frequencyIds.isEmpty(), "Invalid frequency id value");
        checkArgument(frequencyIds.size() >= names.size(),
                      "Number of frequency IDs < Number of names");

        log.info("Start to reserve wavelength path(s). index[{}] freq ID{} names{}",
                index, frequencyIds.toString(), names);

        WavelengthPathCandidate candidate = wavelengthPathCandidates.get(index);
        checkNotNull(candidate);

        if (candidate.getPaths().size() != frequencyIds.size()) {
            // The number of routes and the number of frequency IDs must be the same
            throw new IllegalArgumentException(
                    "Please specify frequency id for each wavelength path. ");
        }

        if (candidate.getPaths().size() > 2) {
            throw new IllegalArgumentException("redundancy > 2 is not implemented.");
        }

        List<Resource> resources = Lists.newArrayList();

        Streams.forEachPair(candidate.getPaths().stream(), frequencyIds.stream(), (wlpath, frequencyId) -> {
            List<ConnectPoint> ports = ImmutableList.of(
                    wlpath.srcOch(), wlpath.dstOch(), wlpath.omsAddPort(), wlpath.omsDropPort()
            );

            /* If the port is disabled, an error occurs because resources cannot be allocated. */
            for (ConnectPoint cp : ports) {
                Port port = deviceService.getPort(cp);
                checkArgument(port != null && port.isEnabled(),
                        "Port is disabled. port=" + cp.toString());
            }

            /* Register resources for OCh, OMS Add/Drop ports */
            resources.addAll(
                ports.stream()
                    .map(cp -> Resources.discrete(cp.deviceId(), cp.port()).resource())
                    .collect(Collectors.toList()));

            /* Register a resource for a specified wavelength */
            OchSignal signal = wlpath.getLambda(frequencyId);
            if (signal == null) {
                throw new CommandFailedException("Invalid frequency ID (" + frequencyId + ").");
            }
            Set<OchSignal> signals = OchSignal.toFlexGrid(signal); // Convert for resources
            resources.addAll(convertToResources(wlpath.wdmPath(), signals));
        });

        long groupId = wavelengthPathStore.issueGroupId();
        Key intentKey = Key.of(groupId, appId);

        // Release of intent resources here is only a temporary solution for handling the
        // case of recompiling due to intent restoration (when intent state is FAILED).
        // TODO: try to release intent resources in IntentManager.
        releaseResource(intentKey);

        if (!allocateResources(intentKey, resources)) {
            wavelengthPathStore.releaseGroupIdIfPossible(groupId);
            throw new CommandFailedException("Lambda/Port resources are not available.");
        }

        Iterator<String> nameIterator = names.iterator();

        /* Create path */
        List<WavelengthPath> wlPathList = Streams.zip(candidate.getPaths().stream(), frequencyIds.stream(),
                                                      (wlpath, frequencyId) -> {
                    OchSignal signal = wlpath.getLambda(frequencyId);
                    String name = nameIterator.hasNext() ? nameIterator.next() : null;
                    return wavelengthPathStore.build(
                            groupId,
                            wlpath.ingressEdge(), wlpath.egressEdge(),
                            wlpath.wdmPath(), frequencyId, signal,
                            wlpath.ochParam().rate(), wlpath.ochParam().modulationFormat(),
                            wlpath.qValue().qValue(), wlpath.qValue().qThreshold(),
                            Strings.nullToEmpty(name));
                }).collect(Collectors.toList());

        /* Tying and storing disjoint paths */
        wavelengthPathStore.addAll(wlPathList);

        log.info("Finished to reserve wavelength path(s). {}", wlPathList.toString());

        return wlPathList;
    }

    /**
     * Submit the wavelength path.
     *
     * @param submitId Submit ID of the wavelength path to register
     */
    public void submitWavelengthPath(long submitId) {
        log.info("Start to submit wavelength path(s). submitId={}", submitId);

        List<WavelengthPath> paths = wavelengthPathStore.findByGroupId(submitId);
        checkArgument(!paths.isEmpty(), "Not found wavelength path. Submit ID=" + submitId);

        boolean submitted = paths.stream().anyMatch(WavelengthPath::isSubmitted);
        checkArgument(!submitted, "Wavelength path is already submitted.");

        Key intentKey = Key.of(paths.get(0).groupId(), appId);
        WavelengthPathIntent intent = WavelengthPathIntent.builder()
                .addWavelengthPaths(paths)
                .appId(appId)
                .key(intentKey)
                .build();

        /* Submit to IntentService */
        intentService.submit(intent);

        /* Update to submitted status */
        for (WavelengthPath path : paths) {
            WavelengthPath updated = path.cloneAsSubmitted();
            wavelengthPathStore.update(updated);
        }

        log.info("Finished to submit wavelength path(s). submitId={}", submitId);
    }

    /**
     * Remove the wavelength path.
     *
     * @param submitId Submit ID of the wavelength path to be removed.
     */
    public void removeWavelengthPath(long submitId) {
        log.info("Start to remove wavelength path(s). submitId={}", submitId);

        List<WavelengthPath> paths = wavelengthPathStore.findByGroupId(submitId);
        checkArgument(!paths.isEmpty(), "Not found wavelength path. Submit ID=" + submitId);

        WavelengthPath path = paths.get(0);
        Key intentKey = Key.of(path.groupId(), appId);
        Intent intent = intentService.getIntent(intentKey);
        if (intent != null) {
            IntentState state = intentService.getIntentState(intentKey);
            checkState(state == IntentState.INSTALLED,
                    "Cannot be removed. Intent state is not INSTALLED. [%s]", state.name());

            /* If the intent has already been registered, delete the intent as well */
            intentService.withdraw(intent);

            // Removal from the store is performed after Intent is removed.
            // Resources are released by IntentManager.
        } else {
            releaseWavelengthPathGroupResource(paths, true);
        }

        log.info("Finished to remove wavelength path(s). submitId={}", submitId);
    }

    private void releaseWavelengthPathGroupResource(List<WavelengthPath> paths, boolean releaseIntentResources) {
        if (paths.isEmpty()) {
            return;
        }

        if (releaseIntentResources) {
            WavelengthPath path = paths.get(0);
            Key intentKey = Key.of(path.groupId(), appId);
            releaseResource(intentKey);
            log.debug("Released resources. GroupID/IntentKey={}", path.groupId());
        }
        wavelengthPathStore.removeAll(paths);
    }

    /*------------------------------------------------------------------------*
     * Stores Getter
     *------------------------------------------------------------------------*/

    /**
     * Get the WDM path store.
     * @return WDM path store
     */
    public WdmPathStore getWdmPathStore() {
        return wdmPathStore;
    }

    /**
     * Get the results of wavelength path calculation (wavelength path candidates).
     * @return List of candidate wavelength paths
     */
    public List<WavelengthPathCandidate> getWavelengthPathCandidates() {
        return ImmutableList.copyOf(wavelengthPathCandidates);
    }

    /**
     * Get the wavelength path store.
     * @return wavelength path store
     */
    public WavelengthPathStore getWavelengthPathStore() {
        return wavelengthPathStore;
    }

    /*------------------------------------------------------------------------*
     * Utilities
     *------------------------------------------------------------------------*/

    private Pair<Device, Port> toDevicePort(ConnectPoint point) {
        return Pair.of(
                deviceService.getDevice(point.deviceId()),
                deviceService.getPort(point));
    }

    private boolean hasLambdaResource(Pair<Device, Port> dp) {
        boolean ret = dp.getRight().type() == Port.Type.OMS &&
            (
                dp.getLeft().type() == Device.Type.ROADM ||
                dp.getLeft().type() == Device.Type.ROADM_OTN
            );
        log.debug("ports={} hasLambdaResource={}", dp, ret);
        return ret;
    }

    // Copy from OpticalConnectivityIntentCompiler.findCommonLambdas()
    /**
     * Find common lambdas on all OMS ports that compose the path.
     *
     * @param path the path
     * @return set of common lambdas
     */
    private Set<OchSignal> findCommonLambdas(Path path) {
        return path.links().stream()
                .flatMap(link -> Stream.of(link.src(), link.dst()))
                .map(this::toDevicePort)
                .filter(this::hasLambdaResource)
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.getLeft().id(), x.getRight().number()).id()
                ))
                .map(x -> resourceService.getAvailableResourceValues(x, OchSignal.class))
                .map(x -> (Set<OchSignal>) ImmutableSet.copyOf(x))
                .peek(x -> log.debug("Find common lambdas: {}", x.toString()))
                .reduce(Sets::intersection)
                .orElse(Collections.emptySet());
    }

    /**
     * Convert given lambda as discrete resource of all path OMS ports.
     *
     * @param path the path
     * @param lambda the lambda
     * @return list of discrete resources
     */
    private List<Resource> convertToResources(Path path, Collection<OchSignal> lambda) {
        return path.links().stream()
                .flatMap(link -> Stream.of(link.src(), link.dst()))
                .map(this::toDevicePort)
                .filter(this::hasLambdaResource)
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.getLeft().id(), x.getRight().number()).resource()
                ))
                .flatMap(x -> lambda.stream().map(x::child))
                .collect(Collectors.toList());
    }

    /**
     * Reserve all required resources for this intent.
     *
     * @param key consumer key (ex: intent.key())
     * @param resources list of resources to reserve
     * @return success/fail
     */
    private boolean allocateResources(Key key, List<Resource> resources) {
        List<ResourceAllocation> allocations = resourceService.allocate(key, resources);
        if (allocations.isEmpty()) {
            log.error("Resource allocation for {} failed (resource request: {})", key, resources);
            if (log.isDebugEnabled()) {
                log.debug("requested resources:\n\t{}", resources.stream()
                        .map(Resource::toString)
                        .collect(Collectors.joining("\n\t")));
            }
            return false;
        }
        return true;
    }

    /**
     * Release all resources.
     *
     * @param key consumer key (ex: intent.key())
     */
    private void releaseResource(Key key) {
        resourceService.release(key);
    }

    /**
     * Grid for resources to Fixed grid.
     * @param from List of flex wavelengths
     * @param spacing frequency width
     * @return List of fixed wavelengths
     */
    public static List<OchSignal> fromResourceGrid(List<OchSignal> from, ChannelSpacing spacing) {
        int multiplier = (int) (spacing.frequency().asHz() / ChannelSpacing.CHL_12P5GHZ.frequency().asHz());
        List<OchSignal> results = Lists.newArrayList();
        for (int i = 0; i * multiplier + multiplier <= from.size(); i++) {
            List<OchSignal> sub = from.subList(i * multiplier, i * multiplier + multiplier);
            results.add(OchSignal.toFixedGrid(sub, spacing));
        }
        return results;
    }

    /**
     * Check OMS Add/Drop port.
     * @param deviceService device service
     * @param point connect point
     * @param errorMessage error message
     */
    public static void checkOmsAddDropPort(DeviceService deviceService, ConnectPoint point, String errorMessage) {
        Device device = deviceService.getDevice(point.deviceId());
        if (device.type() != Device.Type.ROADM && device.type() != Device.Type.ROADM_OTN) {
            throw new IllegalArgumentException(errorMessage);
        }
        if (!RoadmPortUtils.isOmsAddDropPort(device, point.port())) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Check OCh port.
     * @param deviceService device service
     * @param point connect point
     * @param errorMessage error message
     */
    public static void checkOchPort(DeviceService deviceService, ConnectPoint point, String errorMessage) {
        Device device = deviceService.getDevice(point.deviceId());
        if (device == null || device.type() == Device.Type.ROADM || device.type() == Device.Type.ROADM_OTN) {
            throw new IllegalArgumentException(errorMessage);
        }
        Port port = deviceService.getPort(point);
        if (port == null || port.type() != Port.Type.OCH) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
