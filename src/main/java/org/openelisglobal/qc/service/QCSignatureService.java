package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.qc.valueholder.QCSignature;

public interface QCSignatureService {

    QCSignature signReport(Long reportId, String userId, String ipAddress, String comment);

    List<QCSignature> getSignaturesByReportId(Long reportId);
}
