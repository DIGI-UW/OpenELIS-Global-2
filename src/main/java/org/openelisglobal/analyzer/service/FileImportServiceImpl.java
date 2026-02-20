package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.FileImportConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.analyzerimport.analyzerreaders.FileAnalyzerReader;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FileImportServiceImpl extends BaseObjectServiceImpl<FileImportConfiguration, String>
        implements FileImportService {

    @Value("${file.import.base.directory:/data/analyzer-imports}")
    private String baseImportDir;

    @Autowired
    private FileImportConfigurationDAO fileImportConfigurationDAO;

    @Autowired
    private AnalyzerResultsDAO analyzerResultsDAO;

    public FileImportServiceImpl() {
        super(FileImportConfiguration.class);
    }

    @Override
    protected FileImportConfigurationDAO getBaseObjectDAO() {
        return fileImportConfigurationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileImportConfiguration> getByAnalyzerId(Integer analyzerId) {
        return fileImportConfigurationDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileImportConfiguration> getAllActive() {
        return fileImportConfigurationDAO.findAllActive();
    }

    @Override
    public boolean processFile(Path filePath, FileImportConfiguration configuration, String systemUserId) {
        try (InputStream fileStream = Files.newInputStream(filePath)) {
            FileAnalyzerReader reader = new FileAnalyzerReader(configuration);

            boolean readSuccess = reader.readStream(fileStream);
            if (!readSuccess) {
                String error = reader.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                        "Failed to read file " + filePath + ": " + error);
                return false;
            }

            boolean insertSuccess = reader.insertAnalyzerData(systemUserId);
            if (!insertSuccess) {
                String error = reader.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                        "Failed to insert analyzer data from file " + filePath + ": " + error);
                return false;
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "processFile",
                    "Successfully processed file: " + filePath + " for analyzer: " + configuration.getAnalyzerId());
            return true;
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                    "IO error processing file " + filePath + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                    "Unexpected error processing file " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean archiveFile(Path filePath, FileImportConfiguration configuration) {
        try {
            if (configuration.getArchiveDirectory() == null || configuration.getArchiveDirectory().isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "archiveFile",
                        "Archive directory not configured for analyzer: " + configuration.getAnalyzerId());
                return false;
            }

            Path archiveDir = Paths.get(configuration.getArchiveDirectory());

            // Defense-in-depth: verify archive path is within base import directory
            try {
                Path basePath = Paths.get(baseImportDir).normalize().toAbsolutePath();
                if (!archiveDir.normalize().toAbsolutePath().startsWith(basePath)) {
                    LogEvent.logError(this.getClass().getSimpleName(), "archiveFile",
                            "Archive directory outside allowed base: " + configuration.getArchiveDirectory());
                    return false;
                }
            } catch (InvalidPathException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "archiveFile",
                        "Invalid archive directory path: " + configuration.getArchiveDirectory());
                return false;
            }

            if (!Files.exists(archiveDir)) {
                Files.createDirectories(archiveDir);
            }

            Path targetPath = archiveDir.resolve(filePath.getFileName());
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            LogEvent.logInfo(this.getClass().getSimpleName(), "archiveFile",
                    "Archived file: " + filePath + " to: " + targetPath);
            return true;
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "archiveFile",
                    "Error archiving file: " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean moveToErrorDirectory(Path filePath, FileImportConfiguration configuration, String errorMessage) {
        try {
            if (configuration.getErrorDirectory() == null || configuration.getErrorDirectory().isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "moveToErrorDirectory",
                        "Error directory not configured for analyzer: " + configuration.getAnalyzerId());
                return false;
            }

            Path errorDir = Paths.get(configuration.getErrorDirectory());

            // Defense-in-depth: verify error path is within base import directory
            try {
                Path basePath = Paths.get(baseImportDir).normalize().toAbsolutePath();
                if (!errorDir.normalize().toAbsolutePath().startsWith(basePath)) {
                    LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                            "Error directory outside allowed base: " + configuration.getErrorDirectory());
                    return false;
                }
            } catch (InvalidPathException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                        "Invalid error directory path: " + configuration.getErrorDirectory());
                return false;
            }

            if (!Files.exists(errorDir)) {
                Files.createDirectories(errorDir);
            }

            Path targetPath = errorDir.resolve(filePath.getFileName());
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                    "Moved failed file: " + filePath + " to: " + targetPath + " - Error: " + errorMessage);
            return true;
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                    "Error moving file to error directory: " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicate(Integer analyzerId, String sampleId, String testCode, String testDate, String testTime) {
        try {
            AnalyzerResults tempResult = new AnalyzerResults();
            tempResult.setAnalyzerId(String.valueOf(analyzerId));
            tempResult.setAccessionNumber(sampleId);
            tempResult.setTestName(testCode);

            Timestamp completeDate = null;
            if (testDate != null && !testDate.isEmpty()) {
                try {
                    String dateTimeString = testDate;
                    if (testTime != null && !testTime.isEmpty()) {
                        dateTimeString += " " + testTime;
                    } else {
                        dateTimeString += " 00:00:00";
                    }
                    SimpleDateFormat[] formats = { new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm"), new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
                            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss") };
                    for (SimpleDateFormat format : formats) {
                        try {
                            completeDate = new Timestamp(format.parse(dateTimeString).getTime());
                            break;
                        } catch (ParseException e) {
                            // Try next format
                        }
                    }
                    if (completeDate == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "isDuplicate",
                                "Could not parse date/time: " + dateTimeString);
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "isDuplicate",
                            "Error parsing date/time: " + e.getMessage());
                }
            }
            tempResult.setCompleteDate(completeDate);

            List<AnalyzerResults> duplicates = analyzerResultsDAO.getDuplicateResultByAccessionAndTest(tempResult);

            if (duplicates != null && !duplicates.isEmpty()) {
                if (completeDate != null) {
                    for (AnalyzerResults duplicate : duplicates) {
                        if (duplicate.getCompleteDate() != null && duplicate.getCompleteDate().equals(completeDate)) {
                            LogEvent.logDebug(this.getClass().getSimpleName(), "isDuplicate",
                                    "Found exact duplicate: analyzer=" + analyzerId + ", sample=" + sampleId + ", test="
                                            + testCode + ", date=" + completeDate);
                            return true;
                        }
                    }
                    // If we have a date but no exact match, it's not a duplicate
                    return false;
                } else {
                    // No date provided, consider it a duplicate if analyzerId, accessionNumber and
                    // testName match
                    LogEvent.logDebug(this.getClass().getSimpleName(), "isDuplicate",
                            "Found duplicate (no date check): analyzer=" + analyzerId + ", sample=" + sampleId
                                    + ", test=" + testCode);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "isDuplicate", "Error checking duplicate for analyzer: "
                    + analyzerId + ", sample: " + sampleId + ", test: " + testCode + ": " + e.getMessage());
            return false; // On error, don't block processing
        }
    }
}
