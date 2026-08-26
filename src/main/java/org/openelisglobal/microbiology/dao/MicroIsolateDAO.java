package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;

public interface MicroIsolateDAO extends BaseDAO<MicroIsolate, String> {

    List<MicroIsolate> getByCaseId(String caseId);
}
