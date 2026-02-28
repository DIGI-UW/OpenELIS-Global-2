package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;
import org.openelisglobal.common.dao.BaseDAO;

public interface AstmPendingCodeDAO extends BaseDAO<AstmPendingCode, String> {
    List<AstmPendingCode> findByAnalyzerId(String analyzerId);

    List<AstmPendingCode> findPendingByAnalyzerId(String analyzerId);

    int countPendingByAnalyzerId(String analyzerId);
}
