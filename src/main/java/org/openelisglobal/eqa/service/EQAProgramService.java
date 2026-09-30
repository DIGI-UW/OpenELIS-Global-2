package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(read = "PRIV_EQA_VIEW", write = "PRIV_EQA_MANAGE")
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

    /** The scheme's eligible analysts, the round-robin roster. */
    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQASchemeAnalyst> getAnalysts(Long programId);

    /** Replaces the roster with exactly these system users. */
    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    List<EQASchemeAnalyst> setAnalysts(Long programId, List<Long> systemUserIds, String sysUserId);
}
