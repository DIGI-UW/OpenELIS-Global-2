package org.openelisglobal.analyzer.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for FileImportConfiguration and file import operations
 */
public interface FileImportService extends BaseObjectService<FileImportConfiguration, String> {

    /**
     * Get FileImportConfiguration by analyzer ID
     * 
     * @param analyzerId The analyzer ID
     * @return Optional FileImportConfiguration
     */
    Optional<FileImportConfiguration> getByAnalyzerId(Integer analyzerId);

    /**
     * Get all active FileImportConfiguration entries
     * 
     * @return List of active configurations
     */
    List<FileImportConfiguration> getAllActive();

    /**
     * Process a file for import based on configuration
     * 
     * @param filePath      Path to the file to process
     * @param configuration FileImportConfiguration to use
     * @param systemUserId  System user ID for audit trail
     * @return true if processing succeeded, false otherwise
     */
    boolean processFile(Path filePath, FileImportConfiguration configuration, String systemUserId);

    /**
     * Archive a successfully processed file
     * 
     * @param filePath      Path to the file to archive
     * @param configuration FileImportConfiguration with archive directory
     * @return true if archival succeeded, false otherwise
     */
    boolean archiveFile(Path filePath, FileImportConfiguration configuration);

    /**
     * Move a failed file to error directory
     * 
     * @param filePath      Path to the file that failed
     * @param configuration FileImportConfiguration with error directory
     * @param errorMessage  Error message to log
     * @return true if move succeeded, false otherwise
     */
    boolean moveToErrorDirectory(Path filePath, FileImportConfiguration configuration, String errorMessage);

    /**
     * Check for duplicate results (analyzer ID + sample ID + test + timestamp)
     * 
     * @param analyzerId Analyzer ID from configuration
     * @param sampleId   Sample ID
     * @param testCode   Test code
     * @param testDate   Test date
     * @param testTime   Test time
     * @return true if duplicate exists, false otherwise
     */
    boolean isDuplicate(Integer analyzerId, String sampleId, String testCode, String testDate, String testTime);
}
