package org.openelisglobal.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OclZipImporter {
    private static final Logger log = LoggerFactory.getLogger(OclZipImporter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Imports and parses the OCL ZIP package from the configurations/ocl directory.
     *
     * @return List of JsonNode objects representing parsed JSON/CSV files.
     * @throws IOException if the import fails.
     */
    public List<JsonNode> importOclZip(String file, List<JsonNode> nodes) throws IOException {

        log.info("Found OCL ZIP package: {}", file);

        importOclPackage(file, nodes);
        log.info("Finished importing OCL package. Parsed {} nodes.", nodes.size());

        return nodes;
    }

    /**
     * Imports an OCL package from the specified path.
     *
     * @param zipPath Path to the ZIP file.
     * @return List of JsonNode objects from JSON/CSV files.
     * @throws IOException if the file doesn't exist or parsing fails.
     */
    List<JsonNode> importOclPackage(String zipPath, List<JsonNode> jsonNodes) throws IOException {
        File file = new File(zipPath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("OCL package not found or invalid at: " + zipPath);
        }

        try (ZipFile zipFile = new ZipFile(file)) {
            log.debug("ZIP file details: size={} bytes, entries={}", file.length(), zipFile.size());

            zipFile.stream().forEach(entry -> {
                try {
                    log.info("Processing ZIP entry: {}", entry.getName());
                    if (entry.isDirectory()) {
                        log.debug("Skipping directory: {}", entry.getName());
                        return;
                    }

                    JsonNode node = null;
                    if (entry.getName().endsWith(".json")) {
                        log.info("Parsing as JSON: {}", entry.getName());
                        node = parseJsonEntry(zipFile, entry);
                    } else {
                        log.warn("Skipping unsupported file type: {}", entry.getName());
                        return;
                    }

                    if (node != null) {
                        jsonNodes.add(node);
                        log.info("Successfully parsed entry: {}. Node added to list.", entry.getName());
                    } else {
                        log.warn("Parsing returned null for entry: {}", entry.getName());
                    }
                } catch (Exception e) {
                    log.error("Error parsing entry: {}", entry.getName(), e);
                }
            });
        }

        return jsonNodes;
    }

    private JsonNode parseJsonEntry(ZipFile zipFile, ZipEntry entry) {
        try {
            log.debug("Parsing JSON file: {}", entry.getName());
            JsonNode node = objectMapper.readTree(zipFile.getInputStream(entry));
            log.debug("JSON structure: isArray={}, fields={}", node.isArray(),
                    node.isObject() ? node.fieldNames().next() : "N/A");
            return node;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + entry.getName(), e);
        }
    }
}
