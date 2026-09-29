package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Inherited CRUD is gated the same way this interface's own methods are:
 * eqa:view to read, eqa:manage to write.
 *
 * <p>
 * Without this, {@link org.openelisglobal.common.security.CrudGate} found
 * neither a {@code @CrudPrivileges} nor a type-level {@code @PreAuthorize} here
 * and fell through to its open branch, so getAll/insert/update/delete were
 * callable by any authenticated user while the declared finders were correctly
 * refused. That was reachable in production, and was verified live on this
 * service: the Results role, holding no eqa:* privilege, read the whole
 * programme list through {@code EQAProgramRestController.listPrograms} (which
 * calls getAll), and {@code createProgram} committed a new programme through
 * {@code insert} before the handler's next gated call failed and turned the
 * response into a misleading 400 that looked like a refusal.
 */
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
}
