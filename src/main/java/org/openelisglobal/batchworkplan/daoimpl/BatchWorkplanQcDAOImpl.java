package org.openelisglobal.batchworkplan.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class BatchWorkplanQcDAOImpl implements BatchWorkplanQcDAO {

    private static final String SEVERITY_REJECTION = "REJECTION";
    private static final String RESOLUTION_UNRESOLVED = "UNRESOLVED";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<QCControlLot> getControlLots(String testId, String instrumentId) {
        try {
            String hql = "FROM QCControlLot l WHERE l.testId = :testId AND "
                    + (instrumentId == null ? "l.instrumentId IS NULL" : "l.instrumentId = :instrumentId")
                    + " ORDER BY l.activationDate DESC";
            var query = entityManager.createQuery(hql, QCControlLot.class).setParameter("testId", testId);
            if (instrumentId != null) {
                query.setParameter("instrumentId", instrumentId);
            }
            return query.getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading control lots for a batch workplan", e);
        }
    }

    @Override
    public List<QCResult> getRecentResults(String controlLotId, int limit) {
        try {
            return entityManager
                    .createQuery("FROM QCResult r WHERE r.controlLotId = :lotId ORDER BY r.runDateTime DESC",
                            QCResult.class)
                    .setParameter("lotId", controlLotId).setMaxResults(limit).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading QC results for a batch workplan", e);
        }
    }

    @Override
    public QCResult getLatestInStatuses(String controlLotId, List<String> resultStatuses) {
        try {
            List<QCResult> results = entityManager
                    .createQuery("FROM QCResult r WHERE r.controlLotId = :lotId AND r.resultStatus IN (:statuses)"
                            + " ORDER BY r.runDateTime DESC", QCResult.class)
                    .setParameter("lotId", controlLotId).setParameter("statuses", resultStatuses).setMaxResults(1)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading the latest QC result for a batch workplan", e);
        }
    }

    @Override
    public List<QCRuleViolation> getOpenRejections(String controlLotId) {
        try {
            return entityManager
                    .createQuery("SELECT v FROM QCRuleViolation v, QCResult r"
                            + " WHERE r.id = v.triggeringResultId AND r.controlLotId = :lotId"
                            + " AND v.severity = :severity AND v.resolutionStatus = :unresolved"
                            + " ORDER BY v.violationDateTime DESC", QCRuleViolation.class)
                    .setParameter("lotId", controlLotId).setParameter("severity", SEVERITY_REJECTION)
                    .setParameter("unresolved", RESOLUTION_UNRESOLVED).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading open QC violations for a batch workplan", e);
        }
    }

    @Override
    public Map<String, List<QCRuleViolation>> getViolationsForResults(List<String> resultIds) {
        Map<String, List<QCRuleViolation>> byResult = new HashMap<>();
        if (resultIds == null || resultIds.isEmpty()) {
            return byResult;
        }
        try {
            List<QCRuleViolation> violations = entityManager
                    .createQuery("FROM QCRuleViolation v WHERE v.triggeringResultId IN (:ids)", QCRuleViolation.class)
                    .setParameter("ids", resultIds).getResultList();
            for (QCRuleViolation violation : violations) {
                byResult.computeIfAbsent(violation.getTriggeringResultId(), k -> new ArrayList<>()).add(violation);
            }
            return byResult;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading QC violations for a batch workplan", e);
        }
    }

    /**
     * A projection, not an entity load: Analyzer.testUnitIds is a converted
     * collection Hibernate flags dirty on load, which bumps the analyzer's version
     * on flush (see QcViolationNceServiceImpl).
     */
    @Override
    public Map<String, String> getAnalyzerNames(List<String> analyzerIds) {
        Map<String, String> names = new LinkedHashMap<>();
        if (analyzerIds == null || analyzerIds.isEmpty()) {
            return names;
        }
        try {
            List<Object[]> rows = entityManager
                    .createQuery("SELECT a.id, a.name FROM Analyzer a WHERE a.id IN (:ids)", Object[].class)
                    .setParameter("ids", analyzerIds).getResultList();
            for (Object[] row : rows) {
                names.put(String.valueOf(row[0]), (String) row[1]);
            }
            return names;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading analyzer names for a batch workplan", e);
        }
    }
}
