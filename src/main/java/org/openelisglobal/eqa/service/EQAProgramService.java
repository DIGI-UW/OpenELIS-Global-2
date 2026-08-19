package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;

public interface EQAProgramService extends BaseObjectService<EQAProgram, Long> {

    List<EQAProgram> findActivePrograms();

    EQAProgram deactivateProgram(Long programId);

    EQAProgram activateProgram(Long programId);

    List<EQAProgramTest> getTestAssignments(Long programId);

    EQAProgramTest assignTest(Long programId, Long testId);

    void removeTestAssignment(Long programTestId);

    /** FR-V2.4-03: the scheme's eligible analysts, the round-robin roster. */
    List<EQASchemeAnalyst> getAnalysts(Long programId);

    /** Replaces the roster with exactly these system users. */
    List<EQASchemeAnalyst> setAnalysts(Long programId, List<Long> systemUserIds, String sysUserId);
}
