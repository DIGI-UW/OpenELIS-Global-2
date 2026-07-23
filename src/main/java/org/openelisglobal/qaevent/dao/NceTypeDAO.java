package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NceType;

public interface NceTypeDAO extends BaseDAO<NceType, Integer> {

    List<NceType> getAllNceType() throws LIMSRuntimeException;

    List<NceType> getNceTypesByCategoryId(Integer categoryId) throws LIMSRuntimeException;
}
