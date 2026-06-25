package org.openelisglobal.reports.vectorsurveillance.manualentry.daoimpl;

import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.vectorsurveillance.manualentry.dao.ManualEntrySubmissionAuditDAO;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntrySubmissionAudit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ManualEntrySubmissionAuditDAOImpl extends BaseDAOImpl<ManualEntrySubmissionAudit, Integer>
        implements ManualEntrySubmissionAuditDAO {

    public ManualEntrySubmissionAuditDAOImpl() {
        super(ManualEntrySubmissionAudit.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualEntrySubmissionAudit> getByPeriod(LocalDate periodStart, LocalDate periodEnd) {
        try {
            StringBuilder hql = new StringBuilder("from ManualEntrySubmissionAudit a");
            if (periodStart != null) {
                hql.append(" where a.periodStart >= :periodStart");
            }
            if (periodEnd != null) {
                hql.append(periodStart != null ? " and" : " where").append(" a.periodEnd <= :periodEnd");
            }
            hql.append(" order by a.submittedAt desc");
            TypedQuery<ManualEntrySubmissionAudit> query = entityManager.createQuery(hql.toString(),
                    ManualEntrySubmissionAudit.class);
            if (periodStart != null) {
                query.setParameter("periodStart", periodStart);
            }
            if (periodEnd != null) {
                query.setParameter("periodEnd", periodEnd);
            }
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ManualEntrySubmissionAuditDAOImpl.getByPeriod()", e);
        }
    }
}
