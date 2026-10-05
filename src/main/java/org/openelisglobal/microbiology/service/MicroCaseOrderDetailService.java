package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseOrderDetailService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCaseOrderDetail saveOrderDetail(String caseId, MicroCaseOrderDetailRequestForm request, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCaseOrderDetail getOrderDetail(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCaseOrderDetail saveOrderDraft(Sample sample, MicroCaseOrderDetailRequestForm request, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCaseOrderDetailRequestForm getOrderDraft(String sampleId);

    /**
     * Drops the details captured before a case existed. An order that no longer
     * qualifies as microbiology keeps nothing; an established case is unaffected.
     */
    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    void discardOrderDraft(String sampleId, String performedBy);
}
