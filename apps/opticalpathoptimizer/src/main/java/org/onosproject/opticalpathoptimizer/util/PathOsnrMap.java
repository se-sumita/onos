package org.onosproject.opticalpathoptimizer.util;

import org.onosproject.net.Path;
import org.onosproject.net.openroadm.model.OsnrMap;

/**
 * Hold path and OSNR values for each rate/modulation-format.
 */
public final class PathOsnrMap {

    /**
     * Create an instance that holds the specified path.
     * @param path Path
     * @return Instance
     */
    public static PathOsnrMap of(Path path) {
        return new PathOsnrMap(path);
    }

    private final Path path;
    private final OsnrMap osnrMap;

    /**
     * Constructor.
     * @param path Path
     */
    private PathOsnrMap(Path path) {
        this.path = path;
        this.osnrMap = new OsnrMap();
    }

    /**
     * Get path.
     * @return Path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Get OSNR values for each rate/modulation-format.
     * @return OSNR values for each rate/modulation-format
     */
    public OsnrMap getOsnrMap() {
        return osnrMap;
    }
}
