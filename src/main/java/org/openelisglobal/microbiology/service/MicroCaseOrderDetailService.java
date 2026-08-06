package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.sample.valueholder.Sample;

public interface MicroCaseOrderDetailService {

    MicroCaseOrderDetail saveOrderDetail(String caseId, MicroCaseOrderDetailRequestForm request, String performedBy);

    MicroCaseOrderDetail getOrderDetail(String caseId);

    MicroCaseOrderDetail saveOrderDraft(Sample sample, MicroCaseOrderDetailRequestForm request, String performedBy);

    MicroCaseOrderDetailRequestForm getOrderDraft(String sampleId);
}
