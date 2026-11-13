package org.openelisglobal.reportqueue.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reportqueue.dao.ReportVersionDAO;
import org.openelisglobal.reportqueue.valueholder.ReportVersion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReportVersionDAOImpl extends BaseDAOImpl<ReportVersion, Long> implements ReportVersionDAO {

    public ReportVersionDAOImpl() {
        super(ReportVersion.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportVersion> findLatest(Long sampleId, String reportType) {
        try {
            TypedQuery<ReportVersion> query = entityManager
                    .createQuery("FROM ReportVersion v WHERE v.sampleId = :sampleId AND v.reportType = :reportType"
                            + " ORDER BY v.version DESC", ReportVersion.class);
            query.setParameter("sampleId", sampleId);
            query.setParameter("reportType", reportType);
            query.setMaxResults(1);
            List<ReportVersion> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportVersion findLatest()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVersion> findAllForSample(Long sampleId, String reportType) {
        try {
            TypedQuery<ReportVersion> query = entityManager
                    .createQuery("FROM ReportVersion v WHERE v.sampleId = :sampleId AND v.reportType = :reportType"
                            + " ORDER BY v.version ASC", ReportVersion.class);
            query.setParameter("sampleId", sampleId);
            query.setParameter("reportType", reportType);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportVersion findAllForSample()", e);
        }
    }

    @Override
    public ReportVersion save(ReportVersion version) {
        try {
            if (version.getId() == null) {
                entityManager.persist(version);
                return version;
            }
            return entityManager.merge(version);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportVersion save()", e);
        }
    }
}
