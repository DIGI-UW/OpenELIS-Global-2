package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.sample.valueholder.Sample;

public interface MicroCaseOrderDetailService {

    MicroCaseOrderDetail saveOrderDetail(String caseId, MicroCaseOrderDetailRequestForm request, String performedBy);

    MicroCaseOrderDetail getOrderDetail(String caseId);

    MicroCaseOrderDetail saveOrderDraft(Sample sample, MicroCaseOrderDetailRequestForm request, String performedBy);

    MicroCaseOrderDetailRequestForm getOrderDraft(String sampleId);

    /**
     * Drops the details captured before a case existed. An order that no longer
     * qualifies as microbiology keeps nothing; an established case is unaffected.
     */
    void discardOrderDraft(String sampleId, String performedBy);
}
