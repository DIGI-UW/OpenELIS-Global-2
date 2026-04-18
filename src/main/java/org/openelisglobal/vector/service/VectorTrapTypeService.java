package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorTrapType;

public interface VectorTrapTypeService extends BaseObjectService<VectorTrapType, String> {

    List<VectorTrapType> getByGroupId(String groupId);
}
