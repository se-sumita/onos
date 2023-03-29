package org.onosproject.opticalpathoptimizer.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.opticalpathoptimizer.WavelengthPathCandidate;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.api.OpticalPathOptimizerService;
import org.onosproject.opticalpathoptimizer.cli.completer.OchPortCompleter;
import org.onosproject.opticalpathoptimizer.model.QValue;
import org.onosproject.opticalpathoptimizer.util.FrequencyConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.opticalpathoptimizer.cli.CliUtils.checkOchPoint;
import static org.onosproject.opticalpathoptimizer.cli.CliUtils.reloadConnectPort;

/**
 * Calculate wavelength path command.
 */
@Service
@Command(scope = "onos", name = "wavelength-path-calc",
        description = "Calculate Wavelength Path.")
public class WavelengthPathCalcCommand extends AbstractOpticalShellCommand {

    @Option(name = "-i1", aliases = "--ingress1",
            description = "Ingress Device/Port Description [1]",
            required = true)
    @Completion(OchPortCompleter.class)
    String ingressStr1 = null;

    @Option(name = "-e1", aliases = "--egress1",
            description = "Egress Device/Port Description [1]",
            required = true)
    @Completion(OchPortCompleter.class)
    String egressStr1 = null;

    @Option(name = "-i2", aliases = "--ingress2",
            description = "Ingress Device/Port Description [2]",
            required = false)
    @Completion(OchPortCompleter.class)
    String ingressStr2 = null;

    @Option(name = "-e2", aliases = "--egress2",
            description = "Egress Device/Port Description [2]",
            required = false)
    @Completion(OchPortCompleter.class)
    String egressStr2 = null;

    @Option(name = "-f", aliases = "--force",
            description = "Force calculate even if the topology has changed after WDM path calculated.",
            required = false)
    boolean force = false;

    @Override
    protected void doExecute() throws Exception {
        ConnectPoint ingress1 = null;
        ConnectPoint egress1 = null;
        ConnectPoint ingress2 = null;
        ConnectPoint egress2 = null;

        try {
            // ingress 1 => egress 1
            if (ingressStr1 != null) {
                ingress1 = checkOchPoint(ingressStr1);
            }
            if (egressStr1 != null) {
                egress1 = checkOchPoint(egressStr1);
            }

            // ingress 2 => egress 2
            if (ingressStr2 != null) {
                ingress2 = checkOchPoint(ingressStr2);
            }
            if (egressStr2 != null) {
                egress2 = checkOchPoint(egressStr2);
            }

            if (ingress1 == null && egress1 != null) {
                throw new IllegalArgumentException("Missing --ingress1 option");
            }
            if (ingress1 != null && egress1 == null) {
                throw new IllegalArgumentException("Missing --egress1 option");
            }

            if (ingress2 == null && egress2 != null) {
                throw new IllegalArgumentException("Missing --ingress2 option");
            }
            if (ingress2 != null && egress2 == null) {
                throw new IllegalArgumentException("Missing --egress2 option");
            }

            List<Pair<ConnectPoint, ConnectPoint>> portPairs = Lists.newArrayList();
            portPairs.add(Pair.of(ingress1, egress1));

            /* Port duplication check */
            Set<ConnectPoint> ports = Sets.newHashSet(ingress1, egress1);
            if (ingress2 != null) {
                portPairs.add(Pair.of(ingress2, egress2));
                ports.add(ingress2);
                ports.add(egress2);
            }
            if (ports.size() % 2 != 0) {
                throw new IllegalArgumentException("All ports must be different ports.");
            }

            /* Whether WDM path calculation has not been performed after a topology change */
            OpticalPathOptimizerService optimizer = optimizer();
            if (!force && optimizer().getWdmCalcNecessary()) {
                throw new IllegalArgumentException(
                        "Detected the topology has changed after WDM path calculated.\n" +
                                "Before executing wavelength-path-calc, you need to run wdm-path-calc.\n" +
                                "Or execute this command with `--force` option.");
            }

            /* calculate */
            optimizer.calculateWavelengthPaths(portPairs);

            List<WavelengthPathCandidate> list = optimizer.getWavelengthPathCandidates();

            if (outputJson()) {
                print("%s", json(list));
            } else {
                console(list);
            }

        } catch (IllegalArgumentException | CommandFailedException ex) {
            failed(ex);
        }
    }

    private void console(List<WavelengthPathCandidate> list) {
        print("Calculate wavelength path succeeded.");
        print("");

        if (list.isEmpty()) {
            print("No results.");
            return;
        }

        int i = 1;
        for (WavelengthPathCandidate c : list) {
            Collection<WavelengthPathCandidate.WavelengthPathEntry> entries = c.getPaths();
            print("Index=%d %s", i++, entries.size() >= 2 ? "Disjoint-path" : "Single-path");
            int ei = 1;
            for (WavelengthPathCandidate.WavelengthPathEntry entry : entries) {
                QValue value = entry.qValue();
                print("  Path[%d]", ei++);
                print("    Ingress=%s", reloadConnectPort(deviceService, entry.srcOch()));
                print("    Egress=%s", reloadConnectPort(deviceService, entry.dstOch()));
                print("    Qvalue=%.3f Qmargin=%.3f", value.qValue(), value.qValue() - value.qThreshold());
                print("    Rate=%s ModulationFormat=%s",
                        entry.ochParam().rate().name(),
                        entry.ochParam().modulationFormat().name());
                print("    Links");
                int linkIndex = 1;
                for (Link link : entry.links()) {
                    print("      Link[%d] %s -> %s",
                            linkIndex++,
                            reloadConnectPort(deviceService, link.src()),
                            reloadConnectPort(deviceService, link.dst()));
                }

                print("    Available Frequencies");
                for (Map.Entry<Integer, OchSignal> signal : entry.getLambdas().entrySet()) {
                    print("      [%d] %.2fTHz",
                            signal.getKey(),
                            FrequencyConverter.frequency(signal.getValue()).asTHz());
                }
            }
        }
    }

    private JsonNode json(List<WavelengthPathCandidate> paths) {
        ArrayNode result = mapper().createArrayNode();

        int i = 1;
        for (WavelengthPathCandidate c : paths) {
            ObjectNode pathGroup = result.addObject();
            pathGroup.put("index", i++);
            ArrayNode pathList = pathGroup.putArray("paths");

            Collection<WavelengthPathCandidate.WavelengthPathEntry> entries = c.getPaths();
            for (WavelengthPathCandidate.WavelengthPathEntry entry : entries) {
                ObjectNode object = pathList.addObject();
                QValue value = entry.qValue();
                putConnectPointToJson(object.putObject("ingress"), entry.srcOch());
                putConnectPointToJson(object.putObject("egress"), entry.dstOch());
                object.put("qValue", value.qValue());
                object.put("qMargin", value.qValue() - value.qThreshold());
                object.put("rate", entry.ochParam().rate().name());
                object.put("modulationFormat", entry.ochParam().modulationFormat().name());

                putLinksToJson(object.putArray("links"), entry.links());

                ArrayNode frequencies = object.putArray("availableFrequencies");
                for (Map.Entry<Integer, OchSignal> signal : entry.getLambdas().entrySet()) {
                    ObjectNode freqObject = frequencies.addObject();
                    freqObject.put("id", signal.getKey());
                    freqObject.put("centerFrequency",
                            FrequencyConverter.frequency(signal.getValue()).asTHz());
                }
            }
        }
        return result;
    }
}
