package org.onosproject.opticalpathoptimizer.intent;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.openroadm.flow.instructions.TransponderInstruction;
import org.onosproject.net.openroadm.intent.WavelengthPathIntent;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * Wavelength path intent compiler.
 */
@Component(immediate = true)
public class WavelengthPathIntentCompiler implements IntentCompiler<WavelengthPathIntent> {

    protected static final Logger log = LoggerFactory.getLogger(WavelengthPathIntentCompiler.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    // Devices which are wavelength transparent and thus do not require wavelength-based match/actions
    private static final Set<Device.Type> TRANSPARENT_DEVICES =
            ImmutableSet.of(Device.Type.OPTICAL_AMPLIFIER, Device.Type.FIBER_SWITCH);
    // Devices which don't accept flow rules
    private static final Set<Device.Type> NO_FLOWRULE_DEVICES =
            ImmutableSet.of(Device.Type.OPTICAL_AMPLIFIER);

    @Activate
    public void activate() {
        deviceService = opticalView(deviceService);
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(WavelengthPathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(WavelengthPathIntent.class);
    }

    @Override
    public List<Intent> compile(WavelengthPathIntent intent, List<Intent> installable) {
        log.debug("Compiling wavelength path intent.");

        WavelengthPathStore store = getService(WavelengthPathStore.class);
        List<FlowRule> rules = new LinkedList<>();
        for (Long pathId : intent.pathIds()) {
            WavelengthPath path = store.get(pathId);
            checkNotNull(path);
            rules.addAll(createRules(intent, path));
            if (intent.isBidirectional()) {
                rules.addAll(createReverseRules(intent, path));
            }
        }
        return Collections.singletonList(
                new FlowRuleIntent(appId,
                                   intent.key(),
                                   rules,
                                   intent.resources(),
                                   PathIntent.ProtectionType.PRIMARY,
                                   intent.resourceGroup()
                )
        );
    }

    private List<FlowRule> createRules(WavelengthPathIntent intent, WavelengthPath path) {
        List<FlowRule> rules = new LinkedList<>();

        if (!isNoFlowRule(path.srcOch().deviceId())) {
            rules.add(createRulesForTransponder(intent, path, path.srcOch()));
        }
        rules.addAll(createRulesForWavelengthPath(intent, path));
        if (!isNoFlowRule(path.dstOch().deviceId())) {
            rules.add(createRulesForTransponder(intent, path, path.dstOch()));
        }

        return rules;
    }

    private List<FlowRule> createReverseRules(WavelengthPathIntent intent, WavelengthPath path) {
        List<FlowRule> rules = new LinkedList<>();
        rules.addAll(createReverseRulesForWavelengthPath(intent, path));
        return rules;
    }

    private List<FlowRule> createRulesForWavelengthPath(WavelengthPathIntent intent, WavelengthPath path) {
        List<FlowRule> rules = new LinkedList<>();

        /* Selector for OMS-AddPort */
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(onlyNumber(path.addPort().port()));

        ConnectPoint current = path.addPort();
        for (Link link : path.path().links()) {
            /* Treatment for OMS-Port */
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            if (!isTransparent(current.deviceId())) {
                treatmentBuilder.add(Instructions.modL0Lambda(path.signal()));
            }
            treatmentBuilder.setOutput(onlyNumber(link.src().port()));

            /* Create FlowRule */
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(current.deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();
            if (!isNoFlowRule(current.deviceId())) {
                rules.add(rule);
            }

            /* Reset selector */
            selectorBuilder = DefaultTrafficSelector.builder();

            /* Selector for OMS-Port */
            current = link.dst();
            selectorBuilder.matchInPort(onlyNumber(link.dst().port()));
            if (!isTransparent(current.deviceId())) {
                selectorBuilder.add(Criteria.matchLambda(path.signal()));
                selectorBuilder.add(Criteria.matchOchSignalType(intent.signalType()));
            }
        }

        /* Treatments for OMS-DropPort */
        TrafficTreatment.Builder treatmentLast = DefaultTrafficTreatment.builder();
        treatmentLast.setOutput(onlyNumber(path.dropPort().port()));

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(current.deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentLast.build())
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();
        if (!isNoFlowRule(current.deviceId())) {
            rules.add(rule);
        }

        return rules;
    }

    private List<FlowRule> createReverseRulesForWavelengthPath(WavelengthPathIntent intent, WavelengthPath path) {
        List<FlowRule> rules = new LinkedList<>();

        /* Selector for OMS-AddPort */
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(onlyNumber(path.dropPort().port()));

        ConnectPoint current = path.dropPort();
        for (Link link : Lists.reverse(path.path().links())) {
            /* Treatment for OMS-Port */
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            if (!isTransparent(current.deviceId())) {
                treatmentBuilder.add(Instructions.modL0Lambda(path.signal()));
            }
            treatmentBuilder.setOutput(onlyNumber(link.dst().port()));

            /* Create FlowRule */
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(current.deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();
            if (!isNoFlowRule(current.deviceId())) {
                rules.add(rule);
            }

            /* Reset selector */
            selectorBuilder = DefaultTrafficSelector.builder();

            /* Selector for OMS-Port */
            current = link.src();
            selectorBuilder.matchInPort(onlyNumber(link.src().port()));
            if (!isTransparent(current.deviceId())) {
                selectorBuilder.add(Criteria.matchLambda(path.signal()));
                selectorBuilder.add(Criteria.matchOchSignalType(intent.signalType()));
            }
        }

        /* Treatment for OMS-DropPort */
        TrafficTreatment.Builder treatmentLast = DefaultTrafficTreatment.builder();
        treatmentLast.setOutput(onlyNumber(path.addPort().port()));

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(current.deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentLast.build())
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();
        if (!isNoFlowRule(current.deviceId())) {
            rules.add(rule);
        }

        return rules;
    }

    private FlowRule createRulesForTransponder(
            WavelengthPathIntent intent, WavelengthPath path, ConnectPoint target) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        selectorBuilder
                .matchInPort(onlyNumber(target.port()))
                .add(Criteria.matchLambda(path.signal()))
                .add(Criteria.matchOchSignalType(intent.signalType()));
        treatmentBuilder
                .setOutput(onlyNumber(target.port()))
                .add(Instructions.modL0Lambda(path.signal()))
                .extension(
                        TransponderInstruction.of(path.rate(), path.modulationFormat()),
                        target.deviceId());
        return DefaultFlowRule.builder()
                .forDevice(target.deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();
    }

    /**
     * Returns true if device does not accept flow rules, false otherwise.
     *
     * @param deviceId the device
     * @return true if device does not accept flow rule, false otherwise
     */
    private boolean isNoFlowRule(DeviceId deviceId) {
        return NO_FLOWRULE_DEVICES.contains(
                Optional.ofNullable(deviceService.getDevice(deviceId))
                        .map(Device::type)
                        .orElse(Device.Type.OTHER));
    }

    /**
     * Returns true if device is wavelength transparent, false otherwise.
     *
     * @param deviceId the device
     * @return true if wavelength transparent, false otherwise
     */
    private boolean isTransparent(DeviceId deviceId) {
        return TRANSPARENT_DEVICES.contains(
                Optional.ofNullable(deviceService.getDevice(deviceId))
                        .map(Device::type)
                        .orElse(Device.Type.OTHER));
    }

    // Flow-related processing assumes only numbers in some parts, so use only numbers.
    private PortNumber onlyNumber(PortNumber portNumber) {
        if (portNumber.hasName()) {
            return PortNumber.portNumber(portNumber.toLong());
        }
        return portNumber;
    }
}
