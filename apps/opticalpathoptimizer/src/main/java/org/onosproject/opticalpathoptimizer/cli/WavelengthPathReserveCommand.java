package org.onosproject.opticalpathoptimizer.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.Link;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.cli.completer.WavelengthPathCandidateFrequencyIdCompleter;
import org.onosproject.opticalpathoptimizer.cli.completer.WavelengthPathCandidateIndexCompleter;
import org.onosproject.opticalpathoptimizer.util.FrequencyConverter;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.opticalpathoptimizer.cli.CliUtils.reloadConnectPort;

/**
 * Reserve wavelength path command.
 */
@Service
@Command(scope = "onos", name = "wavelength-path-reserve",
        description = "Reserve Wavelength Path.")
public class WavelengthPathReserveCommand extends AbstractOpticalShellCommand {

    @Argument(index = 0, name = "index", description = "Index number in wavelength path calculation results",
            required = true, multiValued = false)
    @Completion(WavelengthPathCandidateIndexCompleter.class)
    int index = 0;

    @Argument(index = 1, name = "frequencies", description = "Frequency ID or ID list (ex. 1 2).",
            required = true, multiValued = true)
    @Completion(WavelengthPathCandidateFrequencyIdCompleter.class)
    List<String> frequencyIds = null;

    @Option(name = "-n", aliases = "--name",
            description = "Path name list (ex. INDEX FREQ_ID1 FREQ_ID2 --name NAME1 NAME2)",
            required = false)
    List<String> names = null;

    @Override
    protected void doExecute() throws Exception {
        // The display and user-specified indexes start at 1,
        // and the internally managed index starts at 0.
        index -= 1;

        // The CLI mechanism does not allow optional arguments to be specified
        // after the multiValued == true parameter (they are recognized as part
        // of multiValued).
        // Therefore, implement parsing here.

        try {
            // Check --name|-n
            OptionalInt nameIndex = IntStream.range(0, frequencyIds.size())
                    .filter(idx -> isNameParameter(frequencyIds.get(idx)))
                    .findFirst();
            // Separate at --name|-n
            List<Integer> ids;
            List<String> pathNames;
            if (nameIndex.isPresent()) {
                ids = toFrequencyIds(frequencyIds.stream().limit(nameIndex.getAsInt()));
                pathNames = frequencyIds.stream().skip(nameIndex.getAsInt() + 1).collect(Collectors.toList());
                if (ids.isEmpty() && pathNames.size() > 0) {
                    int count = pathNames.size() / 2;
                    ids = toFrequencyIds(pathNames.stream().skip(count));
                    pathNames = pathNames.stream().limit(count).collect(Collectors.toList());
                }
            } else {
                ids = toFrequencyIds(frequencyIds.stream());
                pathNames = Collections.emptyList();
            }

            List<WavelengthPath> results = optimizer().reserveWavelengthPath(index, ids, pathNames);
            checkArgument(!results.isEmpty(), "Failed to reserve wavelength path.");

            if (outputJson()) {
                print("%s", json(results));
            } else {
                console(results);
            }
        } catch (IllegalArgumentException | CommandFailedException ex) {
            failed(ex);
        } catch (IndexOutOfBoundsException ex) {
            failed("Invalid index value. index=" + (index + 1));
        }
    }

    private boolean isNameParameter(String text) {
        return "--name".equals(text) || "-n".equals(text);
    }

    private List<Integer> toFrequencyIds(Stream<String> frequencyIds) {
        try {
            return frequencyIds
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "unable to convert argument frequencies with value '"
                            + frequencyIds.collect(Collectors.toList()).toString()
                            + "' to type Integer"
            );
        }
    }

    private void console(List<WavelengthPath> paths) {
        print("Reserve wavelength path succeeded.");
        print("");

        long groupId = paths.get(0).groupId();
        print("Submit ID: %d", groupId);
        int i = 1;
        for (WavelengthPath path : paths) {
            print("  Path[%d]", i++);
            print("    Name=%s", Strings.nullToEmpty(path.name()));
            print("    Ingress=%s", reloadConnectPort(deviceService, path.srcOch()));
            print("    Egress=%s", reloadConnectPort(deviceService, path.dstOch()));
            print("    Frequency=[%d]%.2fTHz", path.frequencyId(),
                    FrequencyConverter.frequency(path.signal()).asTHz());
            print("    Rate=%s", path.rate().name());
            print("    ModulationFormat=%s", path.modulationFormat().name());
            print("    Qvalue=%.3f", path.qValue());
            print("    Qmargin=%.3f", path.qValue() - path.qThreshold());
            int linkIndex = 1;
            print("    Links");
            for (Link link : path.links()) {
                print("      Link[%d] %s -> %s",
                        linkIndex++,
                        reloadConnectPort(deviceService, link.src()),
                        reloadConnectPort(deviceService, link.dst()));
            }
        }
        print("");
        print("When you need submit, please run command shown below:");

        print("");
        print("  wavelength-path-submit %d", groupId);
    }

    private JsonNode json(List<WavelengthPath> paths) {
        ObjectNode result = mapper().createObjectNode();
        long groupId = paths.get(0).groupId();
        result.put("submitId", groupId);

        ArrayNode pathArray = result.putArray("paths");
        for (WavelengthPath path : paths) {
            ObjectNode pathObject = pathArray.addObject();
            pathObject.put("name", Strings.emptyToNull(path.name()));
            putConnectPointToJson(pathObject.putObject("ingress"), path.srcOch());
            putConnectPointToJson(pathObject.putObject("egress"), path.dstOch());
            pathObject
                    .put("frequencyId", path.frequencyId())
                    .put("centerFrequency",
                            FrequencyConverter.frequency(path.signal()).asTHz())
                    .put("rate", path.rate().name())
                    .put("modulationFormat", path.modulationFormat().name())
                    .put("qValue", path.qValue())
                    .put("qMargin", path.qValue() - path.qThreshold());
            putLinksToJson(pathObject.putArray("links"), path.links());
        }
        return result;
    }
}
