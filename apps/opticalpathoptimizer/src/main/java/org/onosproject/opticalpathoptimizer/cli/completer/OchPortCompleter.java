package org.onosproject.opticalpathoptimizer.cli.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * OCh port completer.
 */
@Service
public class OchPortCompleter implements Completer {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        DeviceService service = AbstractShellCommand.get(DeviceService.class);

        SortedSet<String> strings = delegate.getStrings();

        for (Device device : service.getDevices()) {
            if (device.type() == Device.Type.ROADM || device.type() == Device.Type.ROADM_OTN) {
                continue;
            }
            strings.addAll(
                    service.getPorts(device.id()).stream()
                            .filter(p -> p.type() == Port.Type.OCH)
                            .map(p -> new ConnectPoint(device.id(), p.number()))
                            .map(p -> String.format("%s/%s",
                                                    p.deviceId(),
                                                    p.port().toStringForShell()))
                            .collect(Collectors.toList())
            );
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }
}
