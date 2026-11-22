package org.openelisglobal.compliance.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ComplianceReportGenerationDAO;
import org.openelisglobal.compliance.valueholder.ComplianceReportGeneration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ComplianceReportGenerationDAOImpl extends BaseDAOImpl<ComplianceReportGeneration, Long>
        implements ComplianceReportGenerationDAO {

    public ComplianceReportGenerationDAOImpl() {
        super(ComplianceReportGeneration.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceReportGeneration> findLatestBySampleId(Long sampleId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceReportGeneration crg WHERE crg.sample.id = :sampleId "
                    + "ORDER BY crg.generatedAt DESC";
            TypedQuery<ComplianceReportGeneration> query = entityManager.createQuery(hql,
                    ComplianceReportGeneration.class);
            query.setParameter("sampleId", String.valueOf(sampleId));
            query.setMaxResults(1);
            List<ComplianceReportGeneration> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceReportGeneration findLatestBySampleId()", e);
        }
    }

    @Override
    public ComplianceReportGeneration save(ComplianceReportGeneration entity) throws LIMSRuntimeException {
        try {
            entityManager.persist(entity);
            return entity;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceReportGeneration save()", e);
        }
    }
}
