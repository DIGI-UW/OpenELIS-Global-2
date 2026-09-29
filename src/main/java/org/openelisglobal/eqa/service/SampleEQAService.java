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

    /**
     * Order status derived live from the linked order's analyses (OGC-609):
     * COMPLETED when every non-cancelled analysis is finalized, else OVERDUE once
     * the deadline has passed, IN_PROGRESS once any analysis has left NotStarted,
     * else PENDING. Read-side only — no column stores this; the EQA cycle state
     * model replaces it once orders are cycle-linked.
     */
    /**
     * The scheme whose samples this one belongs to, when that scheme captures the
     * analyst on every result.
     *
     * <p>
     * Result entry asks per row, so this answers from the sample rather than making
     * the caller walk sample → cycle → scheme itself.
     *
     * @return the scheme id, or empty when the sample is not EQA, names no cycle,
     *         or its scheme does not capture analysts
     */
    Optional<Long> findPerAnalystSchemeId(Long sampleId);

    /**
     * Whether this sample is an order from an in-house scheme. In-house panels are
     * blinded, and the analyst running them must not be able to tell them from
     * patient samples, so result entry does not mark these rows as EQA.
     *
     * @return false when the sample is not EQA, names no cycle, or its scheme is
     *         not in-house
     */
    boolean isInHouse(Long sampleId);

    String deriveOrderStatus(SampleEQA sampleEQA);
}
