package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.dao.EQAProgramTestDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
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

    /**
     * BR-004 (FR-V2.1-06): external arrangement types have a real provider
     * organization behind them; only in-house schemes may omit it. save() delegates
     * to insert/update, so both service-level write paths are covered.
     * activateProgram/deactivateProgram write through the DAO directly and stay
     * exempt — which is what still lets a legacy provider-less scheme be retired.
     */
    @Override
    public Long insert(EQAProgram program) {
        validateProviderRequired(program);
        return super.insert(program);
    }

    @Override
    public EQAProgram update(EQAProgram program) {
        validateProviderRequired(program);
        return super.update(program);
    }

    private void validateProviderRequired(EQAProgram program) {
        if (program.getSchemeType() != EQASchemeType.IN_HOUSE
                && (program.getProvider() == null || program.getProvider().isBlank())) {
            throw new LIMSRuntimeException("Provider is required unless scheme type is IN_HOUSE");
        }
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

        // UNIQUE(eqa_program_id, test_id) outlives removeTestAssignment, which only
        // clears is_active, so re-assigning a test must revive the existing row —
        // a second insert violates the constraint.
        EQAProgramTest programTest = eqaProgramTestDAO.findByProgramId(programId).stream()
                .filter(existing -> testId.equals(existing.getTestId())).findFirst().orElse(null);

        if (programTest != null) {
            programTest.setIsActive(true);
            return eqaProgramTestDAO.update(programTest);
        }

        programTest = new EQAProgramTest();
        programTest.setEqaProgram(program);
        programTest.setTestId(testId);
        programTest.setIsActive(true);
        // sys_user_id is NOT NULL and nothing else populates it; the assignment
        // inherits the scheme's owner, the only identity available here.
        programTest.setSysUserId(program.getSysUserId());
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
