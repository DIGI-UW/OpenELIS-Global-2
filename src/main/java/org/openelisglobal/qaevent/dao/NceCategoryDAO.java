package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NceCategory;

public interface NceCategoryDAO extends BaseDAO<NceCategory, String> {

    List<NceCategory> getAllNceCategory() throws LIMSRuntimeException;
}
