package org.onosproject.opticalpathstore.impl;

import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventListener;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.event.ListenerService;
import org.onosproject.net.openroadm.model.OpticalPath;
import org.onosproject.opticalpathstore.PathStore;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.util.Collection;
import java.util.stream.Stream;

@Component
public abstract class AbstractDistributedPathStore<Key, T extends OpticalPath<Key>,
            E extends Event, L extends EventListener<E>>
        extends AbstractStore implements PathStore<Key, T>, ListenerService<E, L> {

    protected ConsistentMap<Key, T> pathMap;

    protected final ListenerRegistry<E, L> listenerRegistry = new ListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        pathMap = createConsistentMap();

        initializeCache();
    }

    protected abstract ConsistentMap<Key, T> createConsistentMap();

    protected void initializeCache() {
        pathMap.addListener(new MapEventListener<Key, T>() {
            @Override
            public void event(MapEvent<Key, T> event) {
                switch (event.type()) {
                    case INSERT:
                        addToCache(event.newValue().value());
                        break;
                    case REMOVE:
                        removeFromCache(event.oldValue().value());
                        break;
                    case UPDATE:
                        removeFromCache(event.oldValue().value());
                        addToCache(event.newValue().value());
                        break;
                    default:
                        break;
                }
            }
        });

        // cache for searching
        for (Versioned<T> path : pathMap.values()) {
            addToCache(path.value());
        }
    }

    protected abstract void addToCache(T path);

    protected abstract void removeFromCache(T path);

    @Override
    public T get(Key p) {
        Versioned<T> path = pathMap.get(p);
        return path == null ? null : path.value();
    }

    @Override
    public Collection<T> getPaths() {
        return pathMap.asJavaMap().values();
    }

    @Override
    public void clear() {
        pathMap.clear();
    }

    @Override
    public Stream<T> stream() {
        return pathMap.asJavaMap().values().stream();
    }

    @Override
    public void addListener(L listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(L listener) {
        listenerRegistry.removeListener(listener);
    }

    protected void post(E event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }
}
