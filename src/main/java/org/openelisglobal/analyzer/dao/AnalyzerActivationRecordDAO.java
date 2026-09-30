package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerActivationRecordDAO extends BaseDAO<AnalyzerActivationRecord, String> {

    List<AnalyzerActivationRecord> findByAnalyzerId(String analyzerId);
}
