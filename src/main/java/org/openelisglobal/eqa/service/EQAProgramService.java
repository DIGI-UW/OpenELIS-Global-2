package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EQAProgramService extends BaseObjectService<EQAProgram, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQAProgram> findActivePrograms();

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAProgram deactivateProgram(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAProgram activateProgram(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQAProgramTest> getTestAssignments(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAProgramTest assignTest(Long programId, Long testId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    void removeTestAssignment(Long programTestId);
}
