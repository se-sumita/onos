package org.onosproject.drivers.openroadm.common;

import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.drivers.openroadm.config.ConfigFlowRuleProgrammable;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.openroadm.flow.instructions.TransponderInstruction;
import org.onosproject.net.openroadm.service.OpticalPortUpdaterService;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OpticalDevice;
import org.onosproject.net.optical.device.OchPortHelper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Utility for processing flow rules.
 */
public final class FlowRuleUtils {

    private FlowRuleUtils() {}

    /**
     * Get the port from the selector.
     * @param deviceService device service
     * @param rule flow rule
     * @return port
     */
    public static Port inputPort(DeviceService deviceService, FlowRule rule) {
        Set<Criterion> criteria = rule.selector().criteria();
        PortNumber inPortNum = criteria.stream()
                .filter(c -> c instanceof PortCriterion)
                .map(c -> ((PortCriterion) c).port())
                .findAny()
                .orElse(null);
        return deviceService.getPort(rule.deviceId(), inPortNum);
    }

    /**
     * Get the port from the treatment.
     * @param deviceService device service
     * @param rule flow rule
     * @return port
     */
    public static Port outputPort(DeviceService deviceService, FlowRule rule) {
        List<Instruction> instructions = rule.treatment().immediate();
        PortNumber outPortNum = instructions.stream()
                .filter(i -> i instanceof Instructions.OutputInstruction)
                .map(i -> ((Instructions.OutputInstruction) i).port())
                .findAny()
                .orElse(null);
        return deviceService.getPort(rule.deviceId(), outPortNum);
    }

    /**
     * Get the wavelength information from the selector.
     * @param deviceService device service
     * @param rule flow rule
     * @return wavelength information
     */
    public static OchSignal inputOchSignal(DeviceService deviceService, FlowRule rule) {
        Set<Criterion> criteria = rule.selector().criteria();
        return criteria.stream()
                .filter(c -> c instanceof OchSignalCriterion)
                .map(c -> ((OchSignalCriterion) c).lambda())
                .findAny()
                .orElse(null);
    }

    /**
     * Get the wavelength information from the treatment.
     * @param deviceService device service
     * @param rule flow rule
     * @return wavelength information
     */
    public static OchSignal outputOchSignal(DeviceService deviceService, FlowRule rule) {
        List<Instruction> instructions = rule.treatment().immediate();
        return instructions.stream()
                .filter(i -> i instanceof L0ModificationInstruction.ModOchSignalInstruction)
                .map(i -> ((L0ModificationInstruction.ModOchSignalInstruction) i).lambda())
                .findAny()
                .orElse(null);
    }

    /**
     * Get the the transponder instruction from the treatment.
     * @param deviceService device service
     * @param rule flow rule
     * @return transponder instruction
     */
    public static TransponderInstruction outputTransponder(DeviceService deviceService, FlowRule rule) {
        List<Instruction> instructions = rule.treatment().immediate();
        return instructions.stream()
                .filter(i -> i instanceof Instructions.ExtensionInstructionWrapper)
                .map(i -> (Instructions.ExtensionInstructionWrapper) i)
                .map(i -> (TransponderInstruction) i.extensionInstruction())
                .findAny().orElse(null);
    }

    /**
     * Delegate {@link ConfigFlowRuleProgrammable#getFlowEntries}.
     * @param driver driver
     * @return flow entries
     */
    public static Collection<FlowEntry> delegateGetFlowEntries(FlowRuleProgrammable driver) {
        FlowRuleProgrammable delegate = new ConfigFlowRuleProgrammable();
        delegate.setHandler(driver.handler());
        delegate.setData(driver.data());
        return delegate.getFlowEntries();
    }

    /**
     * Delegate {@link ConfigFlowRuleProgrammable#applyFlowRules}.
     * @param driver driver
     * @param rules flow rules to apply
     * @return flow rules
     */
    public static Collection<FlowRule> delegateApplyFlowRules(FlowRuleProgrammable driver,
                                                              Collection<FlowRule> rules) {
        FlowRuleProgrammable delegate = new ConfigFlowRuleProgrammable();
        delegate.setHandler(driver.handler());
        delegate.setData(driver.data());
        return delegate.applyFlowRules(rules);
    }

    /**
     * Delegate {@link ConfigFlowRuleProgrammable#removeFlowRules}.
     * @param driver driver
     * @param rules flow rules to remove
     * @return flow rules
     */
    public static Collection<FlowRule> delegateRemoveFlowRules(FlowRuleProgrammable driver,
                                                               Collection<FlowRule> rules) {
        FlowRuleProgrammable delegate = new ConfigFlowRuleProgrammable();
        delegate.setHandler(driver.handler());
        delegate.setData(driver.data());
        return delegate.removeFlowRules(rules);
    }

    /**
     * Delegate {@link ConfigFlowRuleProgrammable#containsFlowRules}.
     * @param driver driver
     * @param rules flow rules to check
     * @return flow rules
     */
    public static boolean delegateContainsFlowRules(FlowRuleProgrammable driver, Collection<FlowRule> rules) {
        ConfigFlowRuleProgrammable delegate = new ConfigFlowRuleProgrammable();
        delegate.setHandler(driver.handler());
        delegate.setData(driver.data());
        return delegate.containsFlowRules(rules);
    }

    /**
     * Create empty OCh signal object.
     * @return empty OCh signal
     */
    public static OchSignal createEmptyOchSignal() {
        Frequency centerFrequency = Frequency.ofHz(0);
        ChannelSpacing channelSpacing = ChannelSpacing.CHL_50GHZ;
        int spacingMultiplier = (int) Math.round(
                (double) centerFrequency.subtract(Spectrum.CENTER_FREQUENCY).asHz()
                        / channelSpacing.frequency().asHz()
        );
        return new OchSignal(GridType.DWDM, channelSpacing, spacingMultiplier, 4);
    }

    /**
     * Update OCh signal to the OMS Add/Drop port.
     * @param handler driver handler
     * @param device device
     * @param port port
     * @param ochSignal wavelength information
     */
    public static void updateOchSignalToAddDropPort(DriverHandler handler,
                                                    Device device, Port port,
                                                    OchSignal ochSignal) {
        if (!device.is(OpticalDevice.class)) {
            return;
        }
        OpticalDevice opticalDevice = device.as(OpticalDevice.class);

        OchPort ochPort = opticalDevice.portAs(port, OchPort.class).orElse(null);
        if (ochPort == null) {
            return;
        }

        PortDescription description = OchPortHelper.ochPortDescription(
            ochPort.number(), ochPort.isEnabled(), ochPort.signalType(),
            ochPort.isTunable(), ochSignal,
            (SparseAnnotations) ochPort.annotations()
        );
        OpticalPortUpdaterService service = handler.get(OpticalPortUpdaterService.class);
        service.updatePortDescription(device, description);
    }

    /**
     * Update transponder annotation to the OCh port.
     * @param handler driver handler
     * @param device device
     * @param port port
     * @param transponder transponder instruction
     * @param ochSignal OCh signal
     * @param isRemove `true` if remove
     */
    public static void updateTransponderAnnotationToOchPort(DriverHandler handler, Device device,
                                                            Port port, TransponderInstruction transponder,
                                                            OchSignal ochSignal, boolean isRemove) {
        if (!device.is(OpticalDevice.class)) {
            return;
        }
        OpticalDevice opticalDevice = device.as(OpticalDevice.class);

        OchPort ochPort = opticalDevice.portAs(port, OchPort.class).orElse(null);
        if (ochPort == null) {
            return;
        }

        if (ochSignal == null) {
            ochSignal = createEmptyOchSignal();
        }

        DefaultAnnotations.Builder builder = DefaultAnnotations.builder()
            .putAll(ochPort.annotations());
        if (isRemove) {
            builder.remove(Annotation.KEY_RATE)
                .remove(Annotation.KEY_MODULATION_FORMAT);
        } else {
            builder
                .set(Annotation.KEY_RATE, transponder.rate().name())
                .set(Annotation.KEY_MODULATION_FORMAT, transponder.modulationFormat().name());
        }
        PortDescription description = OchPortHelper.ochPortDescription(
            ochPort.number(), ochPort.isEnabled(), ochPort.signalType(),
            ochPort.isTunable(), ochSignal, builder.build()
        );
        OpticalPortUpdaterService service = handler.get(OpticalPortUpdaterService.class);
        service.updatePortDescription(device, description);
    }
}
