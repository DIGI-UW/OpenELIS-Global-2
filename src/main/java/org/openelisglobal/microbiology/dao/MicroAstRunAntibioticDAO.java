package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;

public interface MicroAstRunAntibioticDAO extends BaseDAO<MicroAstRunAntibiotic, String> {

    List<MicroAstRunAntibiotic> getByRunId(String runId);

    Optional<MicroAstRunAntibiotic> getByRunIdAndAntibioticId(String runId, String antibioticId);
}
