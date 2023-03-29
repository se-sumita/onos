package org.onosproject.opticalpathoptimizer.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.cli.completer.ReservedWavelengthPathCompleter;

/**
 * Submit wavelength path command.
 */
@Service
@Command(scope = "onos", name = "wavelength-path-submit",
        description = "Submit Wavelength Path.")
public class WavelengthPathSubmitCommand extends AbstractOpticalShellCommand {

    @Argument(index = 0, name = "submitId", description = "Submit ID",
            required = true, multiValued = false)
    @Completion(ReservedWavelengthPathCompleter.class)
    long submitId = 0;

    @Override
    protected void doExecute() throws Exception {
        try {
            optimizer().submitWavelengthPath(submitId);

            ok("Started to apply wavelength path configurations. SubmitId=" + submitId);
        } catch (IllegalArgumentException | CommandFailedException ex) {
            failed(ex);
        }
    }
}
