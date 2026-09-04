package org.openelisglobal.vector.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorTrapType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VectorTrapTypeService extends BaseObjectService<VectorTrapType, Integer> {

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<VectorTrapType> getBySampleTypeId(String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    VectorTrapType patchUpdate(Integer id, VectorTrapType patch, Set<String> sampleTypeIds, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    Integer create(VectorTrapType trapType, Set<String> sampleTypeIds, String sysUserId);
}
