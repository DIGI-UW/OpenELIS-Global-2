package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;

public interface MicroAstRunDAO extends BaseDAO<MicroAstRun, String> {

    List<MicroAstRun> getByIsolateId(String isolateId);
}
