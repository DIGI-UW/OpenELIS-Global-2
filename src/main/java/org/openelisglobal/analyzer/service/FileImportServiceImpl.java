package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.FileImportConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FileImportServiceImpl extends BaseObjectServiceImpl<FileImportConfiguration, String>
        implements FileImportService {

    @Autowired
    private FileImportConfigurationDAO fileImportConfigurationDAO;

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
        try {
            // File processing will be handled by FileAnalyzerReader
            // This method is a placeholder for future WatchService integration
            LogEvent.logInfo(this.getClass().getSimpleName(), "processFile",
                    "Processing file: " + filePath + " for analyzer: " + configuration.getAnalyzerId());
            return true;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                    "Error processing file: " + filePath + ": " + e.getMessage());
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
    public boolean isDuplicate(String sampleId, String testCode, String testDate, String testTime) {
        // TODO: Implement duplicate detection by querying AnalyzerResults table
        // This is a placeholder - actual implementation should check AnalyzerResults
        // for matching sample ID, test code, and timestamp
        LogEvent.logDebug(this.getClass().getSimpleName(), "isDuplicate",
                "Checking duplicate for sample: " + sampleId + ", test: " + testCode + ", date: " + testDate
                        + ", time: " + testTime);
        return false;
    }
}
