package org.onosproject.net.openroadm.intent;

import com.google.common.base.MoreObjects;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WavelengthPath;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wavelength path intent.
 */
public final class WavelengthPathIntent extends Intent {

    private final List<Long> pathIds;
    private final OchSignalType signalType = OchSignalType.FIXED_GRID;
    private final Rate rate;
    private final ModulationFormat modulationFormat;
    private final boolean isBidirectional;

    private WavelengthPathIntent(ApplicationId appId,
                                 Key key,
                                 Collection<NetworkResource> resources,
                                 int priority,
                                 ResourceGroup resourceGroup,
                                 List<Long> pathIds,
                                 Rate rate,
                                 ModulationFormat modulationFormat,
                                 boolean isBidirectional) {
        super(appId, key, resources, priority, resourceGroup);
        this.pathIds = pathIds;
        this.rate = rate;
        this.modulationFormat = modulationFormat;
        this.isBidirectional = isBidirectional;
    }

    public List<Long> pathIds() {
        return pathIds;
    }

    public OchSignalType signalType() {
        return signalType;
    }

    public Rate rate() {
        return rate;
    }

    public ModulationFormat modulationFormat() {
        return modulationFormat;
    }

    public boolean isBidirectional() {
        return isBidirectional;
    }

    public static WavelengthPathIntent.Builder builder() {
        return new WavelengthPathIntent.Builder();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("key", key())
                .add("resources", resources())
                .add("pathIds", pathIds)
                .add("signalType", signalType)
                .add("isBidirectional", isBidirectional)
                .toString();
    }

    /**
     * Intent builder for WavelengthPathIntent.
     */
    public static class Builder extends Intent.Builder {
        private List<WavelengthPath> paths = new LinkedList<>();
        private Rate rate = Rate.R100G;
        private ModulationFormat modulationFormat = ModulationFormat.DP_QPSK;
        private boolean isBidirectional = true;

        public List<WavelengthPath> getPaths() {
            return paths;
        }

        @Override
        public WavelengthPathIntent.Builder appId(ApplicationId appId) {
            return (WavelengthPathIntent.Builder) super.appId(appId);
        }

        @Override
        public WavelengthPathIntent.Builder key(Key key) {
            return (WavelengthPathIntent.Builder) super.key(key);
        }

        @Override
        public WavelengthPathIntent.Builder priority(int priority) {
            return (WavelengthPathIntent.Builder) super.priority(priority);
        }

        @Override
        public WavelengthPathIntent.Builder resourceGroup(ResourceGroup resourceGroup) {
            return (WavelengthPathIntent.Builder) super.resourceGroup(resourceGroup);
        }

        public WavelengthPathIntent.Builder addWavelengthPaths(Collection<WavelengthPath> p) {
            paths.addAll(p);
            return this;
        }

        public WavelengthPathIntent.Builder addWavelengthPath(WavelengthPath p) {
            paths.add(p);
            return this;
        }

        public WavelengthPathIntent.Builder rate(Rate rate) {
            this.rate = rate;
            return this;
        }

        public WavelengthPathIntent.Builder modulationFormat(ModulationFormat modulationFormat) {
            this.modulationFormat = modulationFormat;
            return this;
        }

        public WavelengthPathIntent.Builder isBidirectional(boolean isBidirectional) {
            this.isBidirectional = isBidirectional;
            return this;
        }

        public WavelengthPathIntent build() {
            checkNotNull(appId);
            List<NetworkResource> resources = new LinkedList<>();
            for (WavelengthPath path : paths) {
                resources.addAll(path.links());
            }
            return new WavelengthPathIntent(
                    appId,
                    key,
                    resources,
                    priority,
                    resourceGroup,
                    paths.stream().map(WavelengthPath::id).collect(Collectors.toList()),
                    rate,
                    modulationFormat,
                    isBidirectional
            );
        }
    }
}
