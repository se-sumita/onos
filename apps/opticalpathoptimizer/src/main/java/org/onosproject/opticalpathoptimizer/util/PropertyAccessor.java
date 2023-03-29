package org.onosproject.opticalpathoptimizer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.onlab.util.Tools;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility to access the Dictionary provided by ComponentConfiguration.
 */
public class PropertyAccessor {
    private static final Logger log = getLogger(PropertyAccessor.class);

    private final Dictionary<String, Object> properties;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructor.
     * @param context Context
     */
    public PropertyAccessor(ComponentContext context) {
        this.properties = context.getProperties();
    }

    public String getAsString(String fieldName, String defaultValue) {
        String value = get(fieldName);
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

    public double getAsDouble(String fieldName, double defaultValue) {
        String value = get(fieldName);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            log.warn("JSON value error. field[{}] value[{}] use default value[{}]",
                    fieldName, value, defaultValue);
            return defaultValue;
        }
    }

    private String get(String fieldName) {
        if (properties == null) {
            return null;
        }
        return Tools.get(properties, fieldName);
    }

    public <T> Map<String, T> getAsMap(String fieldName, String defaultJson) {
        String json = get(fieldName);
        if (json != null) {
            try {
                return parseJsonMap(json);
            } catch (IOException ex) {
                log.warn("JSON parse error. field[{}] json[{}] use default value[{}] error: {}",
                        fieldName, json, defaultJson, ex.getMessage());
            }
        }
        try {
            return checkNotNull(parseJsonMap(defaultJson));
        } catch (IOException ex) {
            log.error("Invalid default JSON", ex);
            return Maps.newHashMap();
        }
    }

    public Map<String, Double> getMergedDoubleMap(String fieldName, String defaultJson) {
        Map<String, Double> map = getMergedMap(fieldName, defaultJson);
        castToDoubleMap(map);
        return map;
    }

    public <T extends Number> Map<String, T> getMergedMap(String fieldName, String defaultJson) {
        Map<String, T> map;
        try {
            map = checkNotNull(parseJsonMap(defaultJson));
        } catch (IOException ex) {
            log.error("Invalid default JSON. field[{}] json[{}] error[{}]",
                      fieldName, defaultJson, ex);
            map = Maps.newHashMap();
        }

        String json = get(fieldName);
        if (json != null) {
            Map<String, T> target;
            try {
                target = parseJsonMap(json);
            } catch (IOException ex) {
                log.warn("JSON parse error. field[{}] json[{}] use default value[{}] error: {}",
                         fieldName, json, defaultJson, ex.getMessage());
                return map;
            }

            map.putAll(target);
        }
        return map;
    }

    public List<String> getList(String fieldName, String defaultJson) {
        String json = get(fieldName);
        List<String> parsed = null;
        if (json != null) {
            try {
                parsed = parseJsonList(json);
            } catch (IOException ex) {
                log.warn("JSON parse error. field[{}] json[{}] use default value[{}] error[{}]",
                         fieldName, json, defaultJson, ex.getMessage());
            }
            if (parsed != null) {
                return parsed;
            }
        }
        try {
            parsed = checkNotNull(parseJsonList(defaultJson));
        } catch (IOException ex) {
            log.error("Invalid default JSON. field[{}] json[{}] error[{}]",
                      fieldName, defaultJson, ex);
            parsed = Collections.emptyList();
        }
        return parsed;
    }

    private <T> Map<String, T> parseJsonMap(String json) throws IOException {
        TypeReference<LinkedHashMap<String, T>> typeRef
                = new TypeReference<LinkedHashMap<String, T>>() { };
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (IOException ex) {
            // The default value in @Component property specification is
            // obtained as escaped, so try to parse it after unescaping.
            String unescapedJson = json.replace("\\\"", "\"");
            return MAPPER.readValue(unescapedJson, typeRef);
        }
    }

    private <T> List<T> parseJsonList(String json) throws IOException {
        TypeReference<ArrayList<T>> typeRef
                = new TypeReference<ArrayList<T>>() { };
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (IOException ex) {
            // The default value in @Component property specification is
            // obtained as escaped, so try to parse it after unescaping.
            String unescapedJson = json.replace("\\\"", "\"");
            return MAPPER.readValue(unescapedJson, typeRef);
        }
    }

    public static <T> String toJson(T map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Generate json error", e);
        }
        return "";
    }

    // Explicitly check and cast because Jackson does not convert to Double
    // in some cases.
    public static void castToDoubleMap(Map<String, Double> map) {
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof Double)) {
                entry.setValue(castToDouble(value));
            }
        }
    }

    public static Double castToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }
}
