package org.onosproject.opticalpathstore.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.graph.ScalarWeight;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.net.openroadm.model.OsnrMap;
import org.onosproject.net.openroadm.model.WdmPath;
import org.onosproject.net.openroadm.model.WdmPathKey;
import org.onosproject.opticalpathstore.WdmPathEvent;
import org.onosproject.opticalpathstore.WdmPathEventListener;
import org.onosproject.opticalpathstore.WdmPathStore;
import org.onosproject.store.serializers.KryoNamespaces;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation for WDM path store.
 */
@Component(immediate = true)
public class DistributedWdmPathStore
        extends AbstractDistributedPathStore<WdmPathKey, WdmPath, WdmPathEvent, WdmPathEventListener>
        implements WdmPathStore {

    private final Logger log = getLogger(getClass());

    private final Object cacheVarsLock = new Object();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    CoreService coreService;

    Multimap<ConnectPoint, WdmPath> ingressMap;

    Multimap<ConnectPoint, WdmPath> egressMap;

    Multimap<Pair<ConnectPoint, ConnectPoint>, WdmPath> pairMap;

    @Override
    public void activate() {
        super.activate();
        if (eventDispatcher == null) {
            eventDispatcher = DefaultServiceDirectory.getService(EventDeliveryService.class);
        }
        eventDispatcher.addSink(WdmPathEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(WdmPathEvent.class);
        log.info("Stopped");
    }

    protected ConsistentMap<WdmPathKey, WdmPath> createConsistentMap() {
        ApplicationId appId = coreService.getAppId("org.onosproject.opticalpathstore");

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(OchParam.class)
                .register(OsnrMap.class)
                .register(WdmPathKey.class)
                .register(WdmPath.class)
                .register(ScalarWeight.class);

        return storageService.<WdmPathKey, WdmPath>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("wdm-path-store")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();
    }

    @Override
    protected void initializeCache() {
        synchronized (cacheVarsLock) {
            ingressMap = ArrayListMultimap.create();
            egressMap = ArrayListMultimap.create();
            pairMap = ArrayListMultimap.create();
        }
        super.initializeCache();
    }

    @Override
    protected void addToCache(WdmPath path) {
        synchronized (cacheVarsLock) {
            ConnectPoint src = path.src();
            ConnectPoint dst = path.dst();
            ingressMap.put(src, path);
            egressMap.put(dst, path);
            pairMap.put(Pair.of(src, dst), path);
        }
    }

    @Override
    protected void removeFromCache(WdmPath path) {
        synchronized (cacheVarsLock) {
            ConnectPoint src = path.src();
            ConnectPoint dst = path.dst();
            ingressMap.remove(src, path);
            egressMap.remove(dst, path);
            pairMap.remove(Pair.of(src, dst), path);
        }
    }

    void addAll(Collection<WdmPath> paths) {
        checkNotNull(paths);
        for (WdmPath path : paths) {
            pathMap.put(path.key(), path);
        }
    }

    @Override
    public Collection<WdmPath> getPaths(ConnectPoint ingress, ConnectPoint egress) {
        if (ingress != null && egress != null) {
            synchronized (cacheVarsLock) {
                return ImmutableList.copyOf(pairMap.get(Pair.of(ingress, egress)));
            }
        } else if (ingress != null) { // egress == null
            return getPathsByIngressPort(ingress);
        } else if (egress != null) {  // ingress == null
            return getPathsByEgressPort(egress);
        } else { // ingress == null && egress == null
            return getPaths();
        }
    }

    Collection<WdmPath> getPathsByIngressPort(ConnectPoint ingress) {
        synchronized (cacheVarsLock) {
            return ImmutableList.copyOf(ingressMap.get(ingress));
        }
    }

    Collection<WdmPath> getPathsByEgressPort(ConnectPoint egress) {
        synchronized (cacheVarsLock) {
            return ImmutableList.copyOf(egressMap.get(egress));
        }
    }

    @Override
    public void replace(ConnectPoint ingress, ConnectPoint egress, Collection<WdmPath> paths) {
        Collection<WdmPath> removed = remove(ingress, egress);
        addAll(paths);
        post(new WdmPathEvent(WdmPathEvent.Type.PATHS_REPLACED,
                ImmutableList.copyOf(paths), removed));
    }

    @Override
    public void clear() {
        Collection<WdmPath> paths = pathMap.values().stream()
                .map(Versioned::value).collect(Collectors.toList());
        super.clear();
        post(new WdmPathEvent(WdmPathEvent.Type.PATHS_CLEARED, Collections.emptyList(), paths));
    }

    Collection<WdmPath> remove(ConnectPoint ingress, ConnectPoint egress) {
        List<WdmPath> paths = Lists.newArrayList();
        if (ingress != null && egress != null) {
            synchronized (cacheVarsLock) {
                paths.addAll(pairMap.get(Pair.of(ingress, egress)));
                paths.addAll(pairMap.get(Pair.of(egress, ingress)));
            }
            paths.forEach(p -> pathMap.remove(p.key()));
        } else if (ingress != null) { // egress == null
            // Remove bidirectional
            paths.addAll(removeByIngressPort(ingress));
            paths.addAll(removeByEgressPort(ingress));
        } else if (egress != null) {  // ingress == null
            // Remove bidirectional
            paths.addAll(removeByIngressPort(egress));
            paths.addAll(removeByEgressPort(egress));
        } else { // ingress == null && egress == null
            paths = pathMap.values().stream().map(Versioned::value)
                    .collect(Collectors.toList());
            super.clear(); // NO EVENT
        }
        return paths;
    }

    Collection<WdmPath> removeByIngressPort(ConnectPoint ingress) {
        checkNotNull(ingress);

        Collection<WdmPath> paths;
        synchronized (cacheVarsLock) {
            paths = Lists.newArrayList(ingressMap.get(ingress));
        }
        paths.forEach(p -> pathMap.remove(p.key()));
        return paths;
    }

    Collection<WdmPath> removeByEgressPort(ConnectPoint egress) {
        checkNotNull(egress);

        Collection<WdmPath> paths;
        synchronized (cacheVarsLock) {
            paths = Lists.newArrayList(egressMap.get(egress));
        }
        paths.forEach(p -> pathMap.remove(p.key()));
        return paths;
    }

    @Override
    public WdmPath getReversePath(WdmPath path) {
        checkNotNull(path);
        Collection<WdmPath> paths;
        synchronized (cacheVarsLock) {
            paths = pairMap.get(Pair.of(path.dst(), path.src()));
        }
        return paths.stream().filter(p -> isReverse(path, p)).findFirst().orElse(null);
    }

    private boolean isReverse(WdmPath path1, WdmPath path2) {
        return path1.src().equals(path2.dst()) &&
                path1.dst().equals(path2.src()) &&
                Lists.reverse(
                        path1.path().links().stream()
                            .flatMap(l -> Stream.of(l.src(), l.dst()))
                            .collect(Collectors.toList())
                ).equals(
                        path2.path().links().stream()
                                .flatMap(l -> Stream.of(l.src(), l.dst()))
                                .collect(Collectors.toList())
                );
    }
}
