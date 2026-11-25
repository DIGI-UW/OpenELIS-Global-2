package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;

public interface MicroCaseOrderDetailDAO extends BaseDAO<MicroCaseOrderDetail, String> {

    List<org.openelisglobal.program.valueholder.ProgramSample> getProgramSamples(String caseId);

    MicroCaseOrderDetail getByCaseId(String caseId);

    List<MicroCaseOrderDetail> getByCaseIds(List<String> caseIds);
}
