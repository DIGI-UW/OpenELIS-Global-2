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

        // Default import directory. Archive / error directories are no longer
        // used — the bridge is read-only w.r.t. watched directories and tracks
        // processing state in its local FileStateStore (bridge PR #34).
        String safeName = analyzerName != null ? analyzerName.replaceAll("[^a-zA-Z0-9_-]", "-").toLowerCase()
                : "analyzer-" + analyzerId;
        String defaultImportDir = baseImportDir + "/" + safeName + "/incoming";

        // The values computed above are PROFILE DEFAULTS. The form/user may have
        // already set any of these FILE fields on the analyzer at create time —
        // those win; the profile only fills what the form left unset. This honors
        // the create-path contract ("auto-fill any file import fields not already
        // set by the form"). Without it, Add silently discarded a user-entered
        // import directory (and any other customized FILE field) and reverted to
        // the profile default. (Edit already worked — it never calls this method.)
        String importDir = preferSet(analyzer.getImportDirectory(), defaultImportDir);
        // The bridge watches directories inside its OWN container, and resolves a
        // file to an analyzer by directory — so an import dir that is not mounted
        // into the bridge (e.g. a free-form host path like /home/ubuntu/flatfiles)
        // silently resolves to an empty dir, and a directory shared by several
        // analyzers collides in the bridge registry. OpenELIS can't see the
        // bridge's mounts, so we honor the operator's value (per the preserve-
        // user-import-dir contract) but WARN when it falls outside the configured
        // base, so a likely-unwatchable path is visible in the log rather than a
        // silent no-import later. Prefer the per-analyzer default below.
        if (importDir != null && !isUnder(importDir, baseImportDir)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "autoCreateFromProfile",
                    "Import directory '" + importDir + "' is outside the bridge-visible base '" + baseImportDir
                            + "'. If it is not mounted into the analyzer bridge (or is shared by multiple"
                            + " analyzers), files will not import. Prefer a per-analyzer subdir under the base"
                            + " (e.g. '" + defaultImportDir + "'); point file.import.base.directory at a"
                            + " host-writable, bridge-mounted path (e.g. /data/analyzer-drops) for operator drops.");
        }
        filePattern = preferSet(analyzer.getFilePattern(), filePattern);
        fileFormat = preferSet(analyzer.getFileFormat(), fileFormat);
        delimiter = preferSet(analyzer.getDelimiter(), delimiter);
        // `columnMappings` is captured by the parse lambda above, so it must stay
        // effectively final — pick the effective map into a fresh local instead.
        Map<String, String> effectiveColumnMappings = (analyzer.getColumnMappings() != null
                && !analyzer.getColumnMappings().isEmpty()) ? analyzer.getColumnMappings() : columnMappings;
        if (analyzer.getHasHeader() != null) {
            hasHeader = analyzer.getHasHeader();
        }
        if (analyzer.getSkipRows() != null) {
            skipRows = analyzer.getSkipRows();
        }

        // Persist the effective FILE config (form value where provided, else profile
        // default).
        analyzer.setImportDirectory(importDir);
        analyzer.setFilePattern(filePattern);
        analyzer.setColumnMappings(effectiveColumnMappings);
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
                    effectiveColumnMappings, fileFormat, delimiter, skipRows, testMappings);
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "autoCreateFromProfile",
                "Auto-created FILE config for analyzer " + analyzerId + " (format=" + fileFormat + ", delimiter="
                        + delimiter + ", skipRows=" + skipRows + ", pattern=" + filePattern + ", importDir=" + importDir
                        + ")");
    }

    /** The form/user value when set (non-blank), else the profile default. */
    private static String preferSet(String formValue, String profileDefault) {
        return (formValue != null && !formValue.isBlank()) ? formValue : profileDefault;
    }

    /** True if {@code path} is the base or a descendant of it (normalized). */
    private static boolean isUnder(String path, String base) {
        if (path == null || base == null || base.isBlank()) {
            return false;
        }
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(path).normalize();
            java.nio.file.Path b = java.nio.file.Paths.get(base).normalize();
            return p.startsWith(b);
        } catch (RuntimeException e) {
            return false;
        }
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
