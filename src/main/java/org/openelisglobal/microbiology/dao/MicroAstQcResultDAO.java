package org.openelisglobal.microbiology.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstQcResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

public interface MicroAstQcResultDAO extends BaseDAO<MicroAstQcResult, String> {
    List<MicroAstQcResult> getByRunId(String runId);

    List<MicroAstQcResult> getByControlIds(Collection<String> controlIds);

    Optional<QCRuleViolation> getViolationByTriggeringResultId(String qcResultId);
}
