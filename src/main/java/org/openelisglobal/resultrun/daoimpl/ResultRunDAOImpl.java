package org.openelisglobal.resultrun.daoimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultrun.dao.ResultRunDAO;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunSource;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultRunDAOImpl extends BaseDAOImpl<ResultRun, Long> implements ResultRunDAO {

    private static final List<ResultRunState> ACTIVE = List.of(ResultRunState.OPEN, ResultRunState.HELD);

    public ResultRunDAOImpl() {
        super(ResultRun.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRun> findAnalyzerRun(String connectionId, String messageId) {
        if (connectionId == null || messageId == null) {
            return Optional.empty();
        }
        try {
            Query<ResultRun> query = session().createQuery(
                    "FROM ResultRun r WHERE r.source = :source"
                            + " AND r.sourceConnectionId = :connectionId AND r.sourceMessageId = :messageId",
                    ResultRun.class);
            query.setParameter("source", ResultRunSource.ANALYZER);
            query.setParameter("connectionId", connectionId);
            query.setParameter("messageId", messageId);
            query.setMaxResults(1);
            return query.uniqueResultOptional();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the run of an analyzer message", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object[]> getActiveBatchRunsOfAnalyses(Collection<String> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        return session()
                .createNativeQuery("SELECT CAST(item.analysis_id AS VARCHAR), run.id, CAST(a.test_id AS VARCHAR)"
                        + " FROM clinlims.batch_workplan_item item"
                        + " JOIN clinlims.result_run run ON run.batch_workplan_id = item.batch_workplan_id"
                        + " JOIN clinlims.analysis a ON a.id = item.analysis_id"
                        + " WHERE CAST(item.analysis_id AS VARCHAR) IN (:analysisIds) AND run.state IN ('OPEN', 'HELD')")
                .setParameterList("analysisIds", analysisIds).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRun> findBatchRun(Long batchWorkplanId) {
        if (batchWorkplanId == null) {
            return Optional.empty();
        }
        try {
            Query<ResultRun> query = session().createQuery("FROM ResultRun r WHERE r.batchWorkplanId = :batch",
                    ResultRun.class);
            query.setParameter("batch", batchWorkplanId);
            query.setMaxResults(1);
            return query.uniqueResultOptional();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the run of a workplan batch", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<String> getBatchAnalysisIds(Long runId) {
        if (runId == null) {
            return List.of();
        }
        try {
            return ((List<Object>) session()
                    .createNativeQuery("SELECT CAST(item.analysis_id AS VARCHAR) FROM clinlims.batch_workplan_item item"
                            + " JOIN clinlims.result_run run ON run.batch_workplan_id = item.batch_workplan_id"
                            + " WHERE run.id = :run ORDER BY item.sort_order, item.analysis_id")
                    .setParameter("run", runId).getResultList()).stream().map(String::valueOf).toList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting the analyses of a workplan run", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countBatchAnalysesAwaiting(Long runId, String cancelledStatusId) {
        try {
            Object count = session().createNativeQuery("SELECT COUNT(*) FROM clinlims.batch_workplan_item item"
                    + " JOIN clinlims.result_run run ON run.batch_workplan_id = item.batch_workplan_id"
                    + " JOIN clinlims.analysis a ON a.id = item.analysis_id WHERE run.id = :run"
                    + " AND CAST(a.status_id AS VARCHAR) <> :cancelled AND NOT EXISTS (SELECT 1 FROM clinlims.result r"
                    + " WHERE r.analysis_id = a.id AND COALESCE(r.value, '') <> '')").setParameter("run", runId)
                    .setParameter("cancelled", String.valueOf(cancelledStatusId)).getSingleResult();
            return ((Number) count).longValue();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting the analyses a workplan run waits for", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<String> getBatchTestIds(Long runId) {
        if (runId == null) {
            return List.of();
        }
        try {
            return ((List<Object>) session()
                    .createNativeQuery(
                            "SELECT DISTINCT CAST(a.test_id AS VARCHAR)" + " FROM clinlims.batch_workplan_item item"
                                    + " JOIN clinlims.result_run run ON run.batch_workplan_id = item.batch_workplan_id"
                                    + " JOIN clinlims.analysis a ON a.id = item.analysis_id WHERE run.id = :run")
                    .setParameter("run", runId).getResultList()).stream().map(String::valueOf).toList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting the tests of a workplan run", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultRun> getActiveRuns() {
        try {
            Query<ResultRun> query = session().createQuery(
                    "FROM ResultRun r WHERE r.state IN (:states) ORDER BY r.openedAt DESC, r.id DESC", ResultRun.class);
            query.setParameterList("states", ACTIVE);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting active runs", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResults> getStagedRows(Collection<Long> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return List.of();
        }
        try {
            Query<AnalyzerResults> query = session().createQuery(
                    "FROM AnalyzerResults ar WHERE ar.runId IN (:runIds) ORDER BY ar.runId, ar.id",
                    AnalyzerResults.class);
            query.setParameterList("runIds", runIds);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting the staged rows of runs", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countStagedRows(Long runId) {
        try {
            Query<Long> query = session()
                    .createQuery("SELECT COUNT(ar) FROM AnalyzerResults ar WHERE ar.runId = :runId", Long.class);
            query.setParameter("runId", runId);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting the staged rows of a run", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResults> getWaitingRowsForAccessions(Collection<String> accessionNumbers) {
        if (accessionNumbers == null || accessionNumbers.isEmpty()) {
            return List.of();
        }
        try {
            Query<AnalyzerResults> query = session().createQuery(
                    "SELECT ar FROM AnalyzerResults ar, ResultRun r"
                            + " WHERE r.id = ar.runId AND r.state IN (:states) AND ar.isControl = false"
                            + " AND ar.testId IS NOT NULL AND ar.accessionNumber IN (:accessions) ORDER BY ar.id",
                    AnalyzerResults.class);
            query.setParameterList("states", ACTIVE);
            query.setParameterList("accessions", accessionNumbers);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding analyzer values waiting for lab numbers", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getRunsOfAnalyses(Collection<String> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        try {
            Query<Object[]> query = session()
                    .createQuery("SELECT DISTINCT r.analysis.id, r.runId, r.analysis.test.id FROM Result r"
                            + " WHERE r.runId IS NOT NULL AND r.analysis.id IN (:analysisIds)", Object[].class);
            query.setParameterList("analysisIds", analysisIds);
            List<Object[]> runs = new ArrayList<>(query.list());
            runs.addAll(getActiveBatchRunsOfAnalyses(analysisIds));
            return runs;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the runs of analyses", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTestIdsOfResults(Long runId) {
        try {
            return session().createQuery("SELECT DISTINCT r.analysis.test.id FROM Result r WHERE r.runId = :runId",
                    String.class).setParameter("runId", runId).list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the tests of a run's results", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAnalysisIdsOfRunTest(Long runId, String testId) {
        try {
            java.util.Set<String> ids = new java.util.LinkedHashSet<>(session()
                    .createQuery("SELECT DISTINCT r.analysis.id FROM Result r WHERE r.runId = :runId"
                            + " AND r.analysis.test.id = :testId", String.class)
                    .setParameter("runId", runId).setParameter("testId", testId).list());
            ids.addAll(session().createQuery(
                    "SELECT DISTINCT a.id FROM Analysis a, AnalyzerResults ar"
                            + " WHERE ar.runId = :runId AND ar.testId = :testId AND ar.isControl = false"
                            + " AND a.test.id = ar.testId AND a.sampleItem.sample.accessionNumber = ar.accessionNumber",
                    String.class).setParameter("runId", runId).setParameter("testId", testId).list());
            return List.copyOf(ids);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the analyses of a run's test", e);
        }
    }

    private Session session() {
        return entityManager.unwrap(Session.class);
    }
}
