package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
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
 * refused. The EQA REST controllers carry no gates of their own and call that
 * inherited CRUD directly, so the hole was reachable in production. See
 * {@code EQAProgramService} for the case that was verified live, and
 * {@code EqaModuleAccessTest}.
 */
@CrudPrivileges(read = "PRIV_EQA_VIEW", write = "PRIV_EQA_MANAGE")
public interface EQALabProgramEnrollmentService extends BaseObjectService<EQALabProgramEnrollment, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQALabProgramEnrollment> findAll();

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQALabProgramEnrollment> findActiveEnrollments();

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQALabProgramEnrollment createEnrollment(EQALabProgramEnrollment enrollment, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    void softDelete(Long id);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<String> getDistinctProviders();
}
