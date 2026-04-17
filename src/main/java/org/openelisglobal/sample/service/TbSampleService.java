package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TbSampleService {
    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    boolean persistTbData(SampleTbEntryForm form, HttpServletRequest request);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    void getTBFormData(SampleTbEntryForm form);
}
