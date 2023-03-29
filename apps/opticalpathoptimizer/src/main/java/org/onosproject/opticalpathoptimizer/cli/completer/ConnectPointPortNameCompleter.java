package org.onosproject.opticalpathoptimizer.cli.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;

import java.util.List;
import java.util.SortedSet;

/**
 * ConnectPoint port name completer.
 */
@Service
public class ConnectPointPortNameCompleter implements Completer {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        DeviceService service = AbstractShellCommand.get(DeviceService.class);

        // Generate the device ID/port number identifiers
        SortedSet<String> strings = delegate.getStrings();
        for (Device device : service.getDevices()) {
            for (Port port : service.getPorts(device.id())) {
                if (!port.number().isLogical()) {
                    strings.add(device.id().toString() + "/" + port.annotations().value(AnnotationKeys.PORT_NAME));
                }
            }
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }
}
