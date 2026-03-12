package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerFileUpload;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO for AnalyzerFileUpload audit records (OGC-324).
 */
public interface AnalyzerFileUploadDAO extends BaseDAO<AnalyzerFileUpload, Long> {

    /**
     * Find an upload by analyzer ID and file SHA-256 hash (for duplicate
     * detection).
     */
    Optional<AnalyzerFileUpload> findByAnalyzerIdAndFileHash(Integer analyzerId, String fileHashSha256);

    /**
     * Find recent uploads for an analyzer (e.g. for audit display).
     */
    List<AnalyzerFileUpload> findByAnalyzerId(Integer analyzerId, int maxResults);
}
