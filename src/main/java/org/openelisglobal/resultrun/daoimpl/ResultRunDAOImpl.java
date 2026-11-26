package org.openelisglobal.resultrun.daoimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
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
    /** Advisory lock namespace for an analysis's run of one (OGC-1200). */
    private static final int RUN_OF_ONE_LOCK = 1200;

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
    @SuppressWarnings("unchecked")
    public List<Object[]> getActiveRunsOfOne(Collection<String> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        return session()
                .createNativeQuery("SELECT CAST(run.analysis_id AS VARCHAR), run.id, CAST(a.test_id AS VARCHAR)"
                        + " FROM clinlims.result_run run JOIN clinlims.analysis a ON a.id = run.analysis_id"
                        + " WHERE CAST(run.analysis_id AS VARCHAR) IN (:analysisIds) AND run.state IN ('OPEN', 'HELD')")
                .setParameterList("analysisIds", analysisIds).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object[]> getReusableRunsOfOne(Collection<String> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        return session()
                .createNativeQuery("SELECT DISTINCT ON (run.analysis_id) CAST(run.analysis_id AS VARCHAR), run.id,"
                        + " CAST(a.test_id AS VARCHAR), run.state FROM clinlims.result_run run"
                        + " JOIN clinlims.analysis a ON a.id = run.analysis_id"
                        + " WHERE CAST(run.analysis_id AS VARCHAR) IN (:analysisIds) AND run.state <> 'SUPERSEDED'"
                        + " AND NOT EXISTS (SELECT 1 FROM clinlims.result_run_test_disposition d"
                        + " WHERE d.run_id = run.id AND d.action IN ('RETEST', 'REJECT'))"
                        + " ORDER BY run.analysis_id, run.id DESC")
                .setParameterList("analysisIds", analysisIds).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRun> findReusableRunOfOne(String analysisId) {
        if (StringUtils.isBlank(analysisId)) {
            return Optional.empty();
        }
        return getReusableRunsOfOne(List.of(analysisId)).stream().findFirst()
                .flatMap(row -> get(((Number) row[1]).longValue()));
    }

    @Override
    public void lockRunOfOne(String analysisId) {
        session().createNativeQuery("SELECT CAST(pg_advisory_xact_lock(:namespace, hashtext(:analysis)) AS text)")
                .setParameter("namespace", RUN_OF_ONE_LOCK).setParameter("analysis", analysisId).getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRun> findOpenRunOfOne(String analysisId) {
        if (StringUtils.isBlank(analysisId)) {
            return Optional.empty();
        }
        try {
            Query<ResultRun> query = session().createQuery("FROM ResultRun r WHERE r.analysisId = :analysis"
                    + " AND r.state IN (:open, :held) ORDER BY r.id DESC", ResultRun.class);
            query.setParameter("analysis", analysisId);
            query.setParameter("open", ResultRunState.OPEN);
            query.setParameter("held", ResultRunState.HELD);
            query.setMaxResults(1);
            return query.uniqueResultOptional();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the open run of one of an analysis", e);
        }
    }

    @Override
    public List<org.openelisglobal.result.valueholder.Result> getResultsOfRun(Long runId) {
        try {
            return session().createQuery("FROM Result r WHERE r.runId = :runId ORDER BY r.id",
                    org.openelisglobal.result.valueholder.Result.class).setParameter("runId", runId).list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the results of run " + runId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getRunIdsOfResults(Collection<String> resultIds) {
        java.util.Map<String, Long> runs = new java.util.HashMap<>();
        if (resultIds == null || resultIds.isEmpty()) {
            return runs;
        }
        try {
            for (Object[] row : session()
                    .createQuery("SELECT r.id, r.runId FROM Result r WHERE r.runId IS NOT NULL AND r.id IN (:ids)",
                            Object[].class)
                    .setParameterList("ids", resultIds).list()) {
                runs.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
            }
            return runs;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the runs of results", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRun> findBatchRun(Long batchWorkplanId) {
        if (batchWorkplanId == null) {
            return Optional.empty();
        }
        try {
            Query<ResultRun> query = session().createQuery(
                    "FROM ResultRun r WHERE r.batchWorkplanId = :batch ORDER BY r.id DESC", ResultRun.class);
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
        return getBatchAnalysisIdsAwaiting(runId, cancelledStatusId, null).size();
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<String> getBatchAnalysisIdsAwaiting(Long runId, String cancelledStatusId, String testId) {
        if (runId == null) {
            return List.of();
        }
        try {
            return ((List<Object>) session().createNativeQuery("SELECT CAST(planned.analysis_id AS VARCHAR) FROM ("
                    + "SELECT item.analysis_id, item.sort_order FROM clinlims.batch_workplan_item item"
                    + " JOIN clinlims.result_run run ON run.batch_workplan_id = item.batch_workplan_id WHERE run.id = :run"
                    + " AND run.id = (SELECT MAX(latest.id) FROM clinlims.result_run latest"
                    + " WHERE latest.batch_workplan_id = run.batch_workplan_id)"
                    + " UNION ALL SELECT run.analysis_id, 1 FROM clinlims.result_run run"
                    + " WHERE run.id = :run AND run.analysis_id IS NOT NULL) planned"
                    + " JOIN clinlims.result_run run ON run.id = :run AND run.state IN ('OPEN', 'HELD')"
                    + " JOIN clinlims.analysis a ON a.id = planned.analysis_id"
                    + " WHERE CAST(a.status_id AS VARCHAR) <> :cancelled AND NOT EXISTS (SELECT 1"
                    + " FROM clinlims.result r WHERE r.analysis_id = a.id AND COALESCE(r.value, '') <> '')"
                    + " AND (CAST(:test AS VARCHAR) IS NULL OR CAST(a.test_id AS VARCHAR) = CAST(:test AS VARCHAR))"
                    + " ORDER BY planned.sort_order, planned.analysis_id").setParameter("run", runId)
                    .setParameter("cancelled", String.valueOf(cancelledStatusId)).setParameter("test", testId)
                    .getResultList()).stream().map(String::valueOf).toList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the analyses a workplan run waits for", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countResultedAnalyses(Long runId) {
        try {
            Long count = session()
                    .createQuery("SELECT COUNT(DISTINCT r.analysis.id) FROM Result r WHERE r.runId = :runId"
                            + " AND r.value IS NOT NULL AND r.value <> ''", Long.class)
                    .setParameter("runId", runId).uniqueResult();
            return count == null ? 0 : count.intValue();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting the analyses resulted in a run", e);
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
                    .createNativeQuery("SELECT DISTINCT CAST(a.test_id AS VARCHAR) FROM clinlims.result_run run"
                            + " JOIN clinlims.analysis a ON a.id = run.analysis_id WHERE run.id = :run"
                            + " UNION SELECT DISTINCT CAST(a.test_id AS VARCHAR) FROM clinlims.batch_workplan_item item"
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
            runs.addAll(getActiveRunsOfOne(analysisIds));
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

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findLastUsedLot(Collection<Long> lotIds, Long excludeRunId) {
        if (lotIds == null || lotIds.isEmpty()) {
            return Optional.empty();
        }
        try {
            return session()
                    .createQuery("SELECT r.reagentLotId FROM ResultRun r WHERE r.reagentLotId IN (:lots)"
                            + " AND r.id <> :run ORDER BY r.lastActivityAt DESC, r.id DESC", Long.class)
                    .setParameterList("lots", lotIds).setParameter("run", excludeRunId == null ? -1L : excludeRunId)
                    .setMaxResults(1).uniqueResultOptional();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the last-used reagent lot", e);
        }
    }

    private Session session() {
        return entityManager.unwrap(Session.class);
    }
}
