package org.openelisglobal.vector.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorTrapType;

public interface VectorTrapTypeService extends BaseObjectService<VectorTrapType, Integer> {

    List<VectorTrapType> getByGroupId(Integer groupId);

    VectorTrapType patchUpdate(Integer id, VectorTrapType patch, Set<Integer> groupIds, String sysUserId);

    Integer create(VectorTrapType trapType, Set<Integer> groupIds, String sysUserId);
}
