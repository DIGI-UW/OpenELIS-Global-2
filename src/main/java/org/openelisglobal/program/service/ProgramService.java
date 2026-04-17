package org.openelisglobal.program.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PROGRAM_VIEW')")
public interface ProgramService extends BaseObjectService<Program, String> {
}
