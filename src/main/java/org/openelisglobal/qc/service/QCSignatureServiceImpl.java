package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.qc.dao.QCSignatureDAO;
import org.openelisglobal.qc.valueholder.QCSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QCSignatureServiceImpl implements QCSignatureService {

    @Autowired
    private QCSignatureDAO signatureDAO;

    @Override
    public QCSignature signReport(Long reportId, String userId, String ipAddress, String comment) {
        QCSignature signature = new QCSignature();
        signature.setReportId(reportId);
        signature.setUserId(userId);
        signature.setSignedAt(new Timestamp(System.currentTimeMillis()));
        signature.setIpAddress(ipAddress);
        signature.setComment(comment);
        signatureDAO.insert(signature);
        return signature;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCSignature> getSignaturesByReportId(Long reportId) {
        return signatureDAO.findByReportId(reportId);
    }
}
