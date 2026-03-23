// File: OclImportInitializer.java
package org.openelisglobal.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Legacy class for manual OCL import. OCL import is now handled automatically
 * by OclConfigurationHandler via ConfigurationInitializationService.
 * 
 * This class is kept for backward compatibility and manual import scenarios
 * (e.g., REST endpoints).
 */
@Component
public class OclImportInitializer {
    private static final Logger log = LoggerFactory.getLogger(OclImportInitializer.class);

    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    private OclConfigurationHandler oclConfigurationHandler;

    /**
     * Public method to trigger OCL import manually. This can be called from REST
     * endpoints or for manual imports.
     * 
     * Note: This method processes ZIP files directly. For automatic loading via
     * ConfigurationInitializationService, OCL ZIP files should be placed in
     * /var/lib/openelis-global/configuration/backend/ocl/
     * 
     * @param fileDir Directory containing OCL ZIP files
     */
    public void performOclImport(String fileDir) {
        log.info("OCL Import: Manual import triggered from directory: {}", fileDir);
        Path configDir = Paths.get(fileDir);
        if (!Files.exists(configDir)) {
            log.warn("OCL Import: Directory does not exist: {}", fileDir);
            return;
        }
        File[] zipFiles = configDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            log.info("OCL Import: No ZIP files found in directory: {}", fileDir);
            return;
        }
        List<JsonNode> oclNodes = new ArrayList<>();
        for (File file : zipFiles) {
            try {
                log.info("OCL Import: Processing ZIP file: {}", file.getName());
                oclZipImporter.importOclZip(file.getAbsolutePath(), oclNodes);
            } catch (IOException e) {
                log.error("OCL Import: Failed to import ZIP file: {}", file.getName(), e);
            }
        }
        if (!oclNodes.isEmpty()) {
            // Use the handler's performImport method
            oclConfigurationHandler.performImport(oclNodes);
        }
    }
}
