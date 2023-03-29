/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.config.basics;

import com.google.common.collect.Streams;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.config.SubjectFactory;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Set of subject factories for potential configuration subjects.
 */
public final class SubjectFactories {

    // Construction forbidden
    private SubjectFactories() {
    }

    // Required for resolving application identifiers
    private static CoreService coreService;

    /**
     * Application ID subject factory.
     */
    public static final SubjectFactory<ApplicationId> APP_SUBJECT_FACTORY =
            new SubjectFactory<ApplicationId>(ApplicationId.class, "apps") {
                @Override
                public ApplicationId createSubject(String key) {
                    return coreService.registerApplication(key);
                }

                @Override
                public String subjectKey(ApplicationId subject) {
                    return subject.name();
                }
            };

    /**
     * Device ID subject factory.
     */
    public static final SubjectFactory<DeviceId> DEVICE_SUBJECT_FACTORY =
            new SubjectFactory<DeviceId>(DeviceId.class, "devices") {
                @Override
                public DeviceId createSubject(String key) {
                    return DeviceId.deviceId(key);
                }
            };

    /**
     * Connect point subject factory.
     */
    public static final SubjectFactory<ConnectPoint> CONNECT_POINT_SUBJECT_FACTORY =
            new SubjectFactory<ConnectPoint>(ConnectPoint.class, "ports") {
                @Override
                public ConnectPoint createSubject(String key) {
                    return ConnectPoint.deviceConnectPoint(key);
                }

                @Override
                public String subjectKey(ConnectPoint subject) {
                    return key(subject);
                }
            };

    /**
     * Host ID subject factory.
     */
    public static final SubjectFactory<HostId> HOST_SUBJECT_FACTORY =
            new SubjectFactory<HostId>(HostId.class, "hosts") {
                @Override
                public HostId createSubject(String key) {
                    return HostId.hostId(key);
                }
            };

    /**
     * Link key subject factory.
     */
    public static final SubjectFactory<LinkKey> LINK_SUBJECT_FACTORY =
            new SubjectFactory<LinkKey>(LinkKey.class, "links") {
                // {device_name}:{port_name}={device_name}:{port_name}
                private Pattern portNamePattern = Pattern.compile(
                        "^(?<devId1>.+?)\\s*:\\s*(?<port1>[^:=]+)" +
                        "\\s*=\\s*(?<devId2>.+?)\\s*:\\s*(?<port2>[^:=]+)$"
                );
                // {device_name}/[{port_name}]({port_num})={device_name}/[{port_name}]({port_num})
                private Pattern portIdPattern = Pattern.compile(
                        "^(?<devId1>[^\\[]+?)/\\[(?<port1>.+?)]\\(\\d+\\)" +
                        "-(?<devId2>[^\\[]+?)/\\[(?<port2>.+?)]\\(\\d+\\)$"
                );

                @Override
                public LinkKey createSubject(String key) {
                    Matcher matcher = portNamePattern.matcher(key);
                    boolean matched = matcher.matches();
                    if (!matched) {
                        matcher = portIdPattern.matcher(key);
                        matched = matcher.matches();
                    }
                    if (matched) {
                        String deviceId1 = matcher.group("devId1");
                        String portName1 = matcher.group("port1");
                        String deviceId2 = matcher.group("devId2");
                        String portName2 = matcher.group("port2");

                        // Device Name -> Device ID
                        DeviceService service = DefaultServiceDirectory.getService(DeviceService.class);
                        DeviceId device1 = getDevice(service, deviceId1);
                        checkArgument(device1 != null, "Not found device: %s", deviceId1);
                        DeviceId device2 = getDevice(service, deviceId2);
                        checkArgument(device2 != null, "Not found device: %s", deviceId2);

                        // Port Name -> Port
                        Port port1 = getPort(service, device1, portName1);
                        checkArgument(port1 != null, "Not found port: %s in %s", portName1, device1);
                        Port port2 = getPort(service, device2, portName2);
                        checkArgument(port2 != null, "Not found port: %s in %s", portName2, device2);

                        return LinkKey.linkKey(new ConnectPoint(device1, port1.number()),
                                               new ConnectPoint(device2, port2.number()));
                    }

                    String[] cps = key.split("-");
                    checkArgument(cps.length == 2, "Incorrect link key format: %s", key);
                    return LinkKey.linkKey(ConnectPoint.deviceConnectPoint(cps[0]),
                            ConnectPoint.deviceConnectPoint(cps[1]));
                }

                @Override
                public String subjectKey(LinkKey subject) {
                    return key(subject.src()) + "-" + key(subject.dst());
                }

                private DeviceId getDevice(DeviceService service, String deviceId) {
                    Device device = service.getDevice(DeviceId.deviceId(deviceId));
                    if (device == null) {
                        Optional<Device> dev = Streams.stream(service.getDevices())
                                .filter(d -> deviceId.equals(d.annotations().value(AnnotationKeys.NAME)))
                                .findFirst();
                        if (dev.isPresent()) {
                            device = dev.get();
                        }
                    }
                    return device == null ? null : device.id();
                }

                private Port getPort(DeviceService service, DeviceId deviceId, String portName) {
                    Optional<Port> port = service.getPorts(deviceId)
                            .stream()
                            .filter(p -> portName.equals(p.number().name())
                                    || portName.equals(String.valueOf(p.number().toLong()))
                                    || portName.equals(p.annotations().value(AnnotationKeys.PORT_NAME)))
                            .findFirst();
                    if (port.isPresent()) {
                        return port.get();
                    } else {
                        return null;
                    }
                }
            };

    /**
     * Region ID subject factory.
     */
    public static final SubjectFactory<RegionId> REGION_SUBJECT_FACTORY =
            new SubjectFactory<RegionId>(RegionId.class, "regions") {
                @Override
                public RegionId createSubject(String key) {
                    return RegionId.regionId(key);
                }
            };

    /**
     * UI Topology layout ID subject factory.
     */
    public static final SubjectFactory<UiTopoLayoutId> LAYOUT_SUBJECT_FACTORY =
            new SubjectFactory<UiTopoLayoutId>(UiTopoLayoutId.class, "layouts") {
                @Override
                public UiTopoLayoutId createSubject(String key) {
                    return UiTopoLayoutId.layoutId(key);
                }
            };

    /**
     * Provides reference to the core service, which is required for
     * application subject factory.
     *
     * @param service core service reference
     */
    public static synchronized void setCoreService(CoreService service) {
        coreService = service;
    }

    private static String key(ConnectPoint subject) {
        return subject.deviceId() + "/" + subject.port();
    }

}
