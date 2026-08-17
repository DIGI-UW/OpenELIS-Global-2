package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.dao.EQAProgramTestDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAProgramServiceImpl extends BaseObjectServiceImpl<EQAProgram, Long> implements EQAProgramService {

    @Autowired
    private EQAProgramDAO eqaProgramDAO;

    @Autowired
    private EQAProgramTestDAO eqaProgramTestDAO;

    public EQAProgramServiceImpl() {
        super(EQAProgram.class);
    }

    @Override
    protected EQAProgramDAO getBaseObjectDAO() {
        return eqaProgramDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgram> findActivePrograms() {
        return eqaProgramDAO.findByIsActive(true);
    }

    @Override
    public EQAProgram deactivateProgram(Long programId) {
        EQAProgram program = eqaProgramDAO.get(programId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program not found: " + programId));
        program.setIsActive(false);
        return eqaProgramDAO.update(program);
    }

    @Override
    public EQAProgram activateProgram(Long programId) {
        EQAProgram program = eqaProgramDAO.get(programId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program not found: " + programId));
        program.setIsActive(true);
        return eqaProgramDAO.update(program);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramTest> getTestAssignments(Long programId) {
        return eqaProgramTestDAO.findByProgramId(programId);
    }

    @Override
    public EQAProgramTest assignTest(Long programId, Long testId) {
        EQAProgram program = eqaProgramDAO.get(programId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program not found: " + programId));

        EQAProgramTest programTest = new EQAProgramTest();
        programTest.setEqaProgram(program);
        programTest.setTestId(testId);
        programTest.setIsActive(true);
        eqaProgramTestDAO.insert(programTest);
        return programTest;
    }

    @Override
    public void removeTestAssignment(Long programTestId) {
        EQAProgramTest programTest = eqaProgramTestDAO.get(programTestId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program Test not found: " + programTestId));
        programTest.setIsActive(false);
        eqaProgramTestDAO.update(programTest);
    }
}
