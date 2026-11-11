package org.openelisglobal.program.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.program.dao.ProgramDAO;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Programs are audited (OGC-781 FR-8/FR-20): every save writes a history row
 * against the {@code PROGRAM} reference table, so domain changes and
 * deactivate/reactivate flips are traceable.
 */
@Service
public class ProgramServiceImpl extends AuditableBaseObjectServiceImpl<Program, String> implements ProgramService {
    @Autowired
    protected ProgramDAO baseObjectDAO;

    ProgramServiceImpl() {
        super(Program.class);
        this.auditTrailLog = true;
    }

    @Override
    protected ProgramDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
