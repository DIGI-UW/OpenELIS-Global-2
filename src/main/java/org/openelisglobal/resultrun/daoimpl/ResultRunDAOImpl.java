package org.openelisglobal.resultrun.daoimpl;

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

    private Session session() {
        return entityManager.unwrap(Session.class);
    }
}
