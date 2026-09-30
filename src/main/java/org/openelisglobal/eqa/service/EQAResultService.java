package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
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
public interface EQAResultService extends BaseObjectService<EQAResult, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAResult submitResult(Long distributionId, Long organizationId, Long testId, java.math.BigDecimal resultValue,
            EQASubmissionMethod method, String sysUserId);

    /**
     * Provider-side intake of a value as the participant reported it: a number
     * lands in result_value, anything else ("Reactive", "Scanty") in result_text.
     * The provider is the authority on what arrived, so a value after the deadline
     * is recorded as late rather than refused. {@code panelSampleId} names the
     * panel sample the value answers, or is null for a test with no sample behind
     * it.
     */
    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAResult submitReportedValue(Long distributionId, Long organizationId, Long testId, Long panelSampleId,
            String reported, EQASubmissionMethod method, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQAResult> findByDistributionId(Long distributionId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    long countByDistributionId(Long distributionId);
}
