package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerMappingDAO extends BaseDAO<AnalyzerMapping, String> {

    /**
     * The analyzer's newest revision: its working draft, or the one in force when
     * nothing newer exists.
     */
    Optional<AnalyzerMapping> findLatestByAnalyzerId(String analyzerId);

    /**
     * The analyzers whose mapping in force is pinned to any revision of the
     * profile.
     */
    List<Analyzer> findAnalyzersInForceOnProfile(String profileId);
}
