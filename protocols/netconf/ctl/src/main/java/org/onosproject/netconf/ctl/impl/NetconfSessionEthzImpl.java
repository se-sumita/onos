/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.netconf.ctl.impl;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.channel.Channel;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.onosproject.netconf.*;
import org.onosproject.netconf.NetconfDeviceOutputEvent.Type;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a NETCONF session to talk to a device.
 */
public class NetconfSessionEthzImpl extends AbstractNetconfSession {

    private static final Logger log = getLogger(NetconfSessionEthzImpl.class);

    private static final String ENDPATTERN = "]]>]]>";
    private static final String MESSAGE_ID_STRING = "message-id";
    private static final String HELLO = "<hello";
    private static final String NEW_LINE = "\n";
    private static final String END_OF_RPC_OPEN_TAG = "\">";
    private static final String EQUAL = "=";
    private static final String NUMBER_BETWEEN_QUOTES_MATCHER = "\"+([0-9]+)+\"";
    private static final String SUBTREE_FILTER_CLOSE = "</filter>";
    private static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final String SUBSCRIPTION_SUBTREE_FILTER_OPEN =
            "<filter xmlns:base10=\"urn:ietf:params:xml:ns:netconf:base:1.0\" base10:type=\"subtree\">";

    private static final String INTERLEAVE_CAPABILITY_STRING = "urn:ietf:params:netconf:capability:interleave:1.0";

    private static final String CAPABILITY_REGEX = "<capability>\\s*(.*?)\\s*</capability>";
    private static final Pattern CAPABILITY_REGEX_PATTERN = Pattern.compile(CAPABILITY_REGEX);

    private static final String SESSION_ID_REGEX = "<session-id>\\s*(.*?)\\s*</session-id>";
    private static final Pattern SESSION_ID_REGEX_PATTERN = Pattern.compile(SESSION_ID_REGEX);
    private static final String HASH = "#";
    private static final String LF = "\n";
    private static final String LESS_THAN = "<";
    private static final String MSGLEN_REGEX_PATTERN = "\n#\\d+\n";
    private static final String NETCONF_10_CAPABILITY = "urn:ietf:params:netconf:base:1.0";
    protected static final String NETCONF_11_CAPABILITY = "urn:ietf:params:netconf:base:1.1";

    private String sessionID;
    private final AtomicInteger messageIdInteger = new AtomicInteger(1);
    private Connection netconfConnection;
    protected final NetconfDeviceInfo deviceInfo;
    private Session sshSession;
    private boolean connectionActive;
    private Iterable<String> onosCapabilities =
            ImmutableList.of(NETCONF_10_CAPABILITY, NETCONF_11_CAPABILITY);

    /* NOTE: the "serverHelloResponseOld" is deprecated in 1.10.0 and should eventually be removed */
    @Deprecated
    private String serverHelloResponseOld;
    private final Set<String> deviceCapabilities = new LinkedHashSet<>();
    private NetconfStreamHandler streamHandler;
    private Map<Integer, CompletableFuture<String>> replies;
    private List<String> errorReplies;
    private boolean subscriptionConnected = false;
    private String notificationFilterSchema = null;

    private final Collection<NetconfDeviceOutputEventListener> primaryListeners =
            new CopyOnWriteArrayList<>();
    private final Collection<NetconfSession> children =
            new CopyOnWriteArrayList<>();

    private int connectTimeout;
    private int replyTimeout;

    public NetconfSessionEthzImpl(NetconfDeviceInfo deviceInfo) throws NetconfException {
        this.deviceInfo = deviceInfo;
        this.netconfConnection = null;
        this.sshSession = null;
        connectionActive = false;
        replies = new ConcurrentHashMap<>();
        errorReplies = new ArrayList<>();

        startConnection();
    }

    public NetconfSessionEthzImpl(NetconfDeviceInfo deviceInfo, List<String> capabilities) throws NetconfException {
        this.deviceInfo = deviceInfo;
        this.netconfConnection = null;
        this.sshSession = null;
        connectionActive = false;
        replies = new ConcurrentHashMap<>();
        errorReplies = new ArrayList<>();
        setOnosCapabilities(capabilities);
        startConnection();

    }

    private void startConnection() throws NetconfException {
        connectTimeout = deviceInfo.getConnectTimeoutSec().orElse(
                NetconfControllerImpl.netconfConnectTimeout);
        replyTimeout = deviceInfo.getReplyTimeoutSec().orElse(
                NetconfControllerImpl.netconfReplyTimeout);
        log.info("Connecting to {} with timeouts C:{}, R:{}. I:connect-timeout", deviceInfo,
                connectTimeout, replyTimeout);

        if (!connectionActive) {
            netconfConnection = new Connection(deviceInfo.ip().toString(), deviceInfo.port());

            try {
                netconfConnection.connect(null, 1000 * connectTimeout, 1000 * connectTimeout);
            } catch (IOException e) {
                throw new NetconfException("Cannot open a connection with device " + deviceInfo, e);
            }
            boolean isAuthenticated;
            try {
                if (deviceInfo.getKey() != null) {
                    log.debug("Authenticating with key to device {} with username {}",
                            deviceInfo.getDeviceId(), deviceInfo.name());
                    isAuthenticated = netconfConnection.authenticateWithPublicKey(
                            deviceInfo.name(), deviceInfo.getKey(),
                            deviceInfo.password().equals("") ? null : deviceInfo.password());
                } else {
                    log.debug("Authenticating to device {} with username {} with password",
                            deviceInfo.getDeviceId(), deviceInfo.name());
                    isAuthenticated = netconfConnection.authenticateWithPassword(
                            deviceInfo.name(), deviceInfo.password());
                }
            } catch (IOException e) {
                log.error("Authentication connection to device {} failed",
                        deviceInfo.getDeviceId(), e);
                throw new NetconfException("Authentication connection to device " +
                        deviceInfo.getDeviceId() + " failed", e);
            }

            connectionActive = true;
            Preconditions.checkArgument(isAuthenticated,
                    "Authentication to device %s with username " +
                            "%s failed",
                    deviceInfo.getDeviceId(), deviceInfo.name());
            startSshSession();
        }
    }

    private void startSshSession() throws NetconfException {
        try {
            sshSession = netconfConnection.openSession();
            sshSession.startSubSystem("netconf");
            streamHandler = new NetconfStreamThread(sshSession.getStdout(), sshSession.getStdin(),
                    sshSession.getStderr(), deviceInfo,
                    new NetconfSessionDelegateImpl(),
                    replies);
            this.addDeviceOutputListener(new FilteringNetconfDeviceOutputEventListener(deviceInfo));
            sendHello();
        } catch (IOException e) {
            log.error("Failed to create ch.ethz.ssh2.Session session {} ", e.getMessage());
            throw new NetconfException("Failed to create ch.ethz.ssh2.Session session with device" +
                    deviceInfo, e);
        }
    }


    @Beta
    protected void startSubscriptionStream(String filterSchema) throws NetconfException {
        boolean openNewSession = false;
        if (!deviceCapabilities.contains(INTERLEAVE_CAPABILITY_STRING)) {
            log.info("Device {} doesn't support interleave, creating child session", deviceInfo);
            openNewSession = true;

        } else if (subscriptionConnected &&
                notificationFilterSchema != null &&
                !Objects.equal(filterSchema, notificationFilterSchema)) {
            // interleave supported and existing filter is NOT "no filtering"
            // and was requested with different filtering schema
            log.info("Cannot use existing session for subscription {} ({})",
                    deviceInfo, filterSchema);
            openNewSession = true;
        }

        if (openNewSession) {
            log.info("Creating notification session to {} with filter {}",
                    deviceInfo, filterSchema);
            NetconfSession child = new NotificationSession(deviceInfo);

            child.addDeviceOutputListener(new NotificationForwarder());

            child.startSubscription(filterSchema);
            children.add(child);
            return;
        }

        // request to start interleaved notification session
        String reply = sendRequest(createSubscriptionString(filterSchema));
        if (!checkReply(reply)) {
            throw new NetconfException("Subscription not successful with device "
                    + deviceInfo + " with reply " + reply);
        }
        subscriptionConnected = true;
    }

    @Override
    public void startSubscription() throws NetconfException {
        if (!subscriptionConnected) {
            startSubscriptionStream(null);
        }
        streamHandler.setEnableNotifications(true);
    }

    @Beta
    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
        if (!subscriptionConnected) {
            notificationFilterSchema = filterSchema;
            startSubscriptionStream(filterSchema);
        }
        streamHandler.setEnableNotifications(true);
    }

    @Beta
    protected String createSubscriptionString(String filterSchema) {
        StringBuilder subscriptionbuffer = new StringBuilder();
        subscriptionbuffer.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        subscriptionbuffer.append("  <create-subscription\n");
        subscriptionbuffer.append("xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\">\n");
        // FIXME Only subtree filtering supported at the moment.
        if (filterSchema != null) {
            subscriptionbuffer.append("    ");
            subscriptionbuffer.append(SUBSCRIPTION_SUBTREE_FILTER_OPEN).append(NEW_LINE);
            subscriptionbuffer.append(filterSchema).append(NEW_LINE);
            subscriptionbuffer.append("    ");
            subscriptionbuffer.append(SUBTREE_FILTER_CLOSE).append(NEW_LINE);
        }
        subscriptionbuffer.append("  </create-subscription>\n");
        subscriptionbuffer.append("</rpc>\n");
        subscriptionbuffer.append(ENDPATTERN);
        return subscriptionbuffer.toString();
    }

    @Override
    public void endSubscription() throws NetconfException {
        if (subscriptionConnected) {
            streamHandler.setEnableNotifications(false);
        } else {
            throw new NetconfException("Subscription does not exist.");
        }
    }

    private void sendHello() throws NetconfException {
        serverHelloResponseOld = sendRequest(createHelloString(), true);
        Matcher capabilityMatcher = CAPABILITY_REGEX_PATTERN.matcher(serverHelloResponseOld);
        while (capabilityMatcher.find()) {
            deviceCapabilities.add(capabilityMatcher.group(1));
        }
        sessionID = String.valueOf(-1);
        Matcher sessionIDMatcher = SESSION_ID_REGEX_PATTERN.matcher(serverHelloResponseOld);
        if (sessionIDMatcher.find()) {
            sessionID = sessionIDMatcher.group(1);
        } else {
            throw new NetconfException("Missing SessionID in server hello " +
                    "reponse.");
        }

    }

    private String createHelloString() {
        StringBuilder hellobuffer = new StringBuilder();
        hellobuffer.append(XML_HEADER);
        hellobuffer.append("\n");
        hellobuffer.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        hellobuffer.append("  <capabilities>\n");
        onosCapabilities.forEach(
                cap -> hellobuffer.append("    <capability>")
                        .append(cap)
                        .append("</capability>\n"));
        hellobuffer.append("  </capabilities>\n");
        hellobuffer.append("</hello>\n");
        hellobuffer.append(ENDPATTERN);
        return hellobuffer.toString();

    }

    @Override
    public void checkAndReestablish() throws NetconfException {
        if (sshSession.getState() != Channel.STATE_OPEN) {
            try {
                log.debug("Trying to reopen the Sesion with {}", deviceInfo.getDeviceId());
                startSshSession();
            } catch (NetconfException | IllegalStateException e) {
                log.debug("Trying to reopen the Connection with {}", deviceInfo.getDeviceId());
                try {
                    connectionActive = false;
                    replies.clear();
                    startConnection();
                    if (subscriptionConnected) {
                        log.debug("Restarting subscription with {}", deviceInfo.getDeviceId());
                        subscriptionConnected = false;
                        startSubscription(notificationFilterSchema);
                    }
                } catch (NetconfException e2) {
                    log.error("No connection {} for device {}", netconfConnection, e.getMessage());
                    throw new NetconfException("Cannot re-open the connection with device" + deviceInfo, e);
                }
            }
        }
    }

    /**
     * Validate and format message according to chunked framing mechanism.
     *
     * @param request to format
     * @return formated message
     */
    private static String validateChunkedMessage(String request) {
        if (request.endsWith(ENDPATTERN)) {
            request = request.substring(0, request.length() - ENDPATTERN.length());
        }
        if (!request.startsWith(LF + HASH)) {
            request = LF + HASH + request.length() + LF + request + LF + HASH + HASH + LF;
        }
        return request;
    }

    /**
     * Validate and format netconf message.
     *
     * @param request to format
     * @return formated message
     */
    private String validateNetconfMessage(String request) {
        if (deviceCapabilities.contains(NETCONF_11_CAPABILITY)) {
            request = validateChunkedMessage(request);
        } else {
            if (!request.contains(ENDPATTERN)) {
                request = request + NEW_LINE + ENDPATTERN;
            }
        }
        return  request;
    }

    @Override
    public String requestSync(String request) throws NetconfException {
        String reply = sendRequest(request);
        checkReply(reply);
        return reply;
    }

    @Override
    @Deprecated
    public CompletableFuture<String> request(String request) throws NetconfException {
        return streamHandler.sendMessage(request);
    }

    @Override
    public CompletableFuture<String> rpc(String request) throws NetconfException {
        return null;
    }

    @Override
    public int timeoutConnectSec() {
        return connectTimeout;
    }

    @Override
    public int timeoutReplySec() {
        return replyTimeout;
    }

    /**
     * Idle timeout is not settable on ETZ_SSH - the value used is the connect timeout.
     */
    @Override
    public int timeoutIdleSec() {
        return connectTimeout;
    }

    private CompletableFuture<String> request(String request, int messageId) {
        return streamHandler.sendMessage(request, messageId);
    }

    private String sendRequest(String request) throws NetconfException {
        request = validateNetconfMessage(request);
        return sendRequest(request, false);
    }

    private String sendRequest(String request, boolean isHello) throws NetconfException {
        checkAndReestablish();
        int messageId = -1;
        if (!isHello) {
            messageId = messageIdInteger.getAndIncrement();
        }
        request = formatXmlHeader(request);
        request = formatRequestMessageId(request, messageId);
        CompletableFuture<String> futureReply = request(request, messageId);
        String rp;
        try {
            log.debug("Sending request to NETCONF with timeout {} for {}",
                    replyTimeout, deviceInfo.name());
            rp = futureReply.get(replyTimeout, TimeUnit.SECONDS);
            replies.remove(messageId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetconfException("Interrupted waiting for reply for request" + request, e);
        } catch (TimeoutException e) {
            throw new NetconfException("Timed out waiting for reply for request " +
                    request + " after " + replyTimeout + " sec.", e);
        } catch (ExecutionException e) {
            log.warn("Closing session {} for {} due to unexpected Error", sessionID, deviceInfo, e);

            netconfConnection.close(); //Closes the socket which should interrupt NetconfStreamThread
            sshSession.close();

            NetconfDeviceOutputEvent event = new NetconfDeviceOutputEvent(
                    NetconfDeviceOutputEvent.Type.SESSION_CLOSED,
                    null, "Closed due to unexpected error " + e.getCause(),
                    Optional.of(-1), deviceInfo);
            publishEvent(event);
            replies.clear();
            errorReplies.clear();

            throw new NetconfException("Closing session " + sessionID + " for " + deviceInfo +
                    " for request " + request, e);
        }
        log.debug("Result {} from request {} to device {}", rp, request, deviceInfo);
        return rp.trim();
    }

    private String formatRequestMessageId(String request, int messageId) {
        if (request.contains(MESSAGE_ID_STRING)) {
            //FIXME if application provides his own counting of messages this fails that count
            request = request.replaceFirst(MESSAGE_ID_STRING + EQUAL + NUMBER_BETWEEN_QUOTES_MATCHER,
                    MESSAGE_ID_STRING + EQUAL + "\"" + messageId + "\"");
        } else if (!request.contains(MESSAGE_ID_STRING) && !request.contains(HELLO)) {
            //FIXME find out a better way to enforce the presence of message-id
            request = request.replaceFirst(END_OF_RPC_OPEN_TAG, "\" " + MESSAGE_ID_STRING + EQUAL + "\""
                    + messageId + "\"" + ">");
        }
        request = updateRequestLength(request);
        return request;
    }

    private String updateRequestLength(String request) {
        if (request.contains(LF + HASH + HASH + LF)) {
            int oldLen = Integer.parseInt(request.split(HASH)[1].split(LF)[0]);
            String rpcWithEnding = request.substring(request.indexOf(LESS_THAN));
            String firstBlock = request.split(MSGLEN_REGEX_PATTERN)[1].split(LF + HASH + HASH + LF)[0];
            int newLen = 0;
            newLen = firstBlock.getBytes(UTF_8).length;
            if (oldLen != newLen) {
                return LF + HASH + newLen + LF + rpcWithEnding;
            }
        }
        return request;
    }

    private String formatXmlHeader(String request) {
        if (!request.contains(XML_HEADER)) {
            //FIXME if application provides his own XML header of different type there is a clash
            if (request.startsWith(LF + HASH)) {
                request = request.split("<")[0] + XML_HEADER + request.substring(request.split("<")[0].length());
            } else {
                request = XML_HEADER + "\n" + request;
            }
        }
        return request;
    }

    @Override
    public String getSessionId() {
        return sessionID;
    }

    @Override
    public Set<String> getDeviceCapabilitiesSet() {
        return Collections.unmodifiableSet(deviceCapabilities);
    }

    @Override
    public void setOnosCapabilities(Iterable<String> capabilities) {
        onosCapabilities = capabilities;
    }


    @Override
    public void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        streamHandler.addDeviceEventListener(listener);
        primaryListeners.add(listener);
    }

    @Override
    public void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        primaryListeners.remove(listener);
        streamHandler.removeDeviceEventListener(listener);
    }

    @Override
    protected boolean checkReply(String reply) {
        // Overridden to record error logs
        if (reply != null) {
            if (!reply.contains("<rpc-error>")) {
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            } else if (reply.contains("<ok/>")
                    || (reply.contains("<rpc-error>")
                    && reply.contains("warning"))) {
                // FIXME rpc-error with a warning is considered same as Ok??
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            }
        }
        log.warn("Device {} has error in reply {}", deviceInfo, reply);
        return false;
    }

    @Override
    public boolean close() throws NetconfException {
        return close(false);
    }

    private boolean close(boolean force) throws NetconfException {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        if (force) {
            rpc.append("<kill-session/>");
        } else {
            rpc.append("<close-session/>");
        }
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        return checkReply(sendRequest(rpc.toString())) || close(true);
    }

    protected void publishEvent(NetconfDeviceOutputEvent event) {
        primaryListeners.forEach(lsnr -> {
            if (lsnr.isRelevant(event)) {
                lsnr.event(event);
            }
        });
    }

    static class NotificationSession extends NetconfSessionEthzImpl {

        private String notificationFilter;

        NotificationSession(NetconfDeviceInfo deviceInfo)
                throws NetconfException {
            super(deviceInfo);
        }

        @Override
        protected void startSubscriptionStream(String filterSchema)
                throws NetconfException {

            notificationFilter = filterSchema;
            requestSync(createSubscriptionString(filterSchema));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("deviceInfo", deviceInfo)
                    .add("sessionID", getSessionId())
                    .add("notificationFilter", notificationFilter)
                    .toString();
        }
    }

    /**
     * Listener attached to child session for notification streaming.
     * <p>
     * Forwards all notification event from child session to primary session
     * listeners.
     */
    private final class NotificationForwarder
            implements NetconfDeviceOutputEventListener {

        @Override
        public boolean isRelevant(NetconfDeviceOutputEvent event) {
            return event.type() == Type.DEVICE_NOTIFICATION;
        }

        @Override
        public void event(NetconfDeviceOutputEvent event) {
            publishEvent(event);
        }
    }

    public class NetconfSessionDelegateImpl implements NetconfSessionDelegate {

        @Override
        public void notify(NetconfDeviceOutputEvent event) {
            Optional<Integer> messageId = event.getMessageID();
            log.debug("messageID {}, waiting replies messageIDs {}", messageId,
                    replies.keySet());
            if (!messageId.isPresent()) {
                errorReplies.add(event.getMessagePayload());
                log.error("Device {} sent error reply {}",
                        event.getDeviceInfo(), event.getMessagePayload());
                return;
            }
            CompletableFuture<String> completedReply = replies.get(messageId.get());
            if (completedReply != null) {
                completedReply.complete(event.getMessagePayload());
            }
        }
    }

    /**
     * @deprecated in 1.14.0
     */
    @Deprecated
    public static class EthzSshNetconfSessionFactory implements NetconfSessionFactory {

        @Override
        public NetconfSession createNetconfSession(NetconfDeviceInfo netconfDeviceInfo,
                                                   NetconfController netconfController) throws NetconfException {
            return new NetconfSessionEthzImpl(netconfDeviceInfo);
        }
    }
}
