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
}
