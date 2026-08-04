package org.openelisglobal.microbiology.service;

import java.sql.Date;

public interface MicroBreakpointAdminService {

    void activate(String standardId, Date effectiveDate, String actorId);

    void archive(String standardId, String actorId);
}
