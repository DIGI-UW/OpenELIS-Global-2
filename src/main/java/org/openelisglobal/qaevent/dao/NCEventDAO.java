package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NcEvent;

public interface NCEventDAO extends BaseDAO<NcEvent, Integer> {

    NcEvent getNCEvent(String id) throws LIMSRuntimeException;

    List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId);

    /**
     * The NCE created for a trigger source (e.g. a QC violation), or null. At most
     * one exists per source (uq_nc_event_trigger_source).
     */
    NcEvent findByTriggerSource(String triggerSourceType, String triggerSourceId);
}
