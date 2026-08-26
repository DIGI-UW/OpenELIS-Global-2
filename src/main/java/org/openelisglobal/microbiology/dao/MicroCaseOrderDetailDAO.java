package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;

public interface MicroCaseOrderDetailDAO extends BaseDAO<MicroCaseOrderDetail, String> {

    MicroCaseOrderDetail getByCaseId(String caseId);

    MicroCaseOrderDetail getDraftBySampleId(String sampleId);

    List<MicroCaseOrderDetail> getByCaseIds(List<String> caseIds);
}
