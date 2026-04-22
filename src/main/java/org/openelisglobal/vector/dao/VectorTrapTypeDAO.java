package org.openelisglobal.vector.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.vector.valueholder.VectorTrapType;

public interface VectorTrapTypeDAO extends BaseDAO<VectorTrapType, Integer> {

    List<VectorTrapType> getByGroupId(Integer groupId) throws LIMSRuntimeException;
}
