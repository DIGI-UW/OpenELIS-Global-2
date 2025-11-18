package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;

public interface AnalyzerFieldDAO extends BaseDAO<AnalyzerField, String> {
    List<AnalyzerField> findByAnalyzerId(String analyzerId);
    
    /**
     * Find analyzer field by ID with analyzer eagerly fetched
     * Uses JOIN FETCH to load analyzer relationship within transaction
     */
    java.util.Optional<AnalyzerField> findByIdWithAnalyzer(String analyzerFieldId);
}

