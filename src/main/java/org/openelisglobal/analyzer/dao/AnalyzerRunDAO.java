package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerRun;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO for AnalyzerRun (one run per file upload, holds preview data for submit).
 */
public interface AnalyzerRunDAO extends BaseDAO<AnalyzerRun, Long> {

    /**
     * Find the run for a given file upload (one-to-one).
     */
    Optional<AnalyzerRun> findByAnalyzerFileUploadId(Long analyzerFileUploadId);
}
