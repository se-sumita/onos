package org.onosproject.opticalpathoptimizer.model;

import org.onosproject.net.openroadm.model.OchParam;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapping of rate / modulation-format to Q-value / Q-lower threshold.
 */
public class QWithParams extends LinkedHashMap<OchParam, QValue> {
    public QWithParams() {}

    public QWithParams(Map<OchParam, QValue> s) {
        super(s);
    }
}
