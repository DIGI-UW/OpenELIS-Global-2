package org.openelisglobal.resultrun.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.resultrun.valueholder.ResultRun;

public interface ResultRunDAO extends BaseDAO<ResultRun, Long> {

    /** The run one accepted analyzer message created. */
    Optional<ResultRun> findAnalyzerRun(String connectionId, String messageId);

    /** Runs with rows still to work (Open or Held), newest first. */
    List<ResultRun> getActiveRuns();

    /** The values of the given runs still waiting in staging, in arrival order. */
    List<AnalyzerResults> getStagedRows(Collection<Long> runIds);

    long countStagedRows(Long runId);

    /**
     * Patient values waiting in an active run for any of these lab numbers: the
     * rows a worklist must not let anyone result around.
     */
    List<AnalyzerResults> getWaitingRowsForAccessions(Collection<String> accessionNumbers);
}
