package org.openelisglobal.eqa.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;

public interface EQALabProgramEnrollmentService extends BaseObjectService<EQALabProgramEnrollment, Long> {

    /**
     * The three spellings the provider side already uses on eqa_program_enrollment.
     */
    String STATUS_ACTIVE = "Active";

    String STATUS_SUSPENDED = "Suspended";

    String STATUS_WITHDRAWN = "Withdrawn";

    List<EQALabProgramEnrollment> findAll();

    List<EQALabProgramEnrollment> findActiveEnrollments();

    /**
     * @param testAnalytes which analyte each mapped test reports for this scheme,
     *                     keyed by test id (qa/030). Optional, but a test with no
     *                     analyte cannot be submitted automatically — see
     *                     {@link EQACycleSubmissionService}.
     */
    EQALabProgramEnrollment createEnrollment(EQALabProgramEnrollment enrollment, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes);

    EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes);

    /**
     * Moves an enrolment between Active, Suspended and Withdrawn. Withdrawn is
     * terminal, as it is for a provider's enrolment: a laboratory that comes back
     * enrols again. The reason and the effective date are both required, and the
     * prior status, the user and the time are written to the audit history.
     */
    EQALabProgramEnrollment updateStatus(Long id, String newStatus, String reason, Date effectiveDate,
            String sysUserId);

    void softDelete(Long id);

    List<String> getDistinctProviders();
}
