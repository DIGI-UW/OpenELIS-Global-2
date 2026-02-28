package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface AstmFlagMappingDAO extends BaseDAO<AstmFlagMapping, String> {
    List<AstmFlagMapping> findByAnalyzerId(String analyzerId);
}
