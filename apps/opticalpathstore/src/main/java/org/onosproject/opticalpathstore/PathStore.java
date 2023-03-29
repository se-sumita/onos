package org.onosproject.opticalpathstore;

import org.onosproject.net.ConnectPoint;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Path store.
 */
public interface PathStore<Key, T> extends Store {
    /**
     * Get a path by path object.
     * @param key key
     * @return path
     */
    T get(Key key);

    /**
     * Get all paths.
     * @return paths
     */
    Collection<T> getPaths();

    /**
     * Get paths from end ports.
     * @param ingress ingress port
     * @param egress egress port
     * @return paths with the same ingress/egress port as specified parameters.
     */
    Collection<T> getPaths(ConnectPoint ingress, ConnectPoint egress);

    /**
     * Clear path store.
     */
    void clear();

    /**
     * stream.
     * @return stream
     */
    Stream<T> stream();
}
