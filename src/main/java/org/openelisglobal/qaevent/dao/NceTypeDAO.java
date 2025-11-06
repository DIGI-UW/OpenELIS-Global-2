package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NceType;

public interface NceTypeDAO extends BaseDAO<NceType, String> {

    List<NceType> getAllNceType() throws LIMSRuntimeException;
}
