package org.onosproject.opticalpathoptimizer.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.cli.completer.OchPortCompleter;
import org.onosproject.opticalpathoptimizer.cli.completer.WavelengthPathIdCompleter;
import org.onosproject.opticalpathoptimizer.cli.completer.WavelengthPathSubmitIdCompleter;
import org.onosproject.opticalpathoptimizer.util.FrequencyConverter;
import org.onosproject.opticalpathstore.WavelengthPathStore;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparing;
import static org.onosproject.opticalpathoptimizer.cli.CliUtils.checkOchPoint;
import static org.onosproject.opticalpathoptimizer.cli.CliUtils.reloadConnectPort;

/**
 * Show wavelength path command.
 */
@Service
@Command(scope = "onos", name = "wavelength-paths",
        description = "Show Wavelength Path.")
public class WavelengthPathShowCommand extends AbstractOpticalShellCommand {

    @Option(name = "-d", aliases = "--detail", description = "Print detail",
            required = false, multiValued = false)
    boolean detail = false;

    @Option(name = "-id", aliases = "--id", description = "Wavelength Path ID",
            required = false, multiValued = false)
    @Completion(WavelengthPathIdCompleter.class)
    long id = 0;

    @Option(name = "-s", aliases = "--submitId", description = "Wavelength Path Submit ID",
            required = false, multiValued = false)
    @Completion(WavelengthPathSubmitIdCompleter.class)
    long submitId = 0;

    @Option(name = "-i", aliases = "--ingress", description = "Ingress Device/Port Description",
            required = false, multiValued = false)
    @Completion(OchPortCompleter.class)
    String ingressStr = null;

    @Option(name = "-e", aliases = "--egress", description = "Egress Device/Port Description",
            required = false, multiValued = false)
    @Completion(OchPortCompleter.class)
    String egressStr = null;

    @Option(name = "-r", aliases = "--reserved", description = "Show Reserved Wavelength Path",
            required = false, multiValued = false)
    boolean reserve = false;

    @Option(name = "-s", aliases = "--submitted", description = "Show Submitted Wavelength Path",
            required = false, multiValued = false)
    boolean submitted = false;

    @Override
    protected void doExecute() throws Exception {
        ConnectPoint ingress = null;
        ConnectPoint egress = null;

        try {
            if (ingressStr != null) {
                ingress = checkOchPoint(ingressStr);
            }
            if (egressStr != null) {
                egress = checkOchPoint(egressStr);
            }

            WavelengthPathStore wavelengthPathStore = optimizer().getWavelengthPathStore();

            /* Filtering by arguments */
            Collection<WavelengthPath> displayPaths;
            if (id > 0) {
                WavelengthPath path = wavelengthPathStore.get(id);
                checkArgument(Objects.nonNull(path),
                              "Not found wavelength path. ID=" + id);
                displayPaths = ImmutableList.of(path);
            } else if (submitId > 0) {
                displayPaths = wavelengthPathStore.findByGroupId(submitId);
                checkArgument(!displayPaths.isEmpty(),
                        "Not found wavelength path. Submit ID=" + submitId);
            } else {
                displayPaths = wavelengthPathStore.getPaths(ingress, egress);
            }
            Stream<WavelengthPath> stream;
            if (reserve && !submitted) {
                stream = displayPaths.stream().filter(WavelengthPath::isReserved);
            } else if (submitted && !reserve) {
                stream = displayPaths.stream().filter(WavelengthPath::isSubmitted);
            } else {
                stream = displayPaths.stream();
            }
            List<WavelengthPath> wavelengthPaths = stream
                    .sorted(comparing(WavelengthPath::groupId).thenComparing(WavelengthPath::id))
                    .collect(Collectors.toList());

            if (outputJson()) {
                print("%s", json(wavelengthPaths));
            } else {
                console(wavelengthPaths);
            }

        } catch (IllegalArgumentException | CommandFailedException ex) {
            failed(ex);
        }
    }

    private void console(List<WavelengthPath> paths) {
        for (WavelengthPath path : paths) {
            print("ID=%d SubmitID=%d", path.id(), path.groupId());
            print("    Name=%s", Strings.nullToEmpty(path.name()));
            print("    Ingress=%s", reloadConnectPort(deviceService, path.srcOch()));
            print("    Egress=%s", reloadConnectPort(deviceService, path.dstOch()));
            print("    Frequency=[%d]%.2fTHz", path.frequencyId(),
                    FrequencyConverter.frequency(path.signal()).asTHz());
            print("    Rate=%s", path.rate().name());
            print("    ModulationFormat=%s", path.modulationFormat().name());
            print("    Qvalue=%.3f", path.qValue());
            print("    Qmargin=%.3f", path.qValue() - path.qThreshold());
            print("    Submitted=%s", Boolean.toString(path.isSubmitted()));
            if (detail) {
                int linkIndex = 1;
                print("    Links");
                for (Link link : path.links()) {
                    print("      Link[%d] %s -> %s",
                            linkIndex++,
                            reloadConnectPort(deviceService, link.src()),
                            reloadConnectPort(deviceService, link.dst()));
                }
            }
        }
    }

    private JsonNode json(List<WavelengthPath> paths) {
        ArrayNode result = mapper().createArrayNode();

        for (WavelengthPath path : paths) {
            ObjectNode object = result.addObject();
            object.put("id", path.id())
                    .put("submitId", path.groupId())
                    .put("name", Strings.emptyToNull(path.name()));
            putConnectPointToJson(object.putObject("ingress"), path.srcOch());
            putConnectPointToJson(object.putObject("egress"), path.dstOch());
            object
                    .put("frequencyId", path.frequencyId())
                    .put("centerFrequency",
                            FrequencyConverter.frequency(path.signal()).asTHz())
                    .put("rate", path.rate().name())
                    .put("modulationFormat", path.modulationFormat().name())
                    .put("qValue", path.qValue())
                    .put("qMargin", path.qValue() - path.qThreshold())
                    .put("submitted", path.isSubmitted());

            if (detail) {
                putLinksToJson(object.putArray("links"), path.links());
            }
        }
        return result;
    }
}
