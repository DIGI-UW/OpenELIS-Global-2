package org.openelisglobal.analyzerimport.service;

import java.util.Optional;
import org.openelisglobal.analyzerimport.dao.AnalyzerDeliveryReceiptDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerDeliveryBundleServiceImpl implements AnalyzerDeliveryBundleService {

    private final AnalyzerDeliveryReceiptDAO receipts;

    public AnalyzerDeliveryBundleServiceImpl(AnalyzerDeliveryReceiptDAO receipts) {
        this.receipts = receipts;
    }

    @Override
    public Optional<String> getBundle(String receiptId) {
        return receipts.get(receiptId).map(AnalyzerDeliveryReceipt::getBundleJson);
    }

    @Override
    public Optional<String> findReceiptId(String connectionId, String messageId) {
        return receipts.findByDelivery(connectionId, messageId).map(AnalyzerDeliveryReceipt::getId);
    }
}
