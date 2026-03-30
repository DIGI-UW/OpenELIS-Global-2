package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;

public interface EQAProgramService extends BaseObjectService<EQAProgram, Long> {

    List<EQAProgram> findActivePrograms();

    EQAProgram deactivateProgram(Long programId);

    EQAProgram activateProgram(Long programId);

    List<EQAProgramTest> getTestAssignments(Long programId);

    EQAProgramTest assignTest(Long programId, Long testId);

    void removeTestAssignment(Long programTestId);
}
