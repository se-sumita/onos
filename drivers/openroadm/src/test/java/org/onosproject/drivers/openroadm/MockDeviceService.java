package org.onosproject.drivers.openroadm;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceServiceAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MockDeviceService extends DeviceServiceAdapter {

    private List<Device> deviceList = new LinkedList<>();
    private List<Port> portList = new LinkedList<>();

    public void addDevice(Device device) {
        this.deviceList.add(device);
    }

    public void addPorts(List<Port> ports) {
        this.portList.addAll(ports);
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        return this.deviceList.stream()
                .filter(d -> d.id().equals(deviceId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        return this.portList.stream()
                .filter(p -> p.element().id().equals(deviceId))
                .collect(Collectors.toList());
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return getPorts(deviceId).stream()
                .filter(port -> portNumber.equals(port.number()))
                .findFirst().orElse(null);
    }
}
