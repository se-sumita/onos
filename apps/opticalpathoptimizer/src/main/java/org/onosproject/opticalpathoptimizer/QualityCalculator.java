package org.onosproject.opticalpathoptimizer;

import com.google.common.base.MoreObjects;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.openroadm.model.AmpType;
import org.onosproject.net.openroadm.model.FiberType;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.opticalpathoptimizer.api.QualityCalculationPropertyService;
import org.onosproject.opticalpathoptimizer.model.Amp;
import org.onosproject.opticalpathoptimizer.model.Element;
import org.onosproject.opticalpathoptimizer.model.Fiber;
import org.onosproject.opticalpathoptimizer.model.OpenRoadmModelLink;
import org.onosproject.opticalpathoptimizer.model.PreAmpFiber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.onosproject.cli.AbstractShellCommand.get;

// CHECKSTYLE:OFF

/** Quality Calculator. */
public class QualityCalculator {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private OpenRoadmConfigHelper openRoadmConfigHelper;
    private QualityCalculationPropertyService property;

    private static final double PREAMPFIBER_POUT = 4.0;
    private static final double NUM_CH = 10.0;
    private static final double CH_SPACING = (double) ChannelSpacing.CHL_50GHZ.frequency().asHz();

    public static QualityCalculator create() {
        return create(OpenRoadmConfigHelper.create());
    }

    public static QualityCalculator create(OpenRoadmConfigHelper helper) {
        QualityCalculationPropertyService propertyService = get(QualityCalculationPropertyService.class);
        return new QualityCalculator(helper, propertyService);
    }

    public QualityCalculator(OpenRoadmConfigHelper helper, QualityCalculationPropertyService property) {
        this.openRoadmConfigHelper = helper;
        this.property = property;
    }

    private final class AmpParam {
        private double _G;
        private double _nsp;

        /**
         * Characteristic values of the amplifier.
         * @param ampType Amplifier type
         * @param G Gain
         */
        private AmpParam(AmpType ampType, double G) {
            this._G = G;
            this._nsp = property.getNoiseFigure(ampType);
        }

        private double get_G() {
            return _G;
        }

        private double get_nsp() {
            return _nsp;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("G", _G)
                    .add("nsp", _nsp)
                    .toString();
        }
    }

    private final class SpanParam {
        private double _Pout;
        private double _Loss;
        private double _L;
        private double _Aeff;
        private double _n2;
        private double _CD;

        /**
         * Characteristic values of the fiber.
         * @param fiberType Fiber type
         * @param spanLoss Span loss [dB]
         * @param srlgLen SRLG length [km]
         */
        private SpanParam(FiberType fiberType, double spanLoss, double srlgLen) {
            this._Pout = property.getPout(fiberType);            // [dBm]
            this._Loss = spanLoss / (srlgLen * 1.0E-3);          // [dB/km]
            this._L = srlgLen * 1.0E-3;                          // [km]
            this._Aeff = property.getAeff(fiberType) * 1.0E-12;  // [m^2]
            this._n2 = property.getN2(fiberType);                // [m^2/W]
            this._CD = property.getCd(fiberType) * 1.0E-3;       // [ps/nm/m]
        }

        /**
         * Characteristic values of the fiber (fir PreAmpFiber).
         * @param pout Pout
         */
        private SpanParam(double pout) {
            this._Pout = pout;
            this._Loss = 0.0;
            this._L = 0.0;
            this._Aeff = 1.0;
            this._n2 = 1.0;
            this._CD = 1.0;
        }

        private double get_PoutW() {
            return toLinear(_Pout) / 1000.0; // [dBm] -> [W]
        }
        private double get_Loss() {
            return _Loss;
        }
        private double get_L() {
            return _L;
        }
        private double get_Aeff() {
            return _Aeff;
        }
        private double get_n2() {
            return _n2;
        }
        private double get_CD() {
            return _CD;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("Pout", _Pout)
                    .add("Loss", _Loss)
                    .add("Len", _L)
                    .add("Aeff", _Aeff)
                    .add("N2", _n2)
                    .add("CD", _CD)
                    .toString();
        }
    }

    private final class SignalParam {
        private double _lambda;
        private double _f;
        private double _SR;
        private double _DP;

        /**
         * Parameters of signal wavelength
         * @param sol Speed of light
         * @param f Center frequency [Hz]
         * @param rate Rate type [Gbps]
         * @param msType Modulation-format
         */
        private SignalParam(double sol, double f, double rate, ModulationFormat msType) {
            _lambda = sol / f;
            _f = f;
            _DP = property.getPowerSpectral(msType);
            _SR = rate / (_DP * property.getBitSymbol(msType)) * 0.001;
        }

        private double get_lambda() {
            return _lambda;
        }
        private double get_f() {
            return _f;
        }
        private double get_SR() {
            return _SR;
        }
        private double get_DP() {
            return _DP;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("Lambda", _lambda)
                    .add("Frequency", _f)
                    .add("SR", _SR)
                    .add("DP", _DP)
                    .toString();
        }
    }


    /*------------------------------------------------------------------------*/
    /**
     * Calculate the amount of nonlinear interference noise.
     * @param spanParams  Span characteristic value
     * @param ampParams   Amplifier characteristic value
     * @param signalParam Signal characteristic value
     * @return Nonlinear interference noise level (P_NLI_ch)
     */
    private double calcPNLI(List<SpanParam> spanParams, List<AmpParam> ampParams, SignalParam signalParam) {
        double sum_Gs = 0.0;
        double sum_Leff = 0.0;
        for (int s = 0; s < spanParams.size(); ++s) {
            sum_Gs += calcSpectralDensity(spanParams, ampParams, signalParam, s);
            sum_Leff += Leff(spanParams.get(s));
            log.trace("sum_Gs={}", sum_Gs);
        }
        sum_Gs = (16.0 / 27.0) * sum_Gs;
        sum_Gs = (0.0121 * sum_Leff - 0.0744) * sum_Gs;
        return property.getDeltaF().asHz() * sum_Gs / 1.0E+12;
    }

    /**
     * Calculate the power spectral density.
     * @param spanParams  Span characteristic value
     * @param ampParams   Amplifier characteristic value
     * @param ch Signal characteristic value
     * @param s Span number to be calculated
     * @return Power spectral density of span number
     */
    private double calcSpectralDensity(List<SpanParam> spanParams, List<AmpParam> ampParams, SignalParam ch, int s) {

        // (γ * Leff)^2
        double s1 = Math.pow((ganma(spanParams.get(s), ch.get_lambda()) * Leff(spanParams.get(s))), 2);

        SpanParam sp;
        AmpParam  ap;
        // Π{Γ^3 * e^(-6*α*L)}  k=1: k <= s-1
        double s2 = 1.0;
        for (int k = 0; k < s; ++k) {
            sp = spanParams.get(k);
            ap = ampParams.get(k + 1);
            s2 *= Math.pow(toLinear(sp.get_Loss() * sp.get_L()), 3) * Math.exp(-6.0 * alpha(sp) * sp.get_L());
        }

        // Π{Γ * e^(-2*α*L)}  k=s-1; k <= Ns  Ns: Number of spans
        double s3 = 1.0;
        for (int k = s; k < spanParams.size(); ++k) {
            sp = spanParams.get(k);
            ap = ampParams.get(k + 1);
            s3 *= toLinear(sp.get_Loss() * sp.get_L()) * Math.exp(-2.0 * alpha(sp) * sp.get_L());
        }

        // G^3 * asinh((π^2)/2 * {2*α}^(-1) * |β| * (Bch)^2) / (2π * {2α}^(-1) * |β2|)
        sp = spanParams.get(s);
        double Pout = sp.get_PoutW(); // [W]
        double s4 = Math.pow(spectral(ch, Pout), 3) * Psi(sp, ch);
        log.trace("s1={} s2={} s3={} s4={} Pout={}", s1, s2, s3, s4, Pout);

        return s1 * s2 * s3 * s4;
    }

    /**
     * Power spectral density at the center frequency of the Xth signal wavelength (Gx).
     * @param x Signal wavelength number
     * @param Pout Optical fiber input power per wavelength
     * @return Power spectral density (Gx)
     */
    private double spectral(SignalParam x, double Pout) {
        log.trace("spectral() DP={} Pout={} SR={}", x.get_DP(), Pout, x.get_SR());
        return Pout / x.get_SR();
    }

    /**
     * (Gn)^2 * Gch * (2 - delta) * ψs,n,ch.
     * @param sp Span characteristic value
     * @param Pout Optical fiber input power per wavelength [W]
     * @param n Nth signal wavelength characteristic value
     * @param ch CHth signal wavelength characteristic value
     * @param delta δ function (δn,ch : 1 only when n=ch, 0 otherwise)
     * @return (Gn)^2 * Gch * (2 - delta) * ψs,n,ch
     */
    private double spectralDensity(SpanParam sp, double Pout, SignalParam n, SignalParam ch, int delta) {
        double gn3 = Math.pow(spectral(ch, Pout), 3);
        log.trace("spectralDensity() gn3={}", gn3);
        return gn3 * Psi(sp, ch);
    }

    /**
     * ψs,n,ch.
     * @param sp Span characteristic value
     * @param ch CHth signal wavelength characteristic value
     * @return ψs,n,ch
     */
    private double Psi(SpanParam sp, SignalParam ch) {
        double psi = 0.0;
        double pi2 = Math.pow(Math.PI, 2.0);
        double alphaBeta = Math.abs(beta(sp, ch.get_lambda())) / (2.0 * alpha(sp));
        double b = ch.get_SR();
        double b1 = 0.0;
        double b2 = 0.0;
        double f0 = ch.get_f();
        double fi = 0.0;
        for (double i = -NUM_CH; i <= NUM_CH; ++i) {
            if (i != 0.0) {
                fi = f0 + CH_SPACING * i;
                b1 = (fi - f0 + b / 2.0) * b;
                b2 = (fi - f0 - b / 2.0) * b;
                psi += (asinh(pi2 * alphaBeta * b1) - asinh(pi2 * alphaBeta * b2)) / (4.0 * Math.PI * alphaBeta);
            } else {
                psi += 2.0 * (asinh(pi2 / 2.0 * alphaBeta * Math.pow(b, 2.0))) / (2.0 * Math.PI * alphaBeta);
            }
        }
        log.trace("Psi() pi2={} alphaBeta={} b={}", pi2, alphaBeta, b);
        return psi;
    }

    /**
     * Inverse hyperbolic function.
     * @param v value
     * @return sinh^-1(v)
     */
    private double asinh(double v) {
        if (v < 0.0) {
            return -1.0 * (Math.log(-1.0 * v + Math.sqrt((-1.0 * v) * (-1.0 * v) + 1.0)));
        } else {
            double asinh = Math.log(v + Math.sqrt(v * v + 1.0));
            log.trace("asinh() return {}", asinh);
            return asinh;
        }
    }

    /**
     * Group velocity dispersion of a fiber in a span.
     * @param sp Span characteristic value
     * @param lambda Signal wavelength
     * @return Group velocity dispersion (β)
     */
    private double beta(SpanParam sp, double lambda) {
        return (-1.0 * (Math.pow(lambda, 2.0) / (2.0 * Math.PI * property.getSpeedOfLight())) * sp.get_CD() * 1.0E+24);
    }

    /**
     * Non-linear constants of fiber in a span.
     * @param sp Span characteristic value
     * @param lambda Signal wavelength
     * @return Non-linear constant (γ)
     */
    private double ganma(SpanParam sp, double lambda) {
        return (2.0 * Math.PI * sp.get_n2()) / (lambda * sp.get_Aeff()) * 1000.0;
    }

    /**
     * Fiber loss in a span.
     * @param sp Span characteristic value
     * @return Loss (α)
     */
    private double alpha(SpanParam sp) {
        return (sp.get_Loss() / 20.0) * Math.log(10.0);
    }

    /**
     * Effective length of fiber in span.
     * @param sp Span characteristic value
     * @return Effective length (L_eff)
     */
    private double Leff(SpanParam sp) {
        return (1 - exp(sp)) / (2.0 * alpha(sp));
    }

    /**
     * e^(-2αL).
     * @param sp Span characteristic value
     * @return e^(-2αL)
     */
    private double exp(SpanParam sp) {
        return Math.exp(-2.0 * alpha(sp) * sp.get_L());
    }

    /*------------------------------------------------------------------------*/
    /**
     * OSNR value.
     * @param spanParams Span characteristic value
     * @param ampParams Amplifier characteristic value
     * @param ch Signal wavelength characteristic value
     * @return OSNR
     */
    private double calcOSNR(List<SpanParam> spanParams, List<AmpParam> ampParams, SignalParam ch) {
        double noiseSignalRatio = 0.0;
        for (int k = 0; k < ampParams.size(); ++k) {
            double P_OUT = spanParams.get(k).get_PoutW(); // [W]
            double P_ASE = calcPASE(ch, ampParams.get(k));
            noiseSignalRatio += (P_ASE / P_OUT);
        }
        return 1.0 / noiseSignalRatio;
    }

    /**
     * The amount of optical noise produced by the Kth amplifier for a given signal.
     * @param ch Signal characteristic value
     * @param ap Characteristic value of the Kth amplifier
     * @return P_ASE(ch, k)
     */
    private double calcPASE(SignalParam ch, AmpParam ap) {
        return (toLinear(ap.get_G()) - 1.0) * toLinear(ap.get_nsp())
                * property.getPlanckConstant() * property.getDeltaF().asHz() * ch.get_f();
    }

    private double toLinear(double dBVolume) {
        return Math.pow(10.0, dBVolume / 10.0);
    }

    private double toDB(double linearVolume) {
        return 10 * Math.log10(linearVolume);
    }

    /*------------------------------------------------------------------------*/
    /**
     * Calculate total OSNR value.
     * @param path Path
     * @param rate Rate
     * @param modulationFormat Modulation-format
     * @return Total OSNR value
     */
    public double calcTotalOSNR(Path path, Rate rate, ModulationFormat modulationFormat) {
        double f = property.getUserFrequency().asHz();
        double rateValue = property.getRateValue(rate);
        log.trace("path={}", path);
        log.trace("f={} rate={} msType={}", f, rate, modulationFormat);
        SignalParam signalParam = new SignalParam(property.getSpeedOfLight(), f, rateValue, modulationFormat);
        List<SpanParam> spanParamsForOSNR = new ArrayList<>();
        List<SpanParam> spanParams = new ArrayList<>();
        List<AmpParam> ampParams = new ArrayList<>();

        for (Link l : path.links()) {
            log.trace("link={}", l.toString());
            OpenRoadmModelLink openRoadmModelLink = openRoadmConfigHelper.getLinkDetail(l.src(), l.dst());

            for (Element e : openRoadmModelLink.getSectionElements()) {
                if (e instanceof Amp) {
                    AmpType ampType = ((Amp) e).getAmpType();
                    double gain = ((Amp) e).getGain();
                    ampParams.add(new AmpParam(ampType, gain));
                } else if (e instanceof PreAmpFiber) {
                    spanParamsForOSNR.add(new SpanParam(PREAMPFIBER_POUT));
                } else if (e instanceof Fiber) {
                    FiberType fiberType = ((Fiber) e).getFiberType();
                    double spanLoss = ((Fiber) e).getSpanLoss();
                    double srlgLen = ((Fiber) e).getSrlgLen(); // [m]
                    SpanParam spanParam = new SpanParam(fiberType, spanLoss, srlgLen / 1.000 /* m -> km */);
                    spanParamsForOSNR.add(spanParam);
                    spanParams.add(spanParam);
                }
            }
        }
        if (log.isTraceEnabled()) {
            spanParams.forEach(s -> log.trace("spanParam={}", s.toString()));
            ampParams.forEach(a -> log.trace("ampParam={}", a.toString()));
            log.trace("signalParam={} {} {} {}",
                      signalParam.get_lambda(), signalParam.get_DP(),
                      signalParam.get_f(), signalParam.get_SR());
        }
        double osnr = calcOSNR(spanParamsForOSNR, ampParams, signalParam);
        double total_P_ASE = 0.0;
        for (AmpParam ampParam : ampParams) {
            total_P_ASE += calcPASE(signalParam, ampParam);
        }
        double P_NLI = calcPNLI(spanParams, ampParams, signalParam);

        log.trace("osnr={} total_P_ASE={} P_NLI={}", osnr, total_P_ASE, P_NLI);
        return toDB((osnr * total_P_ASE) / (total_P_ASE + P_NLI));
    }

    /*------------------------------------------------------------------------*/

    /**
     * Calculate Q-value.
     * @param constants Constants
     * @param osnr OSNR value [dB]
     * @return Q-value [dB]
     */
    public Double calcQ(List<Double> constants, double osnr) {
        double result = 0.0;
        int i = constants.size() - 1;

        /* Q = C4*OSNR^4 + C3*OSNR^3 + C2*OSNR^2 + C1*OSNR^1 + C0 */
        for (double c : constants) {
            if (i == 0) {
                result += c;
            } else {
                result += (c * Math.pow(osnr, i));
            }
            i--;
            log.debug("result[{}]={}, {}, {}", i, result, c, osnr);
        }

        return result;
    }
}

// CHECKSTYLE:ON
