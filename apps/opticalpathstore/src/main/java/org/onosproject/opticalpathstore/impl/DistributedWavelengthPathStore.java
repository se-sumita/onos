package org.onosproject.opticalpathstore.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.graph.ScalarWeight;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathstore.WavelengthPathEvent;
import org.onosproject.opticalpathstore.WavelengthPathEventListener;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation for Wavelength path store.
 */
@Component(immediate = true)
public class DistributedWavelengthPathStore
        extends AbstractDistributedPathStore<Long, WavelengthPath, WavelengthPathEvent, WavelengthPathEventListener>
        implements WavelengthPathStore {

    private final Logger log = getLogger(getClass());

    private final Object cacheVarsLock = new Object();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    CoreService coreService;

    Multimap<Long, WavelengthPath> groupMap;

    Multimap<ConnectPoint, WavelengthPath> terminationPointMap;

    Multimap<Set<ConnectPoint>, WavelengthPath> pairMap;

    Map<Pair<ConnectPoint, OchSignal>, WavelengthPath> omsLambdaMap;

    private AtomicCounter idCounter;

    private AtomicCounter groupIdCounter;

    @Override
    public void activate() {
        super.activate();

        if (eventDispatcher == null) {
            eventDispatcher = DefaultServiceDirectory.getService(EventDeliveryService.class);
        }
        eventDispatcher.addSink(WavelengthPathEvent.class, listenerRegistry);

        idCounter = storageService.getAtomicCounter("wavelength-path-id");
        groupIdCounter = storageService.getAtomicCounter("wavelength-path-group-id");

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(WavelengthPathEvent.class);
        log.info("Stopped");
    }

    protected ConsistentMap<Long, WavelengthPath> createConsistentMap() {
        ApplicationId appId = coreService.getAppId("org.onosproject.opticalpathstore");

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(WavelengthPath.class)
                .register(ScalarWeight.class);

        return storageService.<Long, WavelengthPath>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("wavelength-path-store")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();
    }

    @Override
    protected void initializeCache() {
        synchronized (cacheVarsLock) {
            groupMap = ArrayListMultimap.create();
            terminationPointMap = ArrayListMultimap.create();
            pairMap = ArrayListMultimap.create();
            omsLambdaMap = Maps.newHashMap();
        }
        super.initializeCache();
    }

    @Override
    protected void addToCache(WavelengthPath path) {
        synchronized (cacheVarsLock) {
            groupMap.put(path.groupId(), path);
            ConnectPoint src = path.src();
            ConnectPoint dst = path.dst();
            terminationPointMap.put(src, path);
            terminationPointMap.put(dst, path);
            pairMap.put(ImmutableSet.of(src, dst), path);
            path.path().links().stream()
                    .flatMap(l -> Stream.of(l.src(), l.dst()))
                    .distinct()
                    .forEach(p -> omsLambdaMap.put(Pair.of(p, path.signal()), path));
        }
    }

    @Override
    protected void removeFromCache(WavelengthPath path) {
        synchronized (cacheVarsLock) {
            groupMap.remove(path.groupId(), path);
            ConnectPoint src = path.src();
            ConnectPoint dst = path.dst();
            terminationPointMap.remove(src, path);
            terminationPointMap.remove(dst, path);
            pairMap.remove(ImmutableSet.of(src, dst), path);
            path.path().links().stream()
                    .flatMap(l -> Stream.of(l.src(), l.dst()))
                    .distinct()
                    .forEach(p -> omsLambdaMap.remove(Pair.of(p, path.signal())));
        }
    }

    public WavelengthPath build(
            long groupId,
            Link ingressEdge, Link egressEdge, Path path,
            int frequencyId, OchSignal signal,
            Rate rate, ModulationFormat modulationFormat,
            double qValue, double qThreshold, String name) {
        checkNotNull(path);
        long id = idCounter.incrementAndGet();
        return WavelengthPath.create(
                id, groupId, frequencyId, signal, ingressEdge, egressEdge,
                path, rate, modulationFormat, qValue, qThreshold, name);
    }

    @Override
    public Collection<WavelengthPath> getPaths(ConnectPoint ingress, ConnectPoint egress) {
        if (ingress != null && egress != null) {
            synchronized (cacheVarsLock) {
                return ImmutableList.copyOf(pairMap.get(ImmutableSet.of(ingress, egress)));
            }
        } else if (ingress != null) { // egress == null
            return getPathsByPort(ingress);
        } else if (egress != null) {  // ingress == null
            return getPathsByPort(egress);
        } else { // ingress == null && egress == null
            return getPaths();
        }
    }

    Collection<WavelengthPath> getPathsByPort(ConnectPoint port) {
        synchronized (cacheVarsLock) {
            return terminationPointMap.get(port);
        }
    }

    void add(WavelengthPath path) {
        checkNotNull(path);
        pathMap.put(path.key(), path);
    }

    @Override
    public void addAll(List<WavelengthPath> paths) {
        checkNotNull(paths);
        paths.forEach(this::add);
        paths.forEach(this::postAddedEvent);
    }

    @Override
    public long update(WavelengthPath path) {
        if (pathMap.containsKey(path.id())) {
            pathMap.put(path.id(), path);
            postUpdatedEvent(path);
        } else {
            pathMap.put(path.id(), path);
            postAddedEvent(path);
        }
        return path.id();
    }

    @Override
    public void remove(long id) {
        Versioned<WavelengthPath> path = pathMap.remove(id);

        if (path != null && path.value() != null) {
            postRemovedEvent(path.value());
        }
    }

    @Override
    public void clear() {
        List<WavelengthPath> paths = ImmutableList.copyOf(pathMap.asJavaMap().values());
        super.clear();
        paths.forEach(this::postRemovedEvent);
    }

    @Override
    public void remove(ConnectPoint ingress, ConnectPoint egress) {
        if (ingress != null && egress != null) {
            Collection<WavelengthPath> paths;
            synchronized (cacheVarsLock) {
                paths = Lists.newArrayList(pairMap.get(ImmutableSet.of(ingress, egress)));
            }
            paths.forEach(p -> pathMap.remove(p.key()));
            paths.forEach(this::postRemovedEvent);
        } else if (ingress != null) { // egress == null
            remove(ingress);
        } else if (egress != null) {  // ingress == null
            remove(egress);
        } else { // ingress == null && egress == null
            clear();
        }
    }

    public void remove(ConnectPoint port) {
        Collection<WavelengthPath> paths;
        synchronized (cacheVarsLock) {
            paths = ImmutableList.copyOf(terminationPointMap.get(port));
        }
        paths.forEach(p -> pathMap.remove(p.key()));
        paths.forEach(this::postRemovedEvent);
    }

    @Override
    public void removeAll(long groupId) {
        removeAll(findByGroupId(groupId));
    }

    @Override
    public void removeAll(List<WavelengthPath> paths) {
        for (WavelengthPath path : paths) {
            remove(path.id());
        }
    }

    @Override
    public List<WavelengthPath> findByGroupId(long groupId) {
        synchronized (cacheVarsLock) {
            return ImmutableList.copyOf(groupMap.get(groupId));
        }
    }

    @Override
    public WavelengthPath findByOmsPortAndLambda(ConnectPoint omsPort, OchSignal signal) {
        synchronized (cacheVarsLock) {
            return omsLambdaMap.get(Pair.of(omsPort, signal));
        }
    }

    public Map<Long, Collection<WavelengthPath>> getGroupMap() {
        synchronized (cacheVarsLock) {
            return Multimaps.asMap(groupMap);
        }
    }

    @Override
    public long issueGroupId() {
        return groupIdCounter.incrementAndGet();
    }

    @Override
    public void releaseGroupIdIfPossible(long groupId) {
        groupIdCounter.compareAndSet(groupId, groupId - 1);
    }

    @Override
    public int size() {
        return pathMap.size();
    }

    private void postAddedEvent(WavelengthPath path) {
        post(new WavelengthPathEvent(WavelengthPathEvent.Type.PATH_ADDED,
                                     path, getCoupledServices(path.groupId())));
    }

    private void postUpdatedEvent(WavelengthPath path) {
        post(new WavelengthPathEvent(WavelengthPathEvent.Type.PATH_UPDATED,
                                     path, getCoupledServices(path.groupId())));
    }

    private void postRemovedEvent(WavelengthPath path) {
        post(new WavelengthPathEvent(WavelengthPathEvent.Type.PATH_REMOVED, path));
    }

    private Set<Long> getCoupledServices(long groupId) {
        synchronized (cacheVarsLock) {
            return ImmutableSet.copyOf(
                    groupMap.get(groupId).stream()
                            .map(WavelengthPath::id)
                            .collect(Collectors.toList()));
        }
    }
}
