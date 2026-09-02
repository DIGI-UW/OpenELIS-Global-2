package org.openelisglobal.compliance.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ComplianceReportArchiveDAO;
import org.openelisglobal.compliance.valueholder.ComplianceReportArchive;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ComplianceReportArchiveDAOImpl extends BaseDAOImpl<ComplianceReportArchive, Long>
        implements ComplianceReportArchiveDAO {

    public ComplianceReportArchiveDAOImpl() {
        super(ComplianceReportArchive.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceReportArchive> findBySampleIdAndAmendmentNumber(Long sampleId, Integer amendmentNumber)
            throws LIMSRuntimeException {
        try {
            TypedQuery<ComplianceReportArchive> query = entityManager.createQuery(
                    "FROM ComplianceReportArchive a WHERE a.sample.id = :sampleId AND a.amendmentNumber = :amendmentNumber",
                    ComplianceReportArchive.class);
            query.setParameter("sampleId", String.valueOf(sampleId));
            query.setParameter("amendmentNumber", amendmentNumber);
            query.setMaxResults(1);
            List<ComplianceReportArchive> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceReportArchive findBySampleIdAndAmendmentNumber()", e);
        }
    }

    @Override
    public ComplianceReportArchive save(ComplianceReportArchive entity) throws LIMSRuntimeException {
        try {
            entityManager.persist(entity);
            return entity;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceReportArchive save()", e);
        }
    }
}
