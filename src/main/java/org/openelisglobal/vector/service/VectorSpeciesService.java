package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VectorSpeciesService extends BaseObjectService<VectorSpecies, Integer> {

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<VectorSpecies> getBySampleTypeId(String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<Dictionary> getLifecycleStagesBySampleTypeId(String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    VectorSpecies patchUpdate(Integer id, VectorSpecies patch, String sampleTypeId, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    Integer create(VectorSpecies species, String sampleTypeId, String sysUserId);
}
