package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroCaseDAO extends BaseDAO<MicroCase, String> {

    MicroCase getBySampleItemAndWorkflow(String sampleItemId, String workflowType);

    List<MicroCase> getBySampleItem(String sampleItemId);

    List<MicroCase> getOpenCases();
}
