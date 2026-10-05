package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleEditService {

    @PreAuthorize("hasAuthority('PRIV_ORDER_EDIT')")
    void editSample(SampleEditForm form, HttpServletRequest request, Sample updatedSample, boolean sampleChanged,
            String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<String> getUpdatedAnalysisList();
}
