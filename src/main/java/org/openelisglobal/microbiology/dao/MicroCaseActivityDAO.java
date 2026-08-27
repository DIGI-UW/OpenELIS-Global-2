package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;

public interface MicroCaseActivityDAO extends BaseDAO<MicroCaseActivity, String> {

    List<MicroCaseActivity> getByCaseId(String caseId);
}
