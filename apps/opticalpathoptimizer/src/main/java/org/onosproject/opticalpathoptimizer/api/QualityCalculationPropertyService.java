package org.onosproject.opticalpathoptimizer.api;

import org.onlab.util.Frequency;
import org.onosproject.net.openroadm.model.AmpType;
import org.onosproject.net.openroadm.model.FiberType;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.net.openroadm.model.Rate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service to provide quality calculation properties.
 */
public interface QualityCalculationPropertyService {
    /**
     * Get quality calculation method.
     * @return Quality calculation method
     */
    String getMethod();

    /**
     * Get the transmission rate as a double type value.
     * @param rate Transmission rate
     * @return Transmission rate [Gbps]
     */
    double getRateValue(Rate rate);

    /**
     * Get bit/symbol.
     * @param format Modulation format
     * @return Bit/Symbol
     */
    double getBitSymbol(ModulationFormat format);

    /**
     * Get power spectral factor.
     * @param format Modulation-format
     * @return Power spectral factor
     */
    double getPowerSpectral(ModulationFormat format);

    /**
     * Get noise figure.
     * @param ampType Amplifier type
     * @return Noise figure [dB]
     */
    double getNoiseFigure(AmpType ampType);

    /**
     * Get noise figure map.
     * @return Noise figure map
     */
    Map<AmpType, Double> getNoiseFigureMap();

    /**
     * Get effective cross-sectional area.
     * @param fiberType Fiber type
     * @return Effective cross-sectional area [um^2]
     */
    double getAeff(FiberType fiberType);

    /**
     * Get effective cross-sectional area map.
     * @return Effective cross-sectional area map
     */
    Map<FiberType, Double> getAeffMap();

    /**
     * Get nonlinear refractive index.
     * @param fiberType Fiber type
     * @return Nonlinear refractive index [m^2/W]
     */
    double getN2(FiberType fiberType);

    /**
     * Get nonlinear refractive index map.
     * @return Nonlinear refractive index map
     */
    Map<FiberType, Double> getN2Map();

    /**
     * Get wavelength dispersion.
     * @param fiberType Fiber type
     * @return Wavelength dispersion [ps/nm/km]
     */
    double getCd(FiberType fiberType);

    /**
     * Get wavelength dispersion map.
     * @return Wavelength dispersion map
     */
    Map<FiberType, Double> getCdMap();

    /**
     * Get input power per wavelength for each fiber type.
     * @param fiberType Fiber type
     * @return Input power per wavelength for each fiber type [dBm]
     */
    double getPout(FiberType fiberType);

    /**
     * Get input power per wavelength for each fiber type map.
     * @return Input power per wavelength for each fiber type map
     */
    Map<FiberType, Double> getPoutMap();

    /**
     * Get input power per wavelength of PreAmp. [dBm]
     * @return Input power per wavelength of PreAmp [dBm]
     */
    double getPreAmpPout();

    /**
     * Get Planck constant.
     * @return Planck constant [Ws]
     */
    double getPlanckConstant();

    /**
     * Get speed of light.
     * @return Speed of light [m/s]
     */
    double getSpeedOfLight();

    /**
     * Get optical signal bandwidth.
     * @return Optical signal bandwidth [Hz]
     */
    Frequency getDeltaF();

    /**
     * Get representative center frequency for OSNR calculation.
     * @return Representative center frequency [Hz]
     */
    Frequency getUserFrequency();

    /**
     * Get list of combinations of rate and modulation-format to calculate OSNR.
     * @return List of combinations of rate and modulation-format to calculate OSNR
     */
    List<OchParam> getRateModFormatPatternList();

    /**
     * Get value used in the OSNR-Q value conversion equation by vendor/rate/modulation-format.
     * @param vendor Vendor
     * @param rate Rate
     * @param modFormat Modulation-format
     * @return Parameters of Q-value conversion
     */
    List<Double> getOsnrQConstants(String vendor, Rate rate, ModulationFormat modFormat);

    /**
     * Get values used in the OSNR-Q value conversion equation map.
     * @return Values used in the OSNR-Q value conversion equation map
     */
    Map<String, Map<String, List<Double>>> getOsnrQConstantsMap();

    /**
     * Lower threshold of Q-value by vendor/rate/modulation-format.
     * @param vendor Vendor
     * @param rate Rate
     * @param modFormat Modulation-format
     * @return Lower thresholds of Q-value
     */
    double getQThreshold(String vendor, Rate rate, ModulationFormat modFormat);

    /**
     * Get lower thresholds of Q-value map.
     * @return Lower thresholds of Q-value map
     */
    Map<String, Map<String, Double>> getQThresholdMap();

    /**
     * Get list of pairs of rate/modulation-format for OSNR calculation.
     * @return List of pairs of rate/modulation-format for OSNR calculation
     */
    Collection<OchParam> getRateAndModulationFormatForCalcOsnr();

    /**
     * Get working directory for executing GNPy.
     * @return working directory path
     */
    String getWorkingDirectory();

    /**
     * Get command string.
     * @return Command string
     */
    String getCommand();
}
