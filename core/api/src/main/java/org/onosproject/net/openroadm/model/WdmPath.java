package org.onosproject.net.openroadm.model;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;

import java.util.Objects;

/**
 * WDM path.
 */
public class WdmPath implements OpticalPath<WdmPathKey> {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Path path;
    private final OsnrMap osnr;

    /**
     * Constructor.
     * @param src src port
     * @param dst dst port
     * @param path path
     * @param osnr OSNR value
     */
    public WdmPath(ConnectPoint src, ConnectPoint dst, Path path, OsnrMap osnr) {
        this.src = src;
        this.dst = dst;
        this.path = path;
        this.osnr = osnr;
    }

    @Override
    public WdmPathKey key() {
        return WdmPathKey.of(src, dst, path);
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public ConnectPoint src() {
        return src;
    }

    @Override
    public ConnectPoint dst() {
        return dst;
    }

    public Pair<ConnectPoint, ConnectPoint> endPoints() {
        return Pair.of(src, dst);
    }

    public OsnrMap osnr() {
        return osnr;
    }

    public String getId() {
        // device/port=device/port%pathhash
        return String.format(
                "%s/%s-%s/%s%%%08X",
                src.elementId().toString(),
                src.port().hasName() ? src.port().name() : src.port().toStringWithoutName(),
                dst.elementId().toString(),
                dst.port().hasName() ? dst.port().name() : dst.port().toStringWithoutName(),
                path.hashCode()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof WdmPath) {
            WdmPath that = (WdmPath) obj;
            return Objects.equals(src, that.src) &&
                    Objects.equals(dst, that.dst) &&
                    Objects.equals(path, that.path) &&
                    Objects.equals(osnr, that.osnr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, path, osnr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("src", src)
                .add("dst", dst)
                .add("path", path)
                .add("osnr", osnr)
                .toString();
    }
}
