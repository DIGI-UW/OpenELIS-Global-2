package org.openelisglobal.observationhistorytype.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;

public interface ObservationHistoryTypeDAO extends BaseDAO<ObservationHistoryType, String> {

    ObservationHistoryType getByName(String name) throws LIMSRuntimeException;
    // insert additional method unique to Demo. History Type here.
}
