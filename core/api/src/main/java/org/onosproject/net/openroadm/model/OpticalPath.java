package org.onosproject.net.openroadm.model;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;

/**
 * Optical path interface.
 * @param <Key> Key of path
 */
public interface OpticalPath<Key> {
    Key key();
    Path path();
    ConnectPoint src();
    ConnectPoint dst();
}
