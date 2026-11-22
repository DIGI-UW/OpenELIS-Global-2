package org.openelisglobal.configuration.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;

public interface UnresolvedReferenceDAO extends BaseDAO<UnresolvedReference, String> {

    List<UnresolvedReference> getByStatus(String status) throws LIMSRuntimeException;

    /** The open item for this kind and spelling, if one already waits. */
    UnresolvedReference getOpen(String referenceType, String referenceValue) throws LIMSRuntimeException;
}
