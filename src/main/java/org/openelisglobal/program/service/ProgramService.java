package org.openelisglobal.program.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.security.access.prepost.PreAuthorize;

// Programmes are an orderable reference list: order entry offers them in a
// dropdown and resolves the chosen one when saving, exactly like sample types
// and panels, so the read side also accepts PRIV_CATALOGUE_VIEW. Configuring
// programmes stays on PRIV_PROGRAM_VIEW's administrative counterparts.
@PreAuthorize("hasAnyAuthority('PRIV_PROGRAM_VIEW','PRIV_CATALOGUE_VIEW')")
public interface ProgramService extends BaseObjectService<Program, String> {
}
