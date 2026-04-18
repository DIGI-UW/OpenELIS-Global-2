package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorSpecies;

public interface VectorSpeciesService extends BaseObjectService<VectorSpecies, String> {

    List<VectorSpecies> getByGroupId(String groupId);
}
