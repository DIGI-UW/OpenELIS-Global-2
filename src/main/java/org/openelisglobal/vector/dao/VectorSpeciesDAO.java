package org.openelisglobal.vector.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.vector.valueholder.VectorSpecies;

public interface VectorSpeciesDAO extends BaseDAO<VectorSpecies, String> {

    List<VectorSpecies> getByGroupId(String groupId) throws LIMSRuntimeException;
}
