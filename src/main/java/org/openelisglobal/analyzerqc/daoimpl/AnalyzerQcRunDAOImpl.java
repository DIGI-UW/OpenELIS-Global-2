package org.openelisglobal.analyzerqc.daoimpl;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzerqc.dao.AnalyzerQcRunDAO;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcRun;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate-backed DAO for AnalyzerQcRun.
 * Extends BaseDAOImpl — same pattern as AnalyzerExperimentDAOImpl.
 *
 * Note: schemaName omitted in JPQL because analyzer_qc_run table
 * has no schema prefix (matches analyzer_qc_rule pattern).
 */
@Component
@Transactional
public class AnalyzerQcRunDAOImpl
        extends BaseDAOImpl<AnalyzerQcRun, String>
        implements AnalyzerQcRunDAO {

    public AnalyzerQcRunDAOImpl() {
        super(AnalyzerQcRun.class);
    }

    @Override
    public Optional<AnalyzerQcRun> getLastPassForAnalyzer(String analyzerId) {
        try {
            List<AnalyzerQcRun> results = entityManager
                .createQuery(
                    "FROM AnalyzerQcRun r " +
                    "WHERE r.analyzer.id = :analyzerId " +
                    "  AND r.result = 'PASS' " +
                    "ORDER BY r.runDate DESC",
                    AnalyzerQcRun.class)
                .setParameter("analyzerId", analyzerId)
                .setMaxResults(1)
                .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            LogEvent.logError(getClass().getName(), "getLastPassForAnalyzer", e.getMessage());
            throw new LIMSRuntimeException("Error in AnalyzerQcRunDAO.getLastPassForAnalyzer", e);
        }
    }

    @Override
    public Optional<AnalyzerQcRun> getLastRunForAnalyzer(String analyzerId) {
        try {
            List<AnalyzerQcRun> results = entityManager
                .createQuery(
                    "FROM AnalyzerQcRun r " +
                    "WHERE r.analyzer.id = :analyzerId " +
                    "ORDER BY r.runDate DESC",
                    AnalyzerQcRun.class)
                .setParameter("analyzerId", analyzerId)
                .setMaxResults(1)
                .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            LogEvent.logError(getClass().getName(), "getLastRunForAnalyzer", e.getMessage());
            throw new LIMSRuntimeException("Error in AnalyzerQcRunDAO.getLastRunForAnalyzer", e);
        }
    }

    @Override
    public List<AnalyzerQcRun> getAllRunsForAnalyzer(String analyzerId) {
        try {
            return entityManager
                .createQuery(
                    "FROM AnalyzerQcRun r " +
                    "WHERE r.analyzer.id = :analyzerId " +
                    "ORDER BY r.runDate DESC",
                    AnalyzerQcRun.class)
                .setParameter("analyzerId", analyzerId)
                .getResultList();
        } catch (Exception e) {
            LogEvent.logError(getClass().getName(), "getAllRunsForAnalyzer", e.getMessage());
            throw new LIMSRuntimeException("Error in AnalyzerQcRunDAO.getAllRunsForAnalyzer", e);
        }
    }
}
