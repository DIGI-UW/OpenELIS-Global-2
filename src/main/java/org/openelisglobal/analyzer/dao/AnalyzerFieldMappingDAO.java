package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerFieldMappingDAO extends BaseDAO<AnalyzerFieldMapping, String> {
    /**
     * Find all mappings by analyzer field ID using HQL (no relationship joins).
     * Returns mappings with ID fields only - relationships must be hydrated in
     * service layer.
     */
    List<AnalyzerFieldMapping> findByAnalyzerFieldId(String analyzerFieldId);

    /**
     * Find active mappings by analyzer ID using HQL (no relationship joins).
     * Returns mappings with ID fields only - relationships must be hydrated in
     * service layer.
     */
    List<AnalyzerFieldMapping> findActiveMappingsByAnalyzerId(String analyzerId);

    /**
     * Find all mappings for an analyzer using HQL (no relationship joins). Returns
     * mappings with ID fields only - relationships must be hydrated in service
     * layer.
     */
    List<AnalyzerFieldMapping> findByAnalyzerId(String analyzerId);
}
