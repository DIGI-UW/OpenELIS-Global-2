package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;

public interface EQAProgramService extends BaseObjectService<EQAProgram, Long> {

    List<EQAProgram> findActivePrograms();

    EQAProgram deactivateProgram(Long programId);

    EQAProgram activateProgram(Long programId);

    List<EQAProgramTest> getTestAssignments(Long programId);

    /**
     * Picker label per test id, "Name (Sample type)", since a scheme can hold
     * same-named tests.
     */
    Map<Long, String> getTestLabels(Long programId);

    EQAProgramTest assignTest(Long programId, Long testId);

    void removeTestAssignment(Long programTestId);

    /** The scheme's eligible analysts, the round-robin roster. */
    List<EQASchemeAnalyst> getAnalysts(Long programId);

    /** Replaces the roster with exactly these system users. */
    List<EQASchemeAnalyst> setAnalysts(Long programId, List<Long> systemUserIds, String sysUserId);
}
