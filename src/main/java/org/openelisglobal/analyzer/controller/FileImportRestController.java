package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.service.FileImportService;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for File Import Configuration management Handles CRUD
 * operations for file-based analyzer import configurations
 */
@RestController
@RequestMapping("/rest/analyzer/file-import")
public class FileImportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(FileImportRestController.class);

    @Value("${file.import.base.directory:/data/analyzer-imports}")
    private String baseImportDir;

    @Autowired
    private FileImportService fileImportService;

    /**
     * Validates that a directory path is within the configured base import
     * directory. Uses NIO Path.startsWith() which correctly handles path component
     * boundaries (unlike String.startsWith which is vulnerable to sibling directory
     * attacks).
     *
     * @param path The directory path to validate (null/empty returns true —
     *             optional field)
     * @return true if the path is within the base directory, false otherwise
     */
    private boolean isPathWithinBase(String path) {
        if (path == null || path.trim().isEmpty()) {
            return true;
        }
        try {
            Path basePath = Paths.get(baseImportDir).normalize().toAbsolutePath();
            Path targetPath = Paths.get(path).normalize().toAbsolutePath();
            return targetPath.startsWith(basePath);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * GET /rest/analyzer/file-import/configurations Retrieve all file import
     * configurations
     */
    @GetMapping("/configurations")
    public ResponseEntity<List<FileImportConfiguration>> getAllConfigurations(
            @RequestParam(required = false) Boolean active) {
        try {
            List<FileImportConfiguration> configurations;
            if (active != null && active) {
                configurations = fileImportService.getAllActive();
            } else {
                configurations = fileImportService.getAll();
            }
            return ResponseEntity.ok(configurations);
        } catch (Exception e) {
            logger.error("Error retrieving file import configurations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /rest/analyzer/file-import/configurations/{id} Retrieve file import
     * configuration by ID
     */
    @GetMapping("/configurations/{id}")
    public ResponseEntity<FileImportConfiguration> getConfiguration(@PathVariable String id) {
        try {
            FileImportConfiguration configuration = fileImportService.get(id);
            if (configuration == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(configuration);
        } catch (Exception e) {
            logger.error("Error retrieving file import configuration: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /rest/analyzer/file-import/configurations/analyzer/{analyzerId} Retrieve
     * file import configuration by analyzer ID
     */
    @GetMapping("/configurations/analyzer/{analyzerId}")
    public ResponseEntity<FileImportConfiguration> getConfigurationByAnalyzerId(@PathVariable Integer analyzerId) {
        try {
            Optional<FileImportConfiguration> configuration = fileImportService.getByAnalyzerId(analyzerId);
            if (configuration.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(configuration.get());
        } catch (Exception e) {
            logger.error("Error retrieving file import configuration for analyzer: " + analyzerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /rest/analyzer/file-import/configurations Create new file import
     * configuration
     */
    @PostMapping("/configurations")
    public ResponseEntity<Map<String, Object>> createConfiguration(@RequestBody FileImportConfiguration configuration,
            HttpServletRequest request) {
        try {
            // Validation
            if (configuration.getAnalyzerId() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (configuration.getImportDirectory() == null || configuration.getImportDirectory().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Import directory is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Validate directory paths are within base import directory
            if (!isPathWithinBase(configuration.getImportDirectory())
                    || !isPathWithinBase(configuration.getArchiveDirectory())
                    || !isPathWithinBase(configuration.getErrorDirectory())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Directory paths must be within " + baseImportDir);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Check for duplicate analyzer ID
            Optional<FileImportConfiguration> existing = fileImportService
                    .getByAnalyzerId(configuration.getAnalyzerId());
            if (existing.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error",
                        "File import configuration already exists for analyzer: " + configuration.getAnalyzerId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            configuration.setSysUserId(getSysUserId(request));
            String id = fileImportService.insert(configuration);

            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("message", "File import configuration created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error creating file import configuration", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /rest/analyzer/file-import/configurations/{id} Update file import
     * configuration
     */
    @PutMapping("/configurations/{id}")
    public ResponseEntity<Map<String, Object>> updateConfiguration(@PathVariable String id,
            @RequestBody FileImportConfiguration configuration, HttpServletRequest request) {
        try {
            FileImportConfiguration existing = fileImportService.get(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate directory paths are within base import directory
            if (!isPathWithinBase(configuration.getImportDirectory())
                    || !isPathWithinBase(configuration.getArchiveDirectory())
                    || !isPathWithinBase(configuration.getErrorDirectory())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Directory paths must be within " + baseImportDir);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Update fields
            existing.setImportDirectory(configuration.getImportDirectory());
            existing.setFilePattern(configuration.getFilePattern());
            existing.setArchiveDirectory(configuration.getArchiveDirectory());
            existing.setErrorDirectory(configuration.getErrorDirectory());
            existing.setColumnMappings(configuration.getColumnMappings());
            existing.setDelimiter(configuration.getDelimiter());
            existing.setHasHeader(configuration.getHasHeader());
            existing.setActive(configuration.getActive());
            existing.setSysUserId(getSysUserId(request));

            fileImportService.update(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "File import configuration updated successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating file import configuration: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /rest/analyzer/file-import/configurations/{id} Delete file import
     * configuration
     */
    @DeleteMapping("/configurations/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfiguration(@PathVariable String id,
            HttpServletRequest request) {
        try {
            FileImportConfiguration configuration = fileImportService.get(id);
            if (configuration == null) {
                return ResponseEntity.notFound().build();
            }

            fileImportService.delete(id, getSysUserId(request));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "File import configuration deleted successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error deleting file import configuration: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
