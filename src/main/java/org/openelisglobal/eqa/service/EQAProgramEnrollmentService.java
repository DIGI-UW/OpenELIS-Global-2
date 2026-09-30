package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(read = "PRIV_EQA_VIEW", write = "PRIV_EQA_MANAGE")
public interface EQAProgramEnrollmentService extends BaseObjectService<EQAProgramEnrollment, Long> {

    /**
     * The enrollment status a participant must hold to count as taking part — one
     * spelling, since the cycle gate and the shipment workbench both size a cycle
     * by it.
     */
    String STATUS_ACTIVE = "Active";

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQAProgramEnrollment> findByProgramId(Long programId);

    /** The scheme's active participants, in enrollment order. */
    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQAProgramEnrollment> findActiveByProgramId(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQAProgramEnrollment> findByProgramIdAndStatus(Long programId, String status);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAProgramEnrollment enrollOrganization(Long programId, Long organizationId, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    List<EQAProgramEnrollment> bulkEnroll(Long programId, List<Long> organizationIds, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAProgramEnrollment updateStatus(Long enrollmentId, String newStatus, String reason, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<Map<String, Object>> getEligibleOrganizations(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    long countActiveEnrollments(Long programId);
}
