package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerDeliveryAction;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerDeliveryActionDAO extends BaseDAO<AnalyzerDeliveryAction, String> {

    List<AnalyzerDeliveryAction> findByOutboxEntryId(String outboxEntryId);
}
