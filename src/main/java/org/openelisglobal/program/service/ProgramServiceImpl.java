package org.openelisglobal.program.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.program.dao.ProgramDAO;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProgramServiceImpl extends AuditableBaseObjectServiceImpl<Program, String> implements ProgramService {
    @Autowired
    protected ProgramDAO baseObjectDAO;

    ProgramServiceImpl() {
        super(Program.class);
    }

    @Override
    protected ProgramDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
