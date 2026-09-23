package org.openelisglobal.analyzerimport.dao;

import java.util.Optional;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerDeliveryReceiptDAO extends BaseDAO<AnalyzerDeliveryReceipt, String> {
    Optional<AnalyzerDeliveryReceipt> findByDelivery(String connectionId, String messageId);
}
