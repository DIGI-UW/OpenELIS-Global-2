package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceSpecimenDAO;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceSpecimenDAOImpl extends BaseDAOImpl<NceSpecimen, Integer> implements NceSpecimenDAO {

    /** NCE trigger sources that represent a QC failure holding patient results. */
    private static final List<String> QC_TRIGGER_SOURCES = List.of("QC_VIOLATION", "QC_BENCH_CONTROL");

    /** Statuses that end a hold. Anything else still counts as open. */
    private static final List<String> CLOSED_STATUSES = List.of("Closed", "Completed");

    public NceSpecimenDAOImpl() {
        super(NceSpecimen.class);
    }

    @Override
    public List<NceSpecimen> getSpecimenByNceId(Integer nceId) throws LIMSRuntimeException {
        try {
            String sql = "from NceSpecimen ns where ns.nceId = :nceId";
            TypedQuery<NceSpecimen> query = entityManager.createQuery(sql, NceSpecimen.class);
            query.setParameter("nceId", nceId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimen getSpecimenByNceId(Integer nceId)", e);
        }
    }

    /**
     * Of the given analyses, those linked to a still-open QC-failure NCE — the
     * Validation QC-fail signal (OGC-1147 FR-C1/C3). Batched deliberately: a
     * validation list is dozens of rows and this must not become a per-row query.
     *
     * <p>
     * "Open" is everything except Closed and Completed. Erring toward keeping a
     * result flagged is the safe direction: a stale hold is an inconvenience a
     * supervisor can clear, an early release is a patient-safety event.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Integer> findAnalysisIdsWithOpenQcHold(Collection<Integer> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT DISTINCT ns.analysisId FROM NceSpecimen ns, NcEvent e"
                + " WHERE ns.nceId = e.id AND ns.analysisId IN :analysisIds"
                + " AND e.triggerSourceType IN :triggerSources"
                + " AND (e.status IS NULL OR e.status NOT IN :closedStatuses)";
        try {
            TypedQuery<Integer> query = entityManager.createQuery(sql, Integer.class);
            query.setParameter("analysisIds", analysisIds);
            query.setParameter("triggerSources", QC_TRIGGER_SOURCES);
            query.setParameter("closedStatuses", CLOSED_STATUSES);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimen findAnalysisIdsWithOpenQcHold", e);
        }
    }

    @Override
    public List<NceSpecimen> getSpecimenBySampleId(Integer sampleId) {
        String sql = "from NceSpecimen ns where ns.sampleItemId = :sampleId";
        TypedQuery<NceSpecimen> query = entityManager.createQuery(sql, NceSpecimen.class);
        query.setParameter("sampleId", sampleId);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId) {
        try {
            String sql = "SELECT COUNT(*) FROM NceSpecimen ns WHERE ns.nceId = :nceId AND ns.sampleItemId = :sampleItemId";
            TypedQuery<Long> query = entityManager.createQuery(sql, Long.class);
            query.setParameter("nceId", nceId);
            query.setParameter("sampleItemId", sampleItemId);
            Long count = query.getSingleResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimenDAOImpl existsByNceIdAndSampleItemId", e);
        }
    }
}
