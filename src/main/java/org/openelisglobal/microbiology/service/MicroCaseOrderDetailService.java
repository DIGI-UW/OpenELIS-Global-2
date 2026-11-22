package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;

public interface MicroCaseOrderDetailService {

    MicroCaseOrderDetail saveOrderDetail(String caseId, MicroCaseOrderDetailRequestForm request, String performedBy);

    MicroCaseOrderDetail getOrderDetail(String caseId);
}
