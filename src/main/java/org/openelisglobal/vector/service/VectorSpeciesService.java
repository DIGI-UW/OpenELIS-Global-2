package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorSpecies;

public interface VectorSpeciesService extends BaseObjectService<VectorSpecies, Integer> {

    List<VectorSpecies> getByGroupId(Integer groupId);

    VectorSpecies patchUpdate(Integer id, VectorSpecies patch, Integer groupId, String sysUserId);

    Integer create(VectorSpecies species, Integer groupId, String sysUserId);
}
