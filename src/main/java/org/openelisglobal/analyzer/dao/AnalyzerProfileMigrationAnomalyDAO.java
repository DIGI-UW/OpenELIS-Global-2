package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerProfileMigrationAnomalyDAO extends BaseDAO<AnalyzerProfileMigrationAnomaly, String> {

    List<AnalyzerProfileMigrationAnomaly> findOpenByAnalyzerId(String analyzerId);

    List<AnalyzerProfileMigrationAnomaly> findByAnalyzerIdForUpdate(String analyzerId);
}
