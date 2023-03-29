package org.onosproject.net.openroadm.model;

import com.google.common.base.MoreObjects;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;

import java.util.Objects;

/**
 * Key of WDM path.
 */
public final class WdmPathKey {
    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Path path;

    /**
     * Build WDM path key.
     * @param src source port
     * @param dst destination port
     * @param path path
     * @return built WDM path key
     */
    public static WdmPathKey of(ConnectPoint src, ConnectPoint dst, Path path) {
        return new WdmPathKey(src, dst, path);
    }

    /**
     * Constructor.
     * @param src source port
     * @param dst destination port
     * @param path path
     */
    private WdmPathKey(ConnectPoint src, ConnectPoint dst, Path path) {
        this.src = src;
        this.dst = dst;
        this.path = path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof WdmPathKey) {
            final WdmPathKey that = (WdmPathKey) obj;
            return Objects.equals(this.src, that.src) &&
                   Objects.equals(this.dst, that.dst) &&
            Objects.equals(this.path, that.path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("src", src)
                .add("dst", dst)
                .add("path", path)
                .toString();
    }
}
