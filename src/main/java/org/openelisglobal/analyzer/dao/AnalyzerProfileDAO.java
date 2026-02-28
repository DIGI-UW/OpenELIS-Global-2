package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerProfileDAO extends BaseDAO<AnalyzerProfile, String> {

    List<AnalyzerProfile> findBySource(String source);

    List<AnalyzerProfile> findByMetaId(String profileMetaId);

    AnalyzerProfile findLatestByMetaId(String profileMetaId);

    AnalyzerProfile findByMetaIdAndVersion(String profileMetaId, String profileMetaVersion);

    boolean existsByMetaIdAndVersion(String profileMetaId, String profileMetaVersion);

    void clearLatestForMetaId(String profileMetaId);
}
