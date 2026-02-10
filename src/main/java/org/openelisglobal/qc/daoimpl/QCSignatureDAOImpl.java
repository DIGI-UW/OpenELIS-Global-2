package org.openelisglobal.qc.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.dao.QCSignatureDAO;
import org.openelisglobal.qc.valueholder.QCSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QCSignatureDAOImpl extends BaseDAOImpl<QCSignature, Long> implements QCSignatureDAO {

    public QCSignatureDAOImpl() {
        super(QCSignature.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCSignature> findByReportId(Long reportId) {
        try {
            String hql = "FROM QCSignature s WHERE s.reportId = :reportId ORDER BY s.signedAt DESC";
            Query<QCSignature> query = entityManager.unwrap(Session.class).createQuery(hql, QCSignature.class);
            query.setParameter("reportId", reportId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QC signatures by report", e);
        }
    }
}
