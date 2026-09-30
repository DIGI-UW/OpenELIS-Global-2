package org.openelisglobal.eqa.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(read = "PRIV_EQA_VIEW", write = "PRIV_EQA_MANAGE")
public interface EQALabProgramEnrollmentService extends BaseObjectService<EQALabProgramEnrollment, Long> {

    /**
     * The three spellings the provider side already uses on eqa_program_enrollment.
     */
    String STATUS_ACTIVE = "Active";

    String STATUS_SUSPENDED = "Suspended";

    String STATUS_WITHDRAWN = "Withdrawn";

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQALabProgramEnrollment> findAll();

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQALabProgramEnrollment> findActiveEnrollments();

    /**
     * @param testAnalytes which analyte each mapped test reports for this scheme,
     *                     keyed by test id (qa/030). Optional, but a test with no
     *                     analyte cannot be submitted automatically — see
     *                     {@link EQACycleSubmissionService}.
     */
    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQALabProgramEnrollment createEnrollment(EQALabProgramEnrollment enrollment, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes);

    /**
     * Moves an enrolment between Active, Suspended and Withdrawn. Withdrawn is
     * terminal, as it is for a provider's enrolment: a laboratory that comes back
     * enrols again. The reason and the effective date are both required, and the
     * prior status, the user and the time are written to the audit history.
     */
    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQALabProgramEnrollment updateStatus(Long id, String newStatus, String reason, Date effectiveDate,
            String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    void softDelete(Long id);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<String> getDistinctProviders();
}
