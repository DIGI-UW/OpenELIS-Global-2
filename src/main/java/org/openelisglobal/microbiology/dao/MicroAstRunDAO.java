package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;

public interface MicroAstRunDAO extends BaseDAO<MicroAstRun, String> {

    List<MicroAstRun> getByIsolateId(String isolateId);

    List<MicroAstRun> getByIsolateIds(List<String> isolateIds);

    List<MicroAstRun> getByAmendmentId(String amendmentId);

    Optional<MicroAstRun> getByAnalyzerAndCard(String analyzerId, String cardId);

    long countUnresolvedByBreakpointStandardId(String breakpointStandardId);
}
