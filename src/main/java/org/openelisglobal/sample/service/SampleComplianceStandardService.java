package org.openelisglobal.sample.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleComplianceStandardService extends BaseObjectService<SampleComplianceStandard, Long> {

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<SampleComplianceStandard> getAllForSample(String sampleId);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void replaceAllForSample(String sampleId, List<SampleComplianceStandard> standards);
}
