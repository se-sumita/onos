package org.onosproject.opticalpathoptimizer.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.cli.completer.WavelengthPathSubmitIdCompleter;

/**
 * Remove wavelength path command.
 */
@Service
@Command(scope = "onos", name = "wavelength-path-remove",
        description = "Remove Wavelength Path.")
public class WavelengthPathRemoveCommand extends AbstractOpticalShellCommand {

    @Argument(index = 0, name = "submitId", description = "Submit ID",
            required = true, multiValued = false)
    @Completion(WavelengthPathSubmitIdCompleter.class)
    long submitId = 0;

    @Override
    protected void doExecute() throws Exception {
        try {
            optimizer().removeWavelengthPath(submitId);

            ok("Started to remove wavelength path configurations. SubmitId=" + submitId);
        } catch (IllegalArgumentException | IllegalStateException | CommandFailedException ex) {
            failed(ex);
        }
    }
}
