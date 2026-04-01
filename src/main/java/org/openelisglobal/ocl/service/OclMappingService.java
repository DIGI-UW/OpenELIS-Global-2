package org.openelisglobal.ocl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Service for mapping OCL test names to sample types and test sections.
 * 
 * Loads mapping data from a JSON file at startup and provides simple lookup. If
 * a test name is not found in the mapping, returns default values.
 * 
 * This ensures that known tests get medically accurate sample type and
 * department assignments, while unknown tests gracefully fall back to defaults.
 */
@Service
public class OclMappingService {

    private static final Log log = LogFactory.getLog(OclMappingService.class);

    @Value("${org.openelisglobal.ocl.mapping.file:ocl-test-mapping.json}")
    private String mappingFileName;

    @Value("${org.openelisglobal.ocl.import.default.sampletype:Whole Blood}")
    private String defaultSampleType;

    @Value("${org.openelisglobal.ocl.import.default.testsection:Hematology}")
    private String defaultTestSection;

    // In-memory mapping store: normalized test name -> mapping entry
    private Map<String, MappingEntry> mappingCache = new HashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        loadMappingFile();
    }

    /**
     * Loads the mapping JSON file from classpath into memory.
     */
    private void loadMappingFile() {
        try {
            ClassPathResource resource = new ClassPathResource(mappingFileName);
            if (!resource.exists()) {
                log.warn("OCL mapping file not found: " + mappingFileName + ". Using defaults for all tests.");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);

                // Load defaults from file if present
                if (root.has("defaults")) {
                    JsonNode defaults = root.get("defaults");
                    if (defaults.has("sampleType")) {
                        defaultSampleType = defaults.get("sampleType").asText();
                    }
                    if (defaults.has("testSection")) {
                        defaultTestSection = defaults.get("testSection").asText();
                    }
                }

                // Load mappings
                if (root.has("mappings")) {
                    JsonNode mappings = root.get("mappings");
                    int count = 0;
                    for (Map.Entry<String, JsonNode> entry : mappings.properties()) {
                        String testName = entry.getKey();
                        String normalized = normalizeName(testName);
                        JsonNode mapping = entry.getValue();

                        String sampleType = mapping.has("sampleType") ? mapping.get("sampleType").asText()
                                : defaultSampleType;
                        String testSection = mapping.has("testSection") ? mapping.get("testSection").asText()
                                : defaultTestSection;

                        mappingCache.put(normalized, new MappingEntry(sampleType, testSection));
                        count++;
                    }
                    log.info("Loaded " + count + " OCL test mappings from " + mappingFileName);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load OCL mapping file: " + e.getMessage(), e);
        }
    }

    /**
     * Normalizes a test name for lookup: lowercase, trimmed, multiple spaces
     * collapsed.
     */
    private String normalizeName(String name) {
        if (name == null)
            return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Looks up sample type and test section for a given test name.
     * 
     * Priority: 1. Exact match in mapping file (normalized) 2. Default values from
     * configuration
     * 
     * @param testName The test name from OCL concept
     * @return MappingEntry containing sample type and test section
     */
    public MappingEntry getMapping(String testName) {
        if (testName == null || testName.trim().isEmpty()) {
            return new MappingEntry(defaultSampleType, defaultTestSection);
        }

        String normalized = normalizeName(testName);
        MappingEntry entry = mappingCache.get(normalized);

        if (entry != null) {
            log.debug("Found mapping for test '" + testName + "': sampleType=" + entry.getSampleType()
                    + ", testSection=" + entry.getTestSection());
            return entry;
        }

        // Not found - use defaults
        log.debug("No mapping found for test '" + testName + "'. Using defaults: sampleType=" + defaultSampleType
                + ", testSection=" + defaultTestSection);
        return new MappingEntry(defaultSampleType, defaultTestSection);
    }

    /**
     * Returns the default sample type.
     */
    public String getDefaultSampleType() {
        return defaultSampleType;
    }

    /**
     * Returns the default test section.
     */
    public String getDefaultTestSection() {
        return defaultTestSection;
    }

    /**
     * Immutable entry representing a single test mapping.
     */
    public static class MappingEntry {
        private final String sampleType;
        private final String testSection;

        public MappingEntry(String sampleType, String testSection) {
            this.sampleType = sampleType;
            this.testSection = testSection;
        }

        public String getSampleType() {
            return sampleType;
        }

        public String getTestSection() {
            return testSection;
        }

        @Override
        public String toString() {
            return "MappingEntry{sampleType='" + sampleType + "', testSection='" + testSection + "'}";
        }
    }
}
