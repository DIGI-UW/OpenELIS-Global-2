package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerFieldMappingDAO extends BaseDAO<AnalyzerFieldMapping, String> {
    List<AnalyzerFieldMapping> findByAnalyzerFieldId(String analyzerFieldId);

    List<AnalyzerFieldMapping> findActiveMappingsByAnalyzerId(String analyzerId);

    /**
     * Find all mappings for an analyzer with analyzer field eagerly fetched Uses
     * JOIN FETCH to load relationships within transaction
     */
    List<AnalyzerFieldMapping> findByAnalyzerIdWithFields(String analyzerId);

    /**
     * Find mapping by ID with analyzer field eagerly fetched Uses JOIN FETCH to
     * load relationships within transaction
     */
    AnalyzerFieldMapping findByIdWithField(String mappingId);
}
