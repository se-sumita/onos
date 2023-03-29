package org.onosproject.drivers.openroadm;

import org.onlab.packet.ChassisId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.optical.OpticalDevice;
import org.onosproject.net.provider.ProviderId;

import java.util.Optional;

public class MockOpticalDevice implements OpticalDevice, Behaviour {
    @Override
    public <T extends Port> boolean portIs(Port port, Class<T> portClass) {
        return true;
    }

    @Override
    public <T extends Port> Optional<T> portAs(Port port, Class<T> portClass) {
        return Optional.of((T) port);
    }

    @Override
    public Port port(Port port) {
        return port;
    }

    @Override
    public DeviceId id() {
        return null;
    }

    @Override
    public Type type() {
        return null;
    }

    @Override
    public String manufacturer() {
        return null;
    }

    @Override
    public String hwVersion() {
        return null;
    }

    @Override
    public String swVersion() {
        return null;
    }

    @Override
    public String serialNumber() {
        return null;
    }

    @Override
    public ChassisId chassisId() {
        return null;
    }

    @Override
    public Annotations annotations() {
        return null;
    }

    @Override
    public ProviderId providerId() {
        return null;
    }

    @Override
    public DriverData data() {
        return null;
    }

    @Override
    public void setData(DriverData data) {

    }

    @Override
    public <B extends Behaviour> B as(Class<B> projectionClass) {
        return null;
    }

    @Override
    public <B extends Behaviour> boolean is(Class<B> projectionClass) {
        return false;
    }
}
