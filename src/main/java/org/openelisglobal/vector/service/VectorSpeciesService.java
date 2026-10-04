package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.security.access.prepost.PreAuthorize;

/*
 * The sampling sites, species and trap types offered on environmental and
 * vector order entry are catalogue data: the VIEW gates below also accept
 * PRIV_CATALOGUE_VIEW. Creating and editing them keeps PRIV_SAMPLE_TYPE_MANAGE.
 */
public interface VectorSpeciesService extends BaseObjectService<VectorSpecies, Integer> {

    @PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
    List<VectorSpecies> getBySampleTypeId(String sampleTypeId);

    @PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Dictionary> getLifecycleStagesBySampleTypeId(String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    VectorSpecies patchUpdate(Integer id, VectorSpecies patch, String sampleTypeId, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    Integer create(VectorSpecies species, String sampleTypeId, String sysUserId);
}
