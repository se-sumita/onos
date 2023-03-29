package org.onosproject.opticalpathoptimizer.cli.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathoptimizer.api.OpticalPathOptimizerService;
import org.onosproject.opticalpathstore.WavelengthPathStore;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Wavelength path submit ID completer.
 */
@Service
public class WavelengthPathSubmitIdCompleter implements Completer {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        OpticalPathOptimizerService service = AbstractShellCommand.get(OpticalPathOptimizerService.class);

        WavelengthPathStore store = service.getWavelengthPathStore();

        SortedSet<String> strings = delegate.getStrings();
        strings.addAll(
                store.stream()
                        .map(WavelengthPath::groupId)
                        .map(String::valueOf)
                        .collect(Collectors.toList())
        );

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }
}
