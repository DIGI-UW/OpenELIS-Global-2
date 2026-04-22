package org.openelisglobal.vector.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.vector.valueholder.VectorOrganismGroup;

public interface VectorOrganismGroupDAO extends BaseDAO<VectorOrganismGroup, Integer> {

    List<VectorOrganismGroup> getActiveGroups() throws LIMSRuntimeException;

    VectorOrganismGroup getByCode(String code) throws LIMSRuntimeException;
}
