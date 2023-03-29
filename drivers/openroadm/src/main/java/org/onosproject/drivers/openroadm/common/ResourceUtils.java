package org.onosproject.drivers.openroadm.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility for template processing.
 */
public final class ResourceUtils {
    private static final Logger log = getLogger(ResourceUtils.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Object INIT_LOCK = new Object();

    private static boolean initialized = false;

    private ResourceUtils() {}

    /**
     * Read templates.
     * @param name template name
     * @return list of templates
     */
    public static List<Template> readTemplates(String name) {
        synchronized (INIT_LOCK) {
            if (!initialized) {
                Properties p = new Properties();
                //p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
                //p.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
                p.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
                p.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, System.getProperty("java.io.tmpdir"));
                try {
                    Velocity.init(p);
                    log.debug("Velocity.init {}", p);
                }  catch (Exception e) {
                    log.error("Velocity init exception", e);
                }
                initialized = true;
            }
        }

        InputStream is = ResourceUtils.class.getResourceAsStream(name);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        List<Template> templates = new ArrayList<>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    File tmpFile = null;
                    try {
                        tmpFile = ResourceUtils.createTempFileAsResource(line);
                        log.debug("create template {} -> {}", line, tmpFile.getPath());
                        Template template = Velocity.getTemplate(tmpFile.getName());
                        templates.add(template);
                    } catch (IOException e) {
                        log.error("exception", e);
                    } finally {
                        if (tmpFile != null && tmpFile.exists()) {
                            log.debug("remove template {}", tmpFile.getPath());
                            tmpFile.delete();
                        }
                    }
                }
            }
        } catch (ResourceNotFoundException | ParseErrorException | IOException e) {
            log.warn("Exception occurred", e);
        } catch (Exception e) {
            log.error("Exception occurred", e);
        }
        return templates;
    }

    /**
     * Load string from file resource.
     * @param name resource file name
     * @return loaded content
     */
    public static String loadResource(String name) {
        try {
            return IOUtils.toString(ResourceUtils.class.getResourceAsStream(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Load string map from json file resource.
     * @param file resource file name
     * @param field field name
     * @return string map
     */
    public static Map<String, String> mapFromJsonFile(String file, String field) {
        InputStream is = ResourceUtils.class.getResourceAsStream(file);
        try {
            JsonNode obj = MAPPER.readTree(is);
            if (obj.has(field)) {
                return mapFromJson(obj.get(field));
            }
        } catch (IOException e) {
            log.warn("exception", e);
        }
        return new TreeMap<>();
    }

    private static Map<String, String> mapFromJson(JsonNode node) {
        Map<String, String> map = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            map.put(entry.getKey(), entry.getValue().asText());
        }
        return map;
    }

    private static File createTempFileAsResource(String resourceName) throws IOException {
        InputStream is = ResourceUtils.class.getResourceAsStream(resourceName);
        Path tmpPath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "drivers-", ".xml");
        File tmpFile = tmpPath.toFile();
        FileUtils.copyInputStreamToFile(is, tmpFile);
        return tmpFile;
    }
}
