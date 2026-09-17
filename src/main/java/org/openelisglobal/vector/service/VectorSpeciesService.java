package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.vector.valueholder.VectorSpecies;

public interface VectorSpeciesService extends BaseObjectService<VectorSpecies, Integer> {

    List<VectorSpecies> getBySampleTypeId(String sampleTypeId);

    List<Dictionary> getLifecycleStagesBySampleTypeId(String sampleTypeId);

    VectorSpecies patchUpdate(Integer id, VectorSpecies patch, String sampleTypeId, String sysUserId);

    Integer create(VectorSpecies species, String sampleTypeId, String sysUserId);
}
