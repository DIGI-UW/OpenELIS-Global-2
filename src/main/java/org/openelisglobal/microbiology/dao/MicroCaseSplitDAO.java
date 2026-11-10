package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseSplit;

public interface MicroCaseSplitDAO extends BaseDAO<MicroCaseSplit, String> {

    List<MicroCaseSplit> getByCaseId(String caseId);
}
