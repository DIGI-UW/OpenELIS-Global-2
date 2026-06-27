package org.openelisglobal.microbiology.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

public interface MicroBreakpointStandardDAO extends BaseDAO<MicroBreakpointStandard, String> {
    MicroBreakpointStandard getActiveStandard(String authority, String version);
}
