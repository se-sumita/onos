package org.onosproject.net.openroadm.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Wavelength path.
 */
public final class WavelengthPath implements OpticalPath<Long> {
    private final long id;
    private final long groupId;
    private final int frequencyId;
    private final OchSignal signal;
    private final Link ingressEdge;
    private final Link egressEdge;
    private final Path path;
    private final Rate rate;
    private final ModulationFormat modulationFormat;
    private final double qValue;
    private final double qThreshold;
    private final String name;
    private final boolean submitted;

    /**
     * Constructor.
     * @param id wavelength path ID (0 if not reserved/submitted)
     * @param groupId path group ID
     * @param frequencyId frequency ID
     * @param signal signal information
     * @param ingressEdge edge(link) of Ingress
     * @param egressEdge edge(link) of Egress
     * @param path path
     * @param rate rate
     * @param modulationFormat modulation-format
     * @param qValue Q-value
     * @param qThreshold Q-threshold
     * @param name name of path
     * @param submitted true if submitted
     */
    private WavelengthPath(long id, long groupId,
                           int frequencyId, OchSignal signal,
                           Link ingressEdge, Link egressEdge, Path path,
                           Rate rate, ModulationFormat modulationFormat,
                           double qValue, double qThreshold, String name,
                           boolean submitted) {
        this.id = id;
        this.groupId = groupId;
        this.frequencyId = frequencyId;
        this.signal = signal;
        this.ingressEdge = ingressEdge;
        this.egressEdge = egressEdge;
        this.path = path;
        this.rate = rate;
        this.modulationFormat = modulationFormat;
        this.qValue = qValue;
        this.qThreshold = qThreshold;
        this.name = name;
        this.submitted = submitted;
    }

    @Override
    public Long key() {
        return id;
    }

    @Override
    public ConnectPoint src() {
        return srcOch();
    }

    @Override
    public ConnectPoint dst() {
        return dstOch();
    }

    @Override
    public Path path() {
        return path;
    }

    public long id() {
        return id;
    }

    public int frequencyId() {
        return frequencyId;
    }

    public OchSignal signal() {
        return signal;
    }

    public ConnectPoint srcOch() {
        return ingressEdge.src();
    }

    public ConnectPoint dstOch() {
        return egressEdge.dst();
    }

    public ConnectPoint addPort() {
        return ingressEdge.dst();
    }

    public ConnectPoint dropPort() {
        return egressEdge.src();
    }

    public Link ingressEdge() {
        return ingressEdge;
    }

    public Link egressEdge() {
        return egressEdge;
    }

    public List<Link> links() {
        List<Link> links = Lists.newArrayListWithCapacity(path.links().size() + 2);
        links.add(ingressEdge);
        links.addAll(path.links());
        links.add(egressEdge);
        return ImmutableList.copyOf(links);
    }

    public Rate rate() {
        return rate;
    }

    public ModulationFormat modulationFormat() {
        return modulationFormat;
    }

    public double qValue() {
        return qValue;
    }

    public double qThreshold() {
        return qThreshold;
    }

    public long groupId() {
        return groupId;
    }

    public String name() {
        return name;
    }

    public boolean isReserved() {
        return !submitted;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public Pair<ConnectPoint, ConnectPoint> endPoints() {
        //return Pair.of(path.srcOch(), path.dstOch());
        return Pair.of(ingressEdge.src(), egressEdge.dst());
    }

    public static WavelengthPath create(long id, long groupId, int frequencyId, OchSignal signal,
                                        Link ingressEdge, Link egressEdge, Path path,
                                        Rate rate, ModulationFormat modulationFormat,
                                        double qValue, double qThreshold, String name) {
        return new WavelengthPath(
                id, groupId, frequencyId, signal, ingressEdge, egressEdge,
                path, rate, modulationFormat, qValue, qThreshold, name, false);
    }

    public static WavelengthPath create(long id, long groupId, int frequencyId, OchSignal signal,
                                        Link ingressEdge, Link egressEdge, Path path,
                                        Rate rate, ModulationFormat modulationFormat,
                                        double qValue, double qThreshold, String name,
                                        boolean submitted) {
        return new WavelengthPath(
                id, groupId, frequencyId, signal, ingressEdge, egressEdge,
                path, rate, modulationFormat, qValue, qThreshold, name, submitted);
    }

    public WavelengthPath cloneAsSubmitted() {
        return new WavelengthPath(
                id, groupId, frequencyId, signal, ingressEdge, egressEdge,
                path, rate, modulationFormat, qValue, qThreshold, name, true);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof WavelengthPath) {
            WavelengthPath that = (WavelengthPath) obj;
            return Objects.equals(id, that.id) &&
                    Objects.equals(groupId, that.groupId) &&
                    Objects.equals(frequencyId, that.frequencyId) &&
                    Objects.equals(signal, that.signal) &&
                    Objects.equals(ingressEdge, that.ingressEdge) &&
                    Objects.equals(egressEdge, that.egressEdge) &&
                    Objects.equals(path, that.path) &&
                    Objects.equals(rate, that.rate) &&
                    Objects.equals(modulationFormat, that.modulationFormat) &&
                    Objects.equals(qValue, that.qValue) &&
                    Objects.equals(qThreshold, that.qThreshold) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(submitted, that.submitted);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                groupId,
                frequencyId,
                signal,
                ingressEdge,
                egressEdge,
                path,
                rate,
                modulationFormat,
                qValue,
                qThreshold,
                name,
                submitted
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("groupId", groupId)
                .add("frequencyId", frequencyId)
                .add("signal", signal)
                .add("path", links().stream()
                        .map(this::linkToString).collect(Collectors.toList()))
                .add("rate", rate)
                .add("modulationFormat", modulationFormat)
                .add("qValue", qValue)
                .add("qThreshold", qThreshold)
                .add("name", name)
                .add("submitted", submitted)
                .toString();
    }

    private String linkToString(Link link) {
        ConnectPoint src = link.src();
        ConnectPoint dst = link.dst();
        return src.deviceId().toString() + "/" + src.port().toLong()
                + "-" + dst.deviceId().toString() + "/" + dst.port().toLong();
    }
}
