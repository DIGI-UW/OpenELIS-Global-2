package org.openelisglobal.configuration.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.configuration.valueholder.ReferenceAlias;

public interface ReferenceAliasDAO extends BaseDAO<ReferenceAlias, String> {

    ReferenceAlias getByTypeAndAlias(String referenceType, String normalizedAlias) throws LIMSRuntimeException;
}
