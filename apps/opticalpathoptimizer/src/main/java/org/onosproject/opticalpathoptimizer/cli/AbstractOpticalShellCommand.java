package org.onosproject.opticalpathoptimizer.cli;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.opticalpathoptimizer.api.OpticalPathOptimizerService;

import java.util.Collection;

public abstract class AbstractOpticalShellCommand extends AbstractShellCommand {
    protected DeviceService deviceService = get(DeviceService.class);

    protected OpticalPathOptimizerService optimizer() {
        return get(OpticalPathOptimizerService.class);
    }

    protected ObjectNode okStatusJson() {
        return mapper().createObjectNode().put("status", "ok");
    }

    protected ObjectNode errorStatusJson(RuntimeException ex) {
        return errorStatusJson(ex.getMessage());
    }

    protected ObjectNode errorStatusJson(String errorMessage) {
        return mapper().createObjectNode()
                .put("status", "error")
                .put("message", errorMessage);
    }

    protected void ok(String consoleMessage) {
        if (outputJson()) {
            print("%s", okStatusJson());
        } else {
            print(consoleMessage);
        }
    }

    protected void failed(String errorMessage) {
        if (outputJson()) {
            print("%s", errorStatusJson(errorMessage));
        } else {
            error(errorMessage);
        }
    }

    protected void failed(RuntimeException ex) {
        if (outputJson()) {
            print("%s", errorStatusJson(ex));
        } else {
            throw ex;
        }
    }

    protected void putConnectPointToJson(ObjectNode object, ConnectPoint point) {
        object
            .put("device", point.deviceId().toString())
            .put("portId", point.port().toLong())
            .put("portName", deviceService.getPort(point).annotations().value(AnnotationKeys.PORT_NAME));
    }

    protected void putLinksToJson(ArrayNode linkArray, Collection<Link> links) {
        for (Link link : links) {
            ObjectNode linkObject = linkArray.addObject();
            putConnectPointToJson(linkObject.putObject("source"), link.src());
            putConnectPointToJson(linkObject.putObject("destination"), link.dst());
        }
    }
}
