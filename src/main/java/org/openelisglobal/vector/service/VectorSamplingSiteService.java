package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VectorSamplingSiteService extends BaseObjectService<VectorSamplingSite, Integer> {

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<VectorSamplingSite> getByType(String type);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<VectorSamplingSite> getActive();

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    VectorSamplingSite getByCode(String code);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<VectorSamplingSite> search(String searchTerm);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    VectorSamplingSite patchUpdate(Integer id, VectorSamplingSite patch, String sysUserId);
}
