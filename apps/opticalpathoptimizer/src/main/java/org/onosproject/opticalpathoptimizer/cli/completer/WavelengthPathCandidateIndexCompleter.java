package org.onosproject.opticalpathoptimizer.cli.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.opticalpathoptimizer.api.OpticalPathOptimizerService;

import java.util.List;
import java.util.SortedSet;

/**
 * Wavelength path candidate completer.
 */
@Service
public class WavelengthPathCandidateIndexCompleter implements Completer {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        OpticalPathOptimizerService service = AbstractShellCommand.get(OpticalPathOptimizerService.class);

        int count = service.getWavelengthPathCandidates().size();
        SortedSet<String> strings = delegate.getStrings();
        for (int index = 1; index <= count; index++) {
            strings.add(String.valueOf(index));
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }
}
