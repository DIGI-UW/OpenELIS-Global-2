package org.openelisglobal.vector.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorTrapType;

public interface VectorTrapTypeService extends BaseObjectService<VectorTrapType, Integer> {

    List<VectorTrapType> getBySampleTypeId(String sampleTypeId);

    VectorTrapType patchUpdate(Integer id, VectorTrapType patch, Set<String> sampleTypeIds, String sysUserId);

    Integer create(VectorTrapType trapType, Set<String> sampleTypeIds, String sysUserId);
}
