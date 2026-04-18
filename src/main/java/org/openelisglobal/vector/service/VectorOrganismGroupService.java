package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorOrganismGroup;

public interface VectorOrganismGroupService extends BaseObjectService<VectorOrganismGroup, String> {

    List<VectorOrganismGroup> getActiveGroups();

    VectorOrganismGroup getByCode(String code);
}
