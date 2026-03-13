package org.openelisglobal.analyzer.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.form.AnalyzerRunPreviewForm;
import org.openelisglobal.analyzer.form.SubmitRequestForm;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReader;
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
     * Resolve the line reader to use for a file format.
     *
     * @param configuration file import configuration
     * @return reader implementation for the configured format
     */
    AnalyzerReader getReaderForFormat(FileImportConfiguration configuration);

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

    /**
     * Parse uploaded file and return preview with validation (OGC-324). Creates an
     * AnalyzerFileUpload audit record and checks for duplicate file (SHA-256).
     *
     * @param analyzerId   analyzer ID (must have file import config)
     * @param fileStream   file content
     * @param filename     original filename
     * @param systemUserId user for audit
     * @return preview with record counts and rows; uploadId for submit;
     *         duplicateWarning if hash matched
     */
    AnalyzerRunPreviewForm parseAndPreview(Integer analyzerId, InputStream fileStream, String filename,
            String systemUserId);

    /**
     * Submit validated results from a preview session (OGC-324). Updates
     * AnalyzerFileUpload status to PROCESSING then COMPLETED.
     *
     * @param analyzerId   analyzer ID
     * @param request      previewSessionId (uploadId) and optional excludedRows
     * @param systemUserId user for audit
     */
    void submitResults(Integer analyzerId, SubmitRequestForm request, String systemUserId);

    /**
     * Auto-create a FileImportConfiguration from a loaded profile's config data.
     * Called during analyzer creation when the profile protocol is FILE.
     *
     * @param analyzerId   the newly created analyzer's ID (as String)
     * @param configData   the full profile JSON parsed as a Map
     * @param analyzerName the analyzer name (used for default directory paths)
     * @param sysUserId    the current user's ID (required for audit column)
     */
    void autoCreateFromProfile(String analyzerId, Map<String, Object> configData, String analyzerName,
            String sysUserId);
}
