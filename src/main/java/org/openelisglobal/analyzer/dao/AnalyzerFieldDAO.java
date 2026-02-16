package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerFieldDAO extends BaseDAO<AnalyzerField, String> {
    List<AnalyzerField> findByAnalyzerId(String analyzerId);

    /**
     * Find analyzer field by ID with analyzer eagerly fetched Uses JOIN FETCH to
     * load analyzer relationship within transaction
     */
    java.util.Optional<AnalyzerField> findByIdWithAnalyzer(String analyzerFieldId);

    /**
     * Find analyzer field by analyzer ID and field name
     * 
     * @param analyzerId The analyzer ID
     * @param fieldName  The field name
     * @return Optional AnalyzerField if found
     */
    java.util.Optional<AnalyzerField> findByAnalyzerIdAndFieldName(String analyzerId, String fieldName);
}
