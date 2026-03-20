package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qc.valueholder.QCSignature;

public interface QCSignatureDAO extends BaseDAO<QCSignature, Long> {

    List<QCSignature> findByReportId(Long reportId);
}
