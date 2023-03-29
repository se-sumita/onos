package org.onosproject.opticalpathoptimizer.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.net.openroadm.model.WdmPath;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.cli.completer.OmsAddDropPortCompleter;
import org.onosproject.opticalpathstore.WdmPathStore;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparing;
import static org.onosproject.opticalpathoptimizer.cli.CliUtils.checkOmsAddDropPort;
import static org.onosproject.opticalpathoptimizer.cli.CliUtils.reloadConnectPort;

/**
 * Show WDM path command.
 */
@Service
@Command(scope = "onos", name = "wdm-paths",
        description = "Show Wdm Path.")
public class WdmPathShowCommand extends AbstractOpticalShellCommand {
    private DeviceService deviceService = get(DeviceService.class);

    @Option(name = "-d", aliases = "--detail", description = "Print detail",
            required = false, multiValued = false)
    boolean detail = false;

    @Option(name = "-I", aliases = "--index", description = "WDM Path Index",
            required = false, multiValued = false)
    int index = 0;

    @Option(name = "-i", aliases = "--ingress", description = "Ingress Device/Port Description",
            required = false, multiValued = false)
    @Completion(OmsAddDropPortCompleter.class)
    String ingressStr = null;

    @Option(name = "-e", aliases = "--egress", description = "Egress Device/Port Description",
            required = false, multiValued = false)
    @Completion(OmsAddDropPortCompleter.class)
    String egressStr = null;

    @Override
    protected void doExecute() throws Exception {
        ConnectPoint ingress = null;
        ConnectPoint egress = null;
        List<WdmPath> wdmPaths;

        try {
            if (ingressStr != null) {
                ingress = checkOmsAddDropPort(ingressStr);
            }
            if (egressStr != null) {
                egress = checkOmsAddDropPort(egressStr);
            }

            WdmPathStore wdmPathStore = optimizer().getWdmPathStore();
            wdmPaths = wdmPathStore.getPaths().stream()
                            .sorted(comparing(WdmPath::endPoints))
                            .collect(Collectors.toList());

            /* Filtering by arguments */
            Map<Integer, WdmPath> displayWdmPaths = Maps.newTreeMap();
            if (index > 0) {
                int i = index - 1;
                checkArgument(i < wdmPaths.size(),
                        "Not found WDM path. index=" + index);
                WdmPath path = wdmPaths.get(i);
                displayWdmPaths.put(index, path);
            } else {
                int i = 0;
                for (WdmPath wdmPath : wdmPaths) {
                    i++;
                    if (ingress != null && !ingress.equals(wdmPath.src())) {
                        continue;
                    }
                    if (egress != null && !egress.equals(wdmPath.dst())) {
                        continue;
                    }
                    displayWdmPaths.put(i, wdmPath);
                }
            }

            if (outputJson()) {
                print("%s", json(displayWdmPaths));
            } else {
                console(displayWdmPaths);
            }
        } catch (IllegalArgumentException | CommandFailedException ex) {
            failed(ex);
        }
    }

    private void console(Map<Integer, WdmPath> wdmPaths) {
        for (Map.Entry<Integer, WdmPath> wdmPathEntry : wdmPaths.entrySet()) {
            int i = wdmPathEntry.getKey();
            WdmPath wdmPath = wdmPathEntry.getValue();
            if (!detail) {
                double osnr;
                try {
                    osnr = wdmPath.osnr().values().stream().findFirst().get();
                } catch (NoSuchElementException e) {
                    // No OSNR calculation result (external tool did not return)
                    osnr = Double.NaN;
                }
                print("Index=%d Ingress=%s Egress=%s OSNR=%.3f",
                        i,
                        wdmPath.src(),
                        wdmPath.dst(),
                        osnr);
            } else {
                print("Index=%d Ingress=%s Egress=%s",
                        i,
                        wdmPath.src(),
                        wdmPath.dst());
                if (wdmPath.osnr().size() > 0) {
                    for (Map.Entry<OchParam, Double> entry : wdmPath.osnr().entrySet()) {
                        print("    OSNR=%.3f (Rate=%s ModulationFormat=%s)",
                              entry.getValue(),
                              entry.getKey().rate().name(),
                              entry.getKey().modulationFormat().name());
                    }
                } else {
                    print("    OSNR=NaN (No value present)");
                }
                int linkIndex = 1;
                for (Link link : wdmPath.path().links()) {
                    print("    Link[%d] %s -> %s",
                            linkIndex++,
                            reloadConnectPort(deviceService, link.src()),
                            reloadConnectPort(deviceService, link.dst()));
                }
            }
        }
    }

    private JsonNode json(Map<Integer, WdmPath> wdmPaths) {
        ArrayNode result = mapper().createArrayNode();

        for (Map.Entry<Integer, WdmPath> wdmPathEntry : wdmPaths.entrySet()) {
            int i = wdmPathEntry.getKey();
            WdmPath wdmPath = wdmPathEntry.getValue();
            ObjectNode object = result.addObject();
            object.put("index", i);
            putConnectPointToJson(object.putObject("ingress"), wdmPath.src());
            putConnectPointToJson(object.putObject("egress"), wdmPath.dst());
            ObjectNode osnrObject = object.putObject("osnr");
            for (Map.Entry<OchParam, Double> ochParam : wdmPath.osnr().entrySet()) {
                OchParam param = ochParam.getKey();
                String key = param.rate().name() + "/" + param.modulationFormat().name();
                osnrObject.put(key, ochParam.getValue());
            }

            if (detail) {
                putLinksToJson(object.putArray("links"), wdmPath.path().links());
            }
        }
        return result;
    }
}
