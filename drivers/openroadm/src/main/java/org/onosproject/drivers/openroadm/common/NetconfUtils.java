package org.onosproject.drivers.openroadm.common;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility for NETCONF processing.
 */
public final class NetconfUtils {

    private NetconfUtils() {}

    private static final Logger log = getLogger(NetconfUtils.class);

    /**
     * Create NETCONF rpc get message with xmlns filter.
     * @param xmlns XML NS
     * @param tag Tag name
     * @return rpc message
     */
    public static String rpcGetMessage(String xmlns, String tag) {
        if (xmlns.indexOf('/') < 0) {
            xmlns = "urn:ietf:params:xml:ns:yang:" + xmlns;
        }
        //Message ID is injected later.
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
            "<get>" +
            "<filter type=\"subtree\">" +
            "<" + tag + " xmlns=\"" + xmlns + "\"/>" +
            "</filter>" +
            "</get>" +
            "</rpc>";
    }

    /**
     * Create NETCONF rpc get message with subtree filter.
     * @param filter filter
     * @return rpc message
     */
    public static String rpcGetMessageWithSubtreeFilter(String filter) {
        //Message ID is injected later.
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
            "<get>" +
            "<filter type=\"subtree\">" +
            filter +
            "</filter>" +
            "</get>" +
            "</rpc>";
    }

    /**
     * Create NETCONF rpc get message with XPath filter.
     * @param xpath XPath
     * @return rpc message
     */
    public static String rpcGetMessageByXpath(String xpath) {
        //Message ID is injected later.
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
            "<get>" +
            "<filter select=\"" + xpath + "\" type=\"xpath\"/>" +
            "</get>" +
            "</rpc>";
    }

    /**
     * Create requests from velocity templates.
     * @param context velocity context
     * @param templates velocity templates
     * @return request messages
     */
    public static List<String> createRequests(VelocityContext context, List<Template> templates) {
        List<String> requests = new ArrayList<>();
        for (Template template : templates) {
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            requests.add(writer.toString());
        }
        return requests;
    }

    /**
     * Create requests from velocity templates with each commit request.
     * @param context velocity context
     * @param templates velocity templates
     * @return request messages
     */
    public static List<String> createRequestsWithCommit(VelocityContext context, List<Template> templates) {
        List<String> requests = new ArrayList<>();
        for (Template template : templates) {
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            requests.add(writer.toString());
            requests.add(rpcCommitMessage());
        }
        return requests;
    }

    /**
     * Create requests from velocity templates and add a commit request.
     * @param context velocity context
     * @param templates velocity templates
     * @return request messages
     */
    public static List<String> createRequestsAndLastCommit(VelocityContext context,
                                                           List<Template> templates) {
        List<String> requests = new ArrayList<>();
        for (Template template : templates) {
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            requests.add(writer.toString());
        }
        requests.add(rpcCommitMessage());
        return requests;
    }

    /**
     * Run a series of NETCONF requests.
     * @param handler driver handler
     * @param requests series of requests
     * @param onSucceeded callback when success
     * @param onFailed callback when failure
     */
    public static void requests(DriverHandler handler,
                                List<String> requests, Runnable onSucceeded, Runnable onFailed) {
        requests(handler, requests, onSucceeded, onFailed, null);
    }

    /**
     * Run a series of NETCONF requests.
     * @param handler driver handler
     * @param requests series of requests
     * @param onSucceeded callback when success
     * @param onFailed callback when failure
     * @param onAfterRequest callback when all done
     */
    public static void requests(DriverHandler handler,
                                List<String> requests, Runnable onSucceeded, Runnable onFailed,
                                Runnable onAfterRequest) {
        DeviceId deviceId = handler.data().deviceId();
        NetconfController controller = checkNotNull(handler.get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();

        boolean error = false;
        synchronized (session) {  // Guard a series of requests
            int i = 1;
            for (String request : requests) {
                String logLabel = deviceId.toString() + "[" + i + "/" + requests.size() + "]";
                if (!request(session, request, logLabel)) {
                    error = true;
                    break;
                }
                if (onAfterRequest != null) {
                    onAfterRequest.run();
                }
                ++i;
            }
        }

        if (error) {
            onFailed.run();
        } else {
            onSucceeded.run();
        }
    }

    private static String rpcCommitMessage() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
                "<commit/>" +
                "</rpc>";
    }

    private static boolean request(NetconfSession session, String rpcEditMessage, String label) {
        try {
            log.debug("{} request={}", label, rpcEditMessage);
            String reply = session.requestSync(rpcEditMessage);
            log.debug("{} reply={}", label, reply);
            HierarchicalConfiguration cfg = XmlConfigParser.loadXmlString(reply);
            if (cfg.containsKey("ok")) {
                return true;
            }
        } catch (NetconfException e) {
            log.warn("exception:", e);
            e.printStackTrace();
        }
        return false;
    }

}
