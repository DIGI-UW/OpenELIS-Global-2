package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * FileImportWatchService - Polls configured import directories for new files.
 * 
 * Uses Spring @Scheduled annotation to poll directories at configured
 * intervals. Processes files matching the configured file pattern and moves
 * them to archive or error directories based on processing results.
 * 
 */
@Service
public class FileImportWatchService {

    @Autowired
    private FileImportService fileImportService;

    @Value("${file.import.poll.interval:60000}")
    private long pollIntervalMillis;

    @Value("${file.import.base.directory:/data/analyzer-imports}")
    private String baseImportDir;

    /**
     * Polls all active import directories for new files. Runs at configured
     * interval (default: 60 seconds).
     */
    @Scheduled(fixedRateString = "${file.import.poll.interval:60000}")
    public void pollImportDirectories() {
        try {
            List<FileImportConfiguration> activeConfigs = fileImportService.getAllActive();
            if (activeConfigs.isEmpty()) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "pollImportDirectories",
                        "No active file import configurations found");
                return;
            }

            for (FileImportConfiguration config : activeConfigs) {
                try {
                    scanDirectory(config);
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "pollImportDirectories",
                            "Error scanning directory for analyzer " + config.getAnalyzerId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "pollImportDirectories",
                    "Error polling import directories: " + e.getMessage());
        }
    }

    /**
     * Scans a single import directory for files matching the configured pattern.
     * 
     * @param config The file import configuration
     */
    private void scanDirectory(FileImportConfiguration config) {
        try {
            Path importDir = Paths.get(config.getImportDirectory());

            // Defense-in-depth: verify path is within base import directory
            try {
                Path basePath = Paths.get(baseImportDir).normalize().toAbsolutePath();
                if (!importDir.normalize().toAbsolutePath().startsWith(basePath)) {
                    LogEvent.logError(this.getClass().getSimpleName(), "scanDirectory",
                            "Import directory outside allowed base: " + config.getImportDirectory());
                    return;
                }
            } catch (InvalidPathException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "scanDirectory",
                        "Invalid import directory path: " + config.getImportDirectory());
                return;
            }

            if (!Files.exists(importDir) || !Files.isDirectory(importDir)) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "scanDirectory",
                        "Import directory does not exist or is not a directory: " + config.getImportDirectory());
                return;
            }

            // Convert file pattern (e.g., "*.csv") to regex pattern
            String filePattern = config.getFilePattern() != null ? config.getFilePattern() : "*.csv";
            Pattern pattern = convertGlobToRegex(filePattern);

            // Scan directory for matching files
            try (var fileStream = Files.list(importDir)) {
                fileStream.filter(Files::isRegularFile).filter(path -> {
                    String fileName = path.getFileName().toString();
                    return pattern.matcher(fileName).matches() && matchesFileFormat(fileName, config.getFileFormat());
                }).forEach(filePath -> {
                    try {
                        processFile(filePath, config);
                    } catch (Exception e) {
                        LogEvent.logError(this.getClass().getSimpleName(), "scanDirectory",
                                "Error processing file " + filePath + ": " + e.getMessage());
                        // Move to error directory on exception
                        fileImportService.moveToErrorDirectory(filePath, config,
                                "Exception during processing: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "scanDirectory",
                    "Error scanning directory " + config.getImportDirectory() + ": " + e.getMessage());
        }
    }

    /**
     * Processes a single file using FileAnalyzerReader.
     * 
     * @param filePath The path to the file to process
     * @param config   The file import configuration
     */
    private void processFile(Path filePath, FileImportConfiguration config) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "processFile",
                "Processing file: " + filePath + " for analyzer: " + config.getAnalyzerId());

        try {
            String systemUserId = config.getSysUserId() != null ? config.getSysUserId() : "1";
            boolean processSuccess = fileImportService.processFile(filePath, config, systemUserId);
            if (!processSuccess) {
                fileImportService.moveToErrorDirectory(filePath, config, "File processing failed");
                return;
            }

            boolean archiveSuccess = fileImportService.archiveFile(filePath, config);
            if (!archiveSuccess) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processFile",
                        "File processed successfully but archiving failed: " + filePath);
                return;
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "processFile",
                    "Successfully processed and archived file: " + filePath);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                    "Unexpected error processing file " + filePath + ": " + e.getMessage());
            fileImportService.moveToErrorDirectory(filePath, config, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Converts a glob pattern (e.g., "*.csv") to a regex pattern.
     * 
     * @param globPattern The glob pattern (e.g., "*.csv", "results_*.txt")
     * @return A regex Pattern that matches the glob pattern
     */
    private Pattern convertGlobToRegex(String globPattern) {
        // Escape special regex characters, then convert glob wildcards
        String regex = globPattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return Pattern.compile(regex);
    }

    private boolean matchesFileFormat(String fileName, String fileFormat) {
        String normalizedFormat = fileFormat == null ? "CSV" : fileFormat.trim().toUpperCase(Locale.ROOT);
        String normalizedName = fileName.toLowerCase(Locale.ROOT);

        switch (normalizedFormat) {
        case "CSV":
            return normalizedName.endsWith(".csv");
        case "TSV":
            return normalizedName.endsWith(".tsv") || normalizedName.endsWith(".txt");
        case "EXCEL":
            return normalizedName.endsWith(".xls") || normalizedName.endsWith(".xlsx");
        default:
            return true;
        }
    }
}
