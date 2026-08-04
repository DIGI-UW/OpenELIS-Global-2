package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointActivationEvent;

public interface MicroBreakpointActivationEventDAO extends BaseDAO<MicroBreakpointActivationEvent, String> {
    List<MicroBreakpointActivationEvent> getByStandardId(String standardId);
}
