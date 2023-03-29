package org.onosproject.opticalpathoptimizer;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {}

    // for OpticalPathOptimizer
    public static final String K = "K";
    public static final int DEFAULT_K = 3;

    public static final String LOWEST_FREQ_THZ_FOR_ID = "lowestFrequencyTHzForCalcId";
    public static final double MIN_LOWEST_FREQ_THZ_FOR_ID = 186.20;
    public static final double DEFAULT_LOWEST_FREQ_THZ_FOR_ID = 191.35;

    // for QualityCalculationProperty
    public static final String METHOD = "method";
    public static final String METHOD_INTERNAL = "internal";
    public static final String METHOD_GNPY = "gnpy";
    public static final String DEFAULT_METHOD = "gnpy";

    public static final String RATE_MAP = "rateMap";
    @SuppressWarnings("checkstyle:LineLength")
    public static final String DEFAULT_RATE_MAP = "{\"R200G\":200.0,\"R150G\":150.0,\"R100G\":100.0,\"R10.7G\":10.7,\"R11.1G\":11.1}";

    public static final String BIT_SYMBOL_MAP = "bitSymbolMap";
    @SuppressWarnings("checkstyle:LineLength")
    public static final String DEFAULT_BIT_SYMBOL = "{\"bpsk\":1.0,\"dc-dp-bpsk\":2.0,\"qpsk\":2.0,\"dp-qpsk\":4.0,\"qam16\":4.0,\"dp-qam16\":8.0,\"dc-dp-qam16\":8.0,\"qam8\":3.0,\"dp-qam8\":6.0,\"dc-dp-qam8\":6.0}";

    public static final String POWER_SPECTRAL_MAP = "powerSpectralMap";
    @SuppressWarnings("checkstyle:LineLength")
    public static final String DEFAULT_POWER_SPECTRAL = "{\"bpsk\":1.0,\"dc-dp-bpsk\":0.5,\"qpsk\":1.0,\"dp-qpsk\":0.5,\"qam16\":1.0,\"dp-qam16\":0.5,\"dc-dp-qam16\":0.5,\"qam8\":1.0,\"dp-qam8\":0.5,\"dc-dp-qam8\":0.5}";

    public static final String NOISE_FIGURES = "noiseFigures";
    public static final String DEFAULT_NOISE_FIGURES = "{\"LowGainAmp\":8.8,\"HighGainAmp\":6.6}";

    public static final String AEFF = "Aeff";
    public static final String DEFAULT_AEFF = "{\"smf\":72,\"dsf\":45}";

    public static final String N2 = "N2";
    public static final String DEFAULT_N2 = "{\"smf\":2.6e-20,\"dsf\":2.6e-20}";

    public static final String CD = "CD";
    public static final String DEFAULT_CD = "{\"smf\":16,\"dsf\":0.0001}";

    public static final String POUT = "Pout";
    public static final String DEFAULT_POUT = "{\"smf\":1,\"dsf\":-5}";

    public static final String PREAMP_POUT = "preAmpPout";
    public static final double DEFAULT_PREAMP_POUT = 4.0;

    public static final String PLANCK_CONSTANT = "planckConstant";
    public static final double DEFAULT_PLANCK_CONSTANT = 6.62607004E-34;

    public static final String SOL = "SoL";
    public static final double DEFAULT_SOL = 299792458;

    public static final String DELTA_F = "deltaF";
    public static final double DEFAULT_DELTA_F = 12.5;

    public static final String USER_FREQUENCY = "userFrequency";
    public static final double DEFAULT_USER_FREQUENCY = 193.1;

    public static final String RATE_MOD_FORMAT_PATTERN = "rateModFormatPattern";
    public static final String DEFAULT_RATE_MOD_FORMAT_PATTERN = "[\"R100G/dp-qpsk\",\"R200G/dp-qam16\"]";

    public static final String OSNR_Q_CONSTANTS_MAP = "osnrQConstantsMap";

    @SuppressWarnings("checkstyle:LineLength")
    public static final String DEFAULT_OSNR_Q_CONSTANTS_MAP = "{}";

    public static final String Q_THRESHOLD_MAP = "qThresholdMap";

    @SuppressWarnings("checkstyle:LineLength")
    public static final String DEFAULT_Q_THRESHOLD_MAP = "{}";

    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String DEFAULT_WORKING_DIRECTORY = "/tmp";

    public static final String COMMAND = "command";
    public static final String DEFAULT_COMMAND = "python3 gnpy.py";

}
