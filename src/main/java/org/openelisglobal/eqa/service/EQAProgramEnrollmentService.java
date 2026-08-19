package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;

public interface EQAProgramEnrollmentService extends BaseObjectService<EQAProgramEnrollment, Long> {

    /**
     * The enrollment status a participant must hold to count as taking part
     * (BR-013) — one spelling, since the cycle gate and the shipment workbench both
     * size a cycle by it.
     */
    String STATUS_ACTIVE = "Active";

    List<EQAProgramEnrollment> findByProgramId(Long programId);

    /** The scheme's active participants, in enrollment order. */
    List<EQAProgramEnrollment> findActiveByProgramId(Long programId);

    List<EQAProgramEnrollment> findByProgramIdAndStatus(Long programId, String status);

    EQAProgramEnrollment enrollOrganization(Long programId, Long organizationId, String sysUserId);

    List<EQAProgramEnrollment> bulkEnroll(Long programId, List<Long> organizationIds, String sysUserId);

    EQAProgramEnrollment updateStatus(Long enrollmentId, String newStatus, String reason, String sysUserId);

    List<Map<String, Object>> getEligibleOrganizations(Long programId);

    long countActiveEnrollments(Long programId);
}
