package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;

public interface MicroCaseOrderDetailDAO extends BaseDAO<MicroCaseOrderDetail, String> {

    MicroCaseOrderDetail getByCaseId(String caseId);

    MicroCaseOrderDetail getDraftBySampleId(String sampleId);

    /**
     * Removes the pre-case draft for a sample. Details already tied to a case are
     * never touched.
     */
    MicroCaseOrderDetail getAnyDraftBySampleId(String sampleId);

    void discardDraftBySampleId(String sampleId, java.sql.Timestamp discardedAt, String discardedBy);

    List<MicroCaseOrderDetail> getByCaseIds(List<String> caseIds);
}
