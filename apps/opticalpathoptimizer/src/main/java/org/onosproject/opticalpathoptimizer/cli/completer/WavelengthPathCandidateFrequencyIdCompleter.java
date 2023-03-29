package org.onosproject.opticalpathoptimizer.cli.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.opticalpathoptimizer.WavelengthPathCandidate;
import org.onosproject.opticalpathoptimizer.api.OpticalPathOptimizerService;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Frequency ID of wavelength path candidate completer.
 */
@Service
public class WavelengthPathCandidateFrequencyIdCompleter implements Completer {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        OpticalPathOptimizerService service = AbstractShellCommand.get(OpticalPathOptimizerService.class);

        // Candidate index (previous argument).
        String candidateIndex = commandLine.getArguments()[commandLine.getCursorArgumentIndex() - 1];
        int index;
        try {
            index = Integer.parseInt(candidateIndex);
        } catch (NumberFormatException ex) {
            return delegate.complete(session, commandLine, candidates);
        }

        List<WavelengthPathCandidate> candidateList = service.getWavelengthPathCandidates();
        if (index <= 0 || index > candidateList.size()) {
            return delegate.complete(session, commandLine, candidates);
        }
        Set<Integer> candidate = candidateList.get(index - 1).getMainPath().getLambdas().keySet();
        SortedSet<String> strings = delegate.getStrings();
        candidate.stream().map(String::valueOf).forEach(strings::add);

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }
}
