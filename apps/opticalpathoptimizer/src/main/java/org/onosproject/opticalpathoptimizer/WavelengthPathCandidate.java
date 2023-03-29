package org.onosproject.opticalpathoptimizer;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.opticalpathoptimizer.model.QValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wavelength path candidate.
 */
public class WavelengthPathCandidate {
    private final List<WavelengthPathEntry> paths;

    public WavelengthPathCandidate() {
        paths = Collections.synchronizedList(Lists.newLinkedList());
    }

    public List<WavelengthPathEntry> getPaths() {
        return ImmutableList.copyOf(paths);
    }

    public WavelengthPathEntry getMainPath() {
        return paths.get(0);
    }

    public void addPath(
            Link ingressEdge, Link egressEdge, Path wdmPath,
            Map<Integer, OchSignal> availableLambdas, OchParam ochParam, QValue q) {
        paths.add(new WavelengthPathEntry(ingressEdge, egressEdge, wdmPath, availableLambdas, ochParam, q));
    }

    public void addPath(WavelengthPathEntry path) {
        paths.add(path);
    }

    public static final class WavelengthPathEntry {
        private final Link ingressEdge;
        private final Link egressEdge;
        private final Path wdmPath;
        private final Map<Integer, OchSignal> availableLambdas;
        private final OchParam ochParam;
        private final QValue q;

        private WavelengthPathEntry(
                Link ingressEdge, Link egressEdge, Path wdmPath,
                Map<Integer, OchSignal> availableLambdas, OchParam ochParam, QValue q) {
            this.ingressEdge = ingressEdge;
            this.egressEdge = egressEdge;
            this.wdmPath = wdmPath;
            this.availableLambdas = availableLambdas;
            this.ochParam = ochParam;
            this.q = q;
        }

        public ConnectPoint srcOch() {
            return ingressEdge.src();
        }

        public ConnectPoint dstOch() {
            return egressEdge.dst();
        }

        public ConnectPoint omsAddPort() {
            return ingressEdge.dst();
        }

        public ConnectPoint omsDropPort() {
            return egressEdge.src();
        }

        public Link ingressEdge() {
            return ingressEdge;
        }

        public Link egressEdge() {
            return egressEdge;
        }

        public Path wdmPath() {
            return wdmPath;
        }

        public OchSignal getLambda(int index) {
            return availableLambdas.get(index);
        }

        public Map<Integer, OchSignal> getLambdas() {
            return availableLambdas;
        }

        public OchParam ochParam() {
            return ochParam;
        }

        public QValue qValue() {
            return q;
        }

        public List<Link> links() {
            List<Link> links = Lists.newArrayListWithCapacity(wdmPath.links().size() + 2);
            links.add(ingressEdge);
            links.addAll(wdmPath.links());
            links.add(egressEdge);
            return links;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    ingressEdge,
                    egressEdge,
                    wdmPath,
                    availableLambdas,
                    ochParam,
                    q
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            WavelengthPathEntry that = (WavelengthPathEntry) o;
            return Objects.equals(ingressEdge, that.ingressEdge) &&
                    Objects.equals(egressEdge, that.egressEdge) &&
                    Objects.equals(wdmPath, that.wdmPath) &&
                    Objects.equals(availableLambdas, that.availableLambdas) &&
                    Objects.equals(ochParam, that.ochParam) &&
                    Objects.equals(q, that.q);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("ingressEdge", ingressEdge)
                    .add("egressEdge", egressEdge)
                    .add("wdmPath", wdmPath)
                    .add("availableLambdas:size", availableLambdas.size())
                    .add("OchParam", q)
                    .add("Qvalue", q)
                    .toString();
        }
    }

    public static boolean isDisjoint(WavelengthPathEntry p1, WavelengthPathEntry p2) {
        /* Extract the set of DeviceIDs contained in p1 (excluding EdgeNode) */
        Set<DeviceId> p1DeviceIdSet = p1.wdmPath().links().stream()
                .map(l -> l.dst().deviceId()).collect(Collectors.toSet());
        p1DeviceIdSet.remove(p1.omsAddPort().deviceId());
        p1DeviceIdSet.remove(p1.omsDropPort().deviceId());

        /* Extract the set of DeviceIDs contained in p2 (excluding EdgeNode) */
        Set<DeviceId> p2DeviceIdSet = p2.wdmPath().links().stream()
                .map(l -> l.dst().deviceId()).collect(Collectors.toSet());
        p2DeviceIdSet.remove(p2.omsAddPort().deviceId());
        p2DeviceIdSet.remove(p2.omsDropPort().deviceId());

        if (p1DeviceIdSet.size() == 0 && p2DeviceIdSet.size() == 0) {
            /* If neither route has a core node, it is not disjoint */
            return false;
        }

        /* Check for inclusion of common elements */
        boolean matches = p1DeviceIdSet.stream().anyMatch(p2DeviceIdSet::contains);

        return !matches;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(getClass());
        for (WavelengthPathEntry path : paths) {
            helper.add("index", paths.indexOf(path));
            helper.add("innerPath",  path.toString());
        }
        return helper.toString();
    }
}
