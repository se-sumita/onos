package org.onosproject.drivers.openroadm.common;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.optical.OpticalDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.stream.LongStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link LambdaQuery}.
 */
public class DefaultRoadmLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Set<OchSignal> queryLambdas(PortNumber portNumber) {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceId);
        Port port = deviceService.getPort(deviceId, portNumber);
        if (device.is(OpticalDevice.class)) {
            OpticalDevice opticalDevice = device.as(OpticalDevice.class);
            if (opticalDevice.portIs(port, OmsPort.class)) {
                OmsPort omsPort = (OmsPort) opticalDevice.port(port);
                Frequency spacing = omsPort.grid();
                Frequency min = omsPort.minFrequency().add(spacing.floorDivision(2));
                Frequency max = omsPort.maxFrequency().subtract(spacing.floorDivision(2));
                long startMultiplier = min.subtract(Spectrum.CENTER_FREQUENCY).asHz() / spacing.asHz();
                long endMultiplier = max.subtract(Spectrum.CENTER_FREQUENCY).asHz() / spacing.asHz();

                ChannelSpacing channelSpacing;
                if (ChannelSpacing.CHL_50GHZ.frequency().equals(spacing)) {
                    channelSpacing = ChannelSpacing.CHL_50GHZ;
                } else if (ChannelSpacing.CHL_100GHZ.frequency().equals(spacing)) {
                    channelSpacing = ChannelSpacing.CHL_100GHZ;
                } else if (ChannelSpacing.CHL_25GHZ.frequency().equals(spacing)) {
                    channelSpacing = ChannelSpacing.CHL_25GHZ;
                } else if (ChannelSpacing.CHL_12P5GHZ.frequency().equals(spacing)) {
                    channelSpacing = ChannelSpacing.CHL_12P5GHZ;
                } else if (ChannelSpacing.CHL_6P25GHZ.frequency().equals(spacing)) {
                    channelSpacing = ChannelSpacing.CHL_6P25GHZ;
                } else {
                    channelSpacing = ChannelSpacing.CHL_50GHZ;
                }
                log.info("Query lambda port[{}/{}] min[{}] max[{}]",
                        device.id(), port.number().toLong(),
                        startMultiplier, endMultiplier);
                return LongStream.rangeClosed(startMultiplier, endMultiplier)
                        .mapToObj(i -> OchSignal.newDwdmSlot(channelSpacing, (int) i))
                        .collect(ImmutableSet.toImmutableSet());
            }
        }
        return Collections.emptySet();
    }
}