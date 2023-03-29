package org.onosproject.opticalpathoptimizer.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.opticalpathoptimizer.api.CommandFailedException;
import org.onosproject.opticalpathoptimizer.cli.completer.OmsAddDropPortCompleter;
import org.onosproject.opticalpathoptimizer.util.OsnrCalculationException;

import static org.onosproject.opticalpathoptimizer.cli.CliUtils.checkOmsAddDropPort;

/**
 * Calculate WDM path command.
 */
@Service
@Command(scope = "onos", name = "wdm-path-calc",
        description = "Calculate WDM Path.")
public class WdmPathCalcCommand extends AbstractOpticalShellCommand {

    @Option(name = "-i", aliases = "--ingress",
            description = "Ingress Device/Port Description",
            required = false, multiValued = false)
    @Completion(OmsAddDropPortCompleter.class)
    String ingressStr = null;

    @Option(name = "-e", aliases = "--egress",
            description = "Egress Device/Port Description",
            required = false, multiValued = false)
    @Completion(OmsAddDropPortCompleter.class)
    String egressStr = null;

    @Override
    protected void doExecute() throws Exception {
        try {
            ConnectPoint ingress = null;
            ConnectPoint egress = null;
            if (ingressStr != null) {
                ingress = checkOmsAddDropPort(ingressStr);
            }
            if (egressStr != null) {
                egress = checkOmsAddDropPort(egressStr);
            }

            optimizer().calculateWdmPaths(ingress, egress);

            ok("Calculate WDM path succeeded.");
        } catch (IllegalArgumentException | CommandFailedException ex) {
            failed(ex);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof OsnrCalculationException) {
                failed(ex.getCause().getMessage());
            } else {
                failed(ex);
            }
        }
    }
}
