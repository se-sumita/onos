package org.onosproject.opticalpathstore.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.graph.ScalarWeight;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;

import java.util.List;
import java.util.stream.Collectors;

public final class PathTestTool {
    public static final ProviderId PID = new ProviderId("test", "test");

    private PathTestTool() {}

    public static Link link(String port1, String port2) {
        return link(point(port1), point(port2));
    }

    public static ConnectPoint point(String deviceId) {
        String[] id = deviceId.split("/");
        return NetTestTools.connectPointNoOF(id[0], Integer.valueOf(id[1]));
    }

    public static Link link(ConnectPoint point1, ConnectPoint point2) {
        return DefaultLink.builder()
                .type(Link.Type.DIRECT)
                .providerId(PID)
                .src(point1)
                .dst(point2).build();
    }

    public static Link reverseLink(Link link) {
        return DefaultLink.builder()
                .type(link.type())
                .providerId(link.providerId())
                .src(link.dst())
                .dst(link.src()).build();
    }

    public static OchSignal och(int ochId) {
        return OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, ochId);
    }

    public static Path path(Link... links) {
        return new DefaultPath(links[0].providerId(),
                ImmutableList.copyOf(links),
                new ScalarWeight(1.0));
    }

    public static Path buildReversePath(Path path) {
        List<Link> links = Lists.reverse(path.links()).stream()
                .map(l -> DefaultLink.builder()
                        .providerId(l.providerId())
                        .src(l.dst()).dst(l.src())
                        .type(l.type()).state(l.state())
                        .annotations(l.annotations())
                        .build())
                .collect(Collectors.toList());
        return new DefaultPath(path.providerId(), links, path.weight(), path.annotations());
    }
}
