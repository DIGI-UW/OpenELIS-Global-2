package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NceSpecimenService extends BaseObjectService<NceSpecimen, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceSpecimen> getSpecimenByNceId(Integer nceId);

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceSpecimen> getSpecimenBySampleItemId(Integer sampleId);

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId);
}
