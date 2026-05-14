package org.openelisglobal.analyzer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for FILE analyzer configuration.
 *
 * <p>
 * File PARSING is owned by the analyzer bridge. This service handles:
 * <ul>
 * <li>Profile-driven config creation (writes to Analyzer entity)
 * <li>Bridge registration sync
 * </ul>
 */
@Service
@Transactional
public class FileImportServiceImpl implements FileImportService {

    @Value("${file.import.base.directory:/data/analyzer-imports}")
    private String baseImportDir;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired(required = false)
    private BridgeRegistrationService bridgeRegistrationService;

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Override
    @SuppressWarnings("unchecked")
    public void autoCreateFromProfile(String analyzerId, Map<String, Object> configData, String analyzerName,
            String sysUserId) {
        if (analyzerId == null || configData == null) {
            return;
        }

        Analyzer analyzer;
        try {
            analyzer = analyzerService.get(analyzerId);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "autoCreateFromProfile",
                    "Invalid analyzer ID: " + analyzerId);
            return;
        }

        if (analyzer == null) {
            LogEvent.logError(this.getClass().getSimpleName(), "autoCreateFromProfile",
                    "Analyzer not found: " + analyzerId);
            return;
        }

        // Extract file format from configDefaults or protocol
        String fileFormat = "CSV";
        Object configDefaults = configData.get("configDefaults");
        if (configDefaults instanceof Map) {
            Object fmt = ((Map<String, Object>) configDefaults).get("fileFormat");
            if (fmt instanceof String) {
                fileFormat = ((String) fmt).trim().toUpperCase();
            }
        }
        Object protocol = configData.get("protocol");
        if (protocol instanceof Map) {
            Object fmt = ((Map<String, Object>) protocol).get("format");
            if (fmt instanceof String && fileFormat.equals("CSV")) {
                fileFormat = ((String) fmt).trim().toUpperCase();
            }
        }

        // Extract hasHeader from configDefaults
        boolean hasHeader = true;
        if (configDefaults instanceof Map) {
            Object hh = ((Map<String, Object>) configDefaults).get("hasHeader");
            if (hh instanceof Boolean) {
                hasHeader = (Boolean) hh;
            }
        }

        // Extract skipRows from configDefaults
        int skipRows = 0;
        if (configDefaults instanceof Map) {
            Object sr = ((Map<String, Object>) configDefaults).get("skipRows");
            if (sr instanceof Number) {
                skipRows = ((Number) sr).intValue();
            }
        }

        // Extract column mappings
        Map<String, String> columnMappings = new HashMap<>();
        Object colMapping = configData.get("column_mapping");
        if (colMapping instanceof Map) {
            ((Map<?, ?>) colMapping).forEach((k, v) -> {
                if (k instanceof String && v instanceof String) {
                    columnMappings.put((String) k, (String) v);
                }
            });
        }

        // Derive file pattern from supported_extensions or fileFormat
        String filePattern = deriveFilePattern(configData, fileFormat);

        // Validate glob syntax
        try {
            java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + filePattern);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid file pattern '" + filePattern + "' for analyzer " + analyzerId + ": " + e.getMessage());
        }

        // Extract delimiter from configDefaults
        String delimiter = fileFormat.equals("TSV") ? "\t" : ",";
        if (configDefaults instanceof Map) {
            Object delim = ((Map<String, Object>) configDefaults).get("delimiter");
            if (delim instanceof String && !((String) delim).isEmpty()) {
                delimiter = (String) delim;
            }
        }

        // Build default import directory path. Archive / error directories are
        // no longer used — the bridge is strictly read-only with respect to
        // watched directories and tracks all processing state in its local
        // FileStateStore (openelis-analyzer-bridge PR #34, plan Phase 1).
        String safeName = analyzerName != null ? analyzerName.replaceAll("[^a-zA-Z0-9_-]", "-").toLowerCase()
                : "analyzer-" + analyzerId;
        String importDir = baseImportDir + "/" + safeName + "/incoming";

        // Write all FILE config to the Analyzer entity (single source of truth)
        analyzer.setImportDirectory(importDir);
        analyzer.setFilePattern(filePattern);
        analyzer.setColumnMappings(columnMappings);
        analyzer.setFileFormat(fileFormat);
        analyzer.setDelimiter(delimiter);
        analyzer.setHasHeader(hasHeader);
        analyzer.setSkipRows(skipRows);
        analyzer.setSysUserId(sysUserId);
        analyzerService.update(analyzer);

        // Register with bridge
        if (bridgeRegistrationService != null) {
            List<String> testMappings = analyzerTestMappingService.getAllForAnalyzer(analyzer.getId()).stream()
                    .map(AnalyzerTestMapping::getAnalyzerTestName).distinct().collect(Collectors.toList());
            bridgeRegistrationService.registerFile(analyzer.getId(), analyzer.getName(), importDir, filePattern,
                    columnMappings, fileFormat, delimiter, skipRows, testMappings);
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "autoCreateFromProfile",
                "Auto-created FILE config for analyzer " + analyzerId + " (format=" + fileFormat + ", delimiter="
                        + delimiter + ", skipRows=" + skipRows + ", pattern=" + filePattern + ", importDir=" + importDir
                        + ")");
    }

    @SuppressWarnings("unchecked")
    private String deriveFilePattern(Map<String, Object> configData, String fileFormat) {
        Object extensions = configData.get("supported_extensions");
        if (extensions instanceof List && !((List<?>) extensions).isEmpty()) {
            List<String> exts = (List<String>) extensions;
            if (exts.size() == 1) {
                return "*" + exts.get(0);
            }
            return "*{" + String.join(",", exts) + "}";
        }
        switch (fileFormat) {
        case "EXCEL":
            return "*.xls";
        case "TSV":
            return "*.tsv";
        default:
            return "*.csv";
        }
    }
}
