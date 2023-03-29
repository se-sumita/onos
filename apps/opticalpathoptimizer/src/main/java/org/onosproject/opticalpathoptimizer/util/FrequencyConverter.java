package org.onosproject.opticalpathoptimizer.util;

import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;

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
     * @param lowestFrequencyTHz Lowest frequency(THz) that determines the '1' in the frequency ID
     */
    public FrequencyConverter(double lowestFrequencyTHz) {
        Frequency spacing = CHANNEL_SPACING; // 50GHz
        Frequency min = Frequency.ofTHz(lowestFrequencyTHz);
        long startMultiplier = min.subtract(Spectrum.CENTER_FREQUENCY).asHz() / spacing.asHz();
        idDelta = (-(int) startMultiplier) + 1;
    }

    /**
     * Get channel ID.
     * @param ochSignal Signal information
     * @return Channel ID
     */
    public int channelId(OchSignal ochSignal) {
        checkNotNull(ochSignal);
        checkArgument(ochSignal.gridType() == GridType.DWDM);
        checkArgument(CHANNEL_SPACING.equals(ochSignal.channelSpacing().frequency()));
        return ochSignal.spacingMultiplier() + idDelta;
    }

    /**
     * Get center frequency from signal information.
     * @param ochSignal Signal information
     * @return Center frequency
     */
    public static Frequency frequency(OchSignal ochSignal) {
        checkNotNull(ochSignal);
        return ochSignal.centralFrequency();
    }
}
