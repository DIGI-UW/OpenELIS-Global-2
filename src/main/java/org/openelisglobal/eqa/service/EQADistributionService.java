package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
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
public interface EQADistributionService extends BaseObjectService<EQADistribution, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQADistribution> findByProgramId(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQADistribution> findByStatus(EQADistributionStatus status);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQADistribution advanceStatus(Long distributionId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    void validateMinParticipants(Long distributionId, int participantCount);
}
