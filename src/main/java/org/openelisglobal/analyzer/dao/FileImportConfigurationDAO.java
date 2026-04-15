package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for FileImportConfiguration
 */
public interface FileImportConfigurationDAO extends BaseDAO<FileImportConfiguration, String> {

    /**
     * Find FileImportConfiguration by analyzer ID
     * 
     * @param analyzerId The analyzer ID
     * @return Optional FileImportConfiguration
     */
    Optional<FileImportConfiguration> findByAnalyzerId(Integer analyzerId);

    /**
     * Find all active FileImportConfiguration entries
     *
     * @return List of active configurations
     */
    java.util.List<FileImportConfiguration> findAllActive();

    /**
     * Find active configurations that overlap with a given directory and file
     * format, excluding a specific analyzer ID (for update validation).
     *
     * @param importDirectory   the import directory path to check
     * @param fileFormat        the file format (CSV, EXCEL, etc.)
     * @param excludeAnalyzerId analyzer ID to exclude (null for create validation)
     * @return list of overlapping active configurations
     */
    java.util.List<FileImportConfiguration> findActiveByImportDirectoryAndFileFormat(String importDirectory,
            String fileFormat, Integer excludeAnalyzerId);
}
