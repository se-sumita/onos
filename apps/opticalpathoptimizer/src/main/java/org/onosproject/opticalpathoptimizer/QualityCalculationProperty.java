package org.onosproject.opticalpathoptimizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.util.Frequency;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.openroadm.model.AmpType;
import org.onosproject.net.openroadm.model.FiberType;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.opticalpathoptimizer.api.QualityCalculationPropertyService;
import org.onosproject.opticalpathoptimizer.util.PropertyAccessor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.opticalpathoptimizer.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;


@Component(
        immediate = true,
        service = { QualityCalculationPropertyService.class },
        property = {
                METHOD + ":String=" + DEFAULT_METHOD,
                RATE_MAP + ":String=" + DEFAULT_RATE_MAP,
                BIT_SYMBOL_MAP + ":String=" + DEFAULT_BIT_SYMBOL,
                POWER_SPECTRAL_MAP + ":String=" + DEFAULT_POWER_SPECTRAL,
                NOISE_FIGURES + ":String=" + DEFAULT_NOISE_FIGURES,
                AEFF + ":String=" + DEFAULT_AEFF,
                N2 + ":String=" + DEFAULT_N2,
                CD + ":String=" + DEFAULT_CD,
                POUT + ":String=" + DEFAULT_POUT,
                PREAMP_POUT + ":String=" + DEFAULT_PREAMP_POUT,
                PLANCK_CONSTANT + ":Double=" + DEFAULT_PLANCK_CONSTANT,
                SOL + ":Double=" + DEFAULT_SOL,
                DELTA_F + ":Double=" + DEFAULT_DELTA_F,
                USER_FREQUENCY + ":Double=" + DEFAULT_USER_FREQUENCY,
                RATE_MOD_FORMAT_PATTERN + ":String=" + DEFAULT_RATE_MOD_FORMAT_PATTERN,
                OSNR_Q_CONSTANTS_MAP + ":String=" + DEFAULT_OSNR_Q_CONSTANTS_MAP,
                Q_THRESHOLD_MAP + ":String=" + DEFAULT_Q_THRESHOLD_MAP,
                WORKING_DIRECTORY + ":String=" + DEFAULT_WORKING_DIRECTORY,
                COMMAND + ":String=" + DEFAULT_COMMAND,
        }
)
@SuppressWarnings("checkstyle:MemberName")
public class QualityCalculationProperty implements QualityCalculationPropertyService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /* ---------------------------------------------------------------------- *
     *  Quality calculation method
     * ---------------------------------------------------------------------- */

    /** Quality calculation method. */
    String method = METHOD_GNPY;

    /* ---------------------------------------------------------------------- *
     *  Transmission rate
     * ---------------------------------------------------------------------- */
    /** Transmission rate. */
    String rateMap = DEFAULT_RATE_MAP;

    private Map<Rate, Double> rateMapValues;

    /* ---------------------------------------------------------------------- *
     *  Modulation format and characteristic values
     * ---------------------------------------------------------------------- */

    /** bit/Symbol number for each modulation method. */
    String bitSymbolMap = DEFAULT_BIT_SYMBOL;

    private Map<ModulationFormat, Double> bitSymbolMapValues;

    /** Power spectral for each modulation method. */
    String powerSpectralMap = DEFAULT_POWER_SPECTRAL;

    private Map<ModulationFormat, Double> powerSpectralMapValues;

    /* ---------------------------------------------------------------------- *
     *  Amplifier type and characteristic values
     * ---------------------------------------------------------------------- */
    /** Noise figure for each amplifier type. [dB] */
    String noiseFigures = DEFAULT_NOISE_FIGURES;

    private Map<AmpType, Double> ampNoiseFigureMap;

    /* ---------------------------------------------------------------------- *
     *  Fiber type and characteristic values
     * ---------------------------------------------------------------------- */
    /** Effective area (cross section) for each fiber type. [um^2] */
    String Aeff = DEFAULT_AEFF;

    private Map<FiberType, Double> fiberAeffMap;

    /** Nonlinear refractive index for each fiber type. [m^2/W] */
    String N2 = DEFAULT_N2;

    private Map<FiberType, Double> fiberN2Map;

    /** Wavelength dispersion for each fiber type. [ps/nm/km] */
    String CD = DEFAULT_CD;

    private Map<FiberType, Double> fiberCdMap;

    /** Input power for each fiber type (Pout). [dBm]" */
    String Pout = DEFAULT_POUT;

    private Map<FiberType, Double> fiberPoutMap;

    /** Pre-Amp's input power (Pout). [dBm] */
    double preAmpPout = DEFAULT_PREAMP_POUT;

    /* ---------------------------------------------------------------------- *
     *  Physical constants
     * ---------------------------------------------------------------------- */
    /** Planck constant. [Ws] */
    private double planckConstant = DEFAULT_PLANCK_CONSTANT;

    /** Speed of light. [m/s] */
    private double SoL = DEFAULT_SOL;

    /* ---------------------------------------------------------------------- *
     *  Quality calculation constants
     * ---------------------------------------------------------------------- */
    /** Noise bandwidth for P_ASE. [GHz] */
    private double deltaF = DEFAULT_DELTA_F;

    /** Signal's frequency for calculation total OSNR. [THz] */
    private double userFrequency = DEFAULT_USER_FREQUENCY;

    /** Rate/Modulation-format pairs for OSNR/Q calculation. */
    String rateModFormatPattern = DEFAULT_RATE_MOD_FORMAT_PATTERN;

    private List<OchParam> rateModFormatPatternList;

    /** Q-value calculate parameter for each vendor/rate/modulation-format. */
    String osnrQConstantsMap = DEFAULT_OSNR_Q_CONSTANTS_MAP;

    private Map<String, Map<String, List<Double>>> osnrQConstantsMapValues;

    /** Lower thresholds of Q-value. */
    String qThresholdMap = DEFAULT_Q_THRESHOLD_MAP;

    private Map<String, Map<String, Double>> qThresholdMapValues;

    /* ---------------------------------------------------------------------- *
     *  Settings related to GNPy integration
     * ---------------------------------------------------------------------- */

    /** Working directory for executing external command. */
    String workingDirectory;

    /** External command. */
    String command = DEFAULT_COMMAND;

    /*------------------------------------------------------------------------*/

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());

        modified(context);
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
    }

    @Modified
    public void modified(ComponentContext context) {
        PropertyAccessor properties = new PropertyAccessor(context);

        // Quality calculation method
        String method = properties.getAsString(METHOD, METHOD_GNPY);
        if (METHOD_INTERNAL.equals(method) || METHOD_GNPY.equals(method)) {
            this.method = method;
        } else {
            log.error("Parameter 'method' must be '{}' or '{}'", METHOD_INTERNAL, METHOD_GNPY);
            cfgService.setProperty(getClass().getCanonicalName(), METHOD, this.method);
        }
        log.info("method={}", this.method);

        // Transmission rate
        Map<String, Double> map = properties.getMergedDoubleMap(RATE_MAP, DEFAULT_RATE_MAP);
        rateMapValues = map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Rate.valueOf(e.getKey()),
                        Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
        rateMap = PropertyAccessor.toJson(rateMapValues);
        log.info("rateMap={}", rateMapValues);

        // bitSymbol
        map = properties.getMergedDoubleMap(BIT_SYMBOL_MAP, DEFAULT_BIT_SYMBOL);
        bitSymbolMapValues = map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> ModulationFormat.valueOf(e.getKey()),
                        Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
        bitSymbolMap = PropertyAccessor.toJson(bitSymbolMapValues);
        log.info("bitSymbolMap={}", bitSymbolMapValues);

        // powerSpectral
        map = properties.getMergedDoubleMap(POWER_SPECTRAL_MAP, DEFAULT_POWER_SPECTRAL);
        powerSpectralMapValues = map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> ModulationFormat.valueOf(e.getKey()),
                        Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
        powerSpectralMap = PropertyAccessor.toJson(powerSpectralMapValues);
        log.info("powerSpectralMap={}", powerSpectralMapValues);

        //-------------------------------------------
        // Amplifier
        //-------------------------------------------
        // Noise figure per amplifier [dB]
        map = properties.getMergedDoubleMap(NOISE_FIGURES, DEFAULT_NOISE_FIGURES);
        ampNoiseFigureMap = map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> AmpType.valueOf(e.getKey()),
                        Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
        noiseFigures = PropertyAccessor.toJson(ampNoiseFigureMap);
        log.info("noiseFigures={}", ampNoiseFigureMap);

        //-------------------------------------------
        // Fiber
        //-------------------------------------------
        // Effective cross-sectional area [um^2]
        fiberAeffMap = toFiberTypeKeyMap(properties.getMergedDoubleMap(AEFF, DEFAULT_AEFF));
        Aeff = PropertyAccessor.toJson(fiberAeffMap);
        log.info("Aeff={}", fiberAeffMap);

        // Nonlinear refractive index [m^2/W]
        fiberN2Map = toFiberTypeKeyMap(properties.getMergedDoubleMap(OsgiPropertyConstants.N2, DEFAULT_N2));
        N2 = PropertyAccessor.toJson(fiberN2Map);
        log.info("N2={}", fiberN2Map);

        // Wavelength dispersion [ps/nm/km]
        fiberCdMap = toFiberTypeKeyMap(properties.getMergedDoubleMap(OsgiPropertyConstants.CD, DEFAULT_CD));
        CD = PropertyAccessor.toJson(fiberCdMap);
        log.info("CD={}", fiberCdMap);

        // Input power per wavelength for each fiber type (Pout) [dBm]
        fiberPoutMap = toFiberTypeKeyMap(properties.getMergedDoubleMap(POUT, DEFAULT_POUT));
        Pout = PropertyAccessor.toJson(fiberPoutMap);
        log.info("Pout={}", fiberPoutMap);

        // Input power per wavelength of PreAmp (Pout) [dBm]
        preAmpPout = properties.getAsDouble(PREAMP_POUT, DEFAULT_PREAMP_POUT);
        log.info("preAmpPout={}", preAmpPout);

        //-------------------------------------------
        // Physical constants
        //-------------------------------------------
        // Planck constant [Ws]
        planckConstant = properties.getAsDouble(PLANCK_CONSTANT, DEFAULT_PLANCK_CONSTANT);
        log.info("planckConstant={}", planckConstant);

        // Speed of light [m/s]
        SoL = properties.getAsDouble("SoL", DEFAULT_SOL);
        log.info("SoL={}", SoL);

        //-------------------------------------------
        // Quality calculation constants
        //-------------------------------------------
        // Optical signal bandwidth [GHz]
        deltaF = properties.getAsDouble(DELTA_F, DEFAULT_DELTA_F);
        log.info("deltaF={}", deltaF);

        // Representative center frequency [THz]
        userFrequency = properties.getAsDouble(USER_FREQUENCY, DEFAULT_USER_FREQUENCY);
        log.info("userFrequency={}", userFrequency);

        // List of combinations of rate and modulation-format to calculate OSNR
        List<String> list = properties.getList(
                RATE_MOD_FORMAT_PATTERN, DEFAULT_RATE_MOD_FORMAT_PATTERN
        );
        rateModFormatPatternList = list.stream()
                .filter(e -> e.indexOf('/') > 0)
                .map(e -> {
                    String[] s = e.split("/", 2);
                    return OchParam.of(Rate.valueOf(s[0]), ModulationFormat.valueOf(s[1]));
                })
                .collect(Collectors.toList());

        list = rateModFormatPatternList.stream()
                .map(p -> p.rate().name() + "/" + p.modulationFormat().name())
                .collect(Collectors.toList());
        rateModFormatPattern = PropertyAccessor.toJson(list);
        log.info("rateModFormatPattern={}", rateModFormatPatternList);

        // Values used in the OSNR-Q value conversion equation
        osnrQConstantsMapValues = Maps.newLinkedHashMap();
        Map<String, Map<String, List<Number>>> qConstants = properties.getAsMap(
                OSNR_Q_CONSTANTS_MAP, DEFAULT_OSNR_Q_CONSTANTS_MAP
        );
        for (Map.Entry<String, Map<String, List<Number>>> constantsEntry : qConstants.entrySet()) {
            Map<String, List<Double>> constants = Maps.newLinkedHashMap();
            for (Map.Entry<String, List<Number>> entry : constantsEntry.getValue().entrySet()) {
                constants.put(
                        entry.getKey(),
                        entry.getValue().stream().map(PropertyAccessor::castToDouble).collect(Collectors.toList())
                );
            }
            osnrQConstantsMapValues.put(constantsEntry.getKey(), constants);
        }
        osnrQConstantsMap = PropertyAccessor.toJson(osnrQConstantsMapValues);
        log.info("osnrQConstantsMap={}", osnrQConstantsMapValues);

        // Lower thresholds of Q-value
        qThresholdMapValues = properties.getAsMap(Q_THRESHOLD_MAP, DEFAULT_Q_THRESHOLD_MAP);
        for (Map<String, Double> thresholds : qThresholdMapValues.values()) {
            PropertyAccessor.castToDoubleMap(thresholds);
        }
        qThresholdMap = PropertyAccessor.toJson(qThresholdMapValues);
        log.info("qThresholdMap={}", qThresholdMapValues);

        //-------------------------------------------
        // Settings related to GNPy integration
        //-------------------------------------------
        // Working directory
        workingDirectory = properties.getAsString(WORKING_DIRECTORY, DEFAULT_WORKING_DIRECTORY);
        log.info("workingDirectory={}", workingDirectory);

        // Command string
        command = properties.getAsString(COMMAND, DEFAULT_COMMAND);
        log.info("command={}", command);
    }

    /**
     * Get quality calculation method.
     * @return Quality calculation method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Get the transmission rate as a double type value.
     * @param rate Transmission rate
     * @return Transmission rate [Gbps]
     */
    public double getRateValue(Rate rate) {
        return rateMapValues.getOrDefault(rate, 100.0);
    }

    /**
     * Get bit/symbol.
     * @param format Modulation format
     * @return Bit/Symbol
     */
    public double getBitSymbol(ModulationFormat format) {
        return bitSymbolMapValues.getOrDefault(format, 1.0);
    }

    /**
     * Get power spectral factor.
     * @param format Modulation-format
     * @return Power spectral factor
     */
    public double getPowerSpectral(ModulationFormat format) {
        return powerSpectralMapValues.getOrDefault(format, 1.0);
    }

    /**
     * Get noise figure.
     * @param ampType Amplifier type
     * @return Noise figure [dB]
     */
    public double getNoiseFigure(AmpType ampType) {
        checkNotNull(ampType);
        Double value = null;
        if (!ampNoiseFigureMap.containsKey(ampType)) {
            log.warn("Illegal config detected. Amplifier type = {}", ampType);
        } else {
            value = ampNoiseFigureMap.get(ampType);
        }
        if (value == null) { // Use the first if available
            Optional<Double> v = ampNoiseFigureMap.values().stream().findFirst();
            value = v.orElse(Double.NaN);
        }
        return value;
    }

    /**
     * Get noise figure map.
     * @return Noise figure map
     */
    public Map<AmpType, Double> getNoiseFigureMap() {
        return ampNoiseFigureMap;
    }

    /**
     * Get effective cross-sectional area.
     * @param fiberType Fiber type
     * @return Effective cross-sectional area [um^2]
     */
    public double getAeff(FiberType fiberType) {
        return verifiedValueForFiberType(fiberAeffMap, fiberType);
    }

    /**
     * Get effective cross-sectional area map.
     * @return Effective cross-sectional area map
     */
    public Map<FiberType, Double> getAeffMap() {
        return fiberAeffMap;
    }

    /**
     * Get nonlinear refractive index.
     * @param fiberType Fiber type
     * @return Nonlinear refractive index [m^2/W]
     */
    public double getN2(FiberType fiberType) {
        return verifiedValueForFiberType(fiberN2Map, fiberType);
    }

    /**
     * Get nonlinear refractive index map.
     * @return Nonlinear refractive index map
     */
    public Map<FiberType, Double> getN2Map() {
        return fiberN2Map;
    }

    /**
     * Get wavelength dispersion.
     * @param fiberType Fiber type
     * @return Wavelength dispersion [ps/nm/km]
     */
    public double getCd(FiberType fiberType) {
        return verifiedValueForFiberType(fiberCdMap, fiberType);
    }

    /**
     * Get wavelength dispersion map.
     * @return Wavelength dispersion map
     */
    public Map<FiberType, Double> getCdMap() {
        return fiberCdMap;
    }

    /**
     * Get input power per wavelength for each fiber type.
     * @param fiberType Fiber type
     * @return Input power per wavelength for each fiber type [dBm]
     */
    public double getPout(FiberType fiberType) {
        return verifiedValueForFiberType(fiberPoutMap, fiberType);
    }

    /**
     * Get input power per wavelength for each fiber type map.
     * @return Input power per wavelength for each fiber type map
     */
    public Map<FiberType, Double> getPoutMap() {
        return fiberPoutMap;
    }

    /**
     * Get input power per wavelength of PreAmp. [dBm]
     * @return Input power per wavelength of PreAmp [dBm]
     */
    public double getPreAmpPout() {
        return preAmpPout;
    }

    /**
     * Get Planck constant.
     * @return Planck constant [Ws]
     */
    public double getPlanckConstant() {
        return planckConstant;
    }

    /**
     * Get speed of light.
     * @return Speed of light [m/s]
     */
    public double getSpeedOfLight() {
        return SoL;
    }

    /**
     * Get optical signal bandwidth.
     * @return Optical signal bandwidth [Hz]
     */
    public Frequency getDeltaF() {
        return Frequency.ofGHz(deltaF);
    }

    /**
     * Get representative center frequency for OSNR calculation.
     * @return Representative center frequency [Hz]
     */
    public Frequency getUserFrequency() {
        return Frequency.ofTHz(userFrequency);
    }

    /**
     * Get list of combinations of rate and modulation-format to calculate OSNR.
     * @return List of combinations of rate and modulation-format to calculate OSNR
     */
    public List<OchParam> getRateModFormatPatternList() {
        return rateModFormatPatternList;
    }

    /**
     * Get value used in the OSNR-Q value conversion equation by vendor/rate/modulation-format.
     * @param vendor Vendor
     * @param rate Rate
     * @param modFormat Modulation-format
     * @return Parameters of Q-value conversion
     */
    public List<Double> getOsnrQConstants(String vendor, Rate rate, ModulationFormat modFormat) {
        Map<String, List<Double>> constants = osnrQConstantsMapValues.get(vendor);
        if (constants != null) {
            String key = rate.name() + "/" + modFormat.name();
            return constants.get(key);
        }
        return null;
    }

    /**
     * Get values used in the OSNR-Q value conversion equation map.
     * @return Values used in the OSNR-Q value conversion equation map
     */
    public Map<String, Map<String, List<Double>>> getOsnrQConstantsMap() {
        return osnrQConstantsMapValues;
    }

    /**
     * Lower threshold of Q-value by vendor/rate/modulation-format.
     * @param vendor Vendor
     * @param rate Rate
     * @param modFormat Modulation-format
     * @return Lower thresholds of Q-value
     */
    public double getQThreshold(String vendor, Rate rate, ModulationFormat modFormat) {
        Map<String, Double> thresholds = qThresholdMapValues.get(vendor);
        if (thresholds != null) {
            String key = rate.name() + "/" + modFormat.name();
            Double threshold =  thresholds.get(key);
            if (threshold != null) {
                return threshold;
            }
        }
        return Double.NaN;
    }

    /**
     * Get lower thresholds of Q-value map.
     * @return Lower thresholds of Q-value map
     */
    public Map<String, Map<String, Double>> getQThresholdMap() {
        return qThresholdMapValues;
    }

    /**
     * Get list of pairs of rate/modulation-format for OSNR calculation.
     * @return List of pairs of rate/modulation-format for OSNR calculation
     */
    public Collection<OchParam> getRateAndModulationFormatForCalcOsnr() {
        List<OchParam> params = Lists.newArrayListWithCapacity(rateModFormatPatternList.size());
        for (OchParam param : rateModFormatPatternList) {
            if (!rateMapValues.containsKey(param.rate())) {
                log.warn("Rate[" + param.rate() + "] is not in RateMap");
                continue;
            }
            params.add(param);
        }
        return params;
    }

    /**
     * Get working directory for executing external command.
     * @return working directory path
     */
    @Override
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Get command string.
     * @return Command string
     */
    @Override
    public String getCommand() {
        return command;
    }

    private double verifiedValueForFiberType(Map<FiberType, Double> map, FiberType fiberType) {
        Double value = null;
        checkNotNull(fiberType);
        if (!map.containsKey(fiberType)) {
            log.warn("Illegal config detected. Fiber type = {}", fiberType.name());
        } else {
            value = map.get(fiberType);
        }
        if (value == null) { // Use the first if available
            Optional<Double> v = map.values().stream().findFirst();
            value = v.orElse(Double.NaN);
        }
        return value;
    }

    private Map<FiberType, Double> toFiberTypeKeyMap(Map<String, Double> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> FiberType.valueOf(e.getKey()),
                        Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

}
