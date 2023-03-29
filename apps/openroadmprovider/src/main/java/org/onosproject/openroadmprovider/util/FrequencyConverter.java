package org.onosproject.openroadmprovider.util;

import com.google.common.collect.Streams;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.Device;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.optical.OpticalDevice;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Converter for frequency and ID.
 */
public class FrequencyConverter {
    private static final Frequency CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ.frequency();
    private final int idDelta;

    /**
     * Constructor.
     * @param deviceService device service
     */
    public FrequencyConverter(DeviceService deviceService) {
        Optional<OmsPort> port = Streams.concat(
                Streams.stream(deviceService.getDevices(Device.Type.ROADM)),
                Streams.stream(deviceService.getDevices(Device.Type.ROADM_OTN)))
            .filter(d -> d.is(OpticalDevice.class))
            .map(d -> d.as(OpticalDevice.class))
            .flatMap(d -> deviceService.getPorts(d.id()).stream()
                            .filter(p -> d.portIs(p, OmsPort.class))
                            .map(p -> (OmsPort) d.port(p)))
            .findFirst();

        checkArgument(port.isPresent(),
                "Could not convert frequency to id. Not found a OMS port in any ROADM device.");

        OmsPort omsPort = port.get();
        Frequency spacing = CHANNEL_SPACING; // 50GHz fixed
        Frequency min = omsPort.minFrequency().add(spacing.floorDivision(2));
        long startMultiplier = min.subtract(Spectrum.CENTER_FREQUENCY).asHz() / spacing.asHz();

        idDelta = (-(int) startMultiplier) + 1;
    }

    /**
     * Obtain frequency ID.
     * @param ochSignal wavelength information
     * @return frequency ID
     */
    public int channelId(OchSignal ochSignal) {
        checkNotNull(ochSignal);
        checkArgument(ochSignal.gridType() == GridType.DWDM);
        checkArgument(CHANNEL_SPACING.equals(ochSignal.channelSpacing().frequency()));
        return ochSignal.spacingMultiplier() + idDelta;
    }
}
