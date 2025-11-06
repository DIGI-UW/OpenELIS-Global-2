package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NcEvent;

public interface NCEventDAO extends BaseDAO<NcEvent, String> {

    NcEvent getNCEvent(String id) throws LIMSRuntimeException;

    List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId);
}
