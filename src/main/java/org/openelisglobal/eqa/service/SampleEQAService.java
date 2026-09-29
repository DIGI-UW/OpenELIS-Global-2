package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.SampleEQA;
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
public interface SampleEQAService extends BaseObjectService<SampleEQA, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    Optional<SampleEQA> findBySampleId(Long sampleId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<SampleEQA> findByDeadlineBefore(Timestamp deadline);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<SampleEQA> findByProgramId(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<SampleEQA> findEqaSamples();
}
