package org.openelisglobal.analyzerimport.daoimpl;

import java.util.Optional;
import org.openelisglobal.analyzerimport.dao.AnalyzerDeliveryReceiptDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerDeliveryReceiptDAOImpl extends BaseDAOImpl<AnalyzerDeliveryReceipt, String>
        implements AnalyzerDeliveryReceiptDAO {
    public AnalyzerDeliveryReceiptDAOImpl() {
        super(AnalyzerDeliveryReceipt.class);
    }

    @Override
    public Optional<AnalyzerDeliveryReceipt> findByDelivery(String connectionId, String messageId) {
        return entityManager
                .createQuery(
                        "FROM AnalyzerDeliveryReceipt receipt "
                                + "WHERE receipt.connectionId = :connectionId AND receipt.messageId = :messageId",
                        AnalyzerDeliveryReceipt.class)
                .setParameter("connectionId", connectionId).setParameter("messageId", messageId).getResultStream()
                .findFirst();
    }
}
