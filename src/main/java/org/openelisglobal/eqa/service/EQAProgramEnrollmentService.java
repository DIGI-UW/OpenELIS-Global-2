package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;

public interface EQAProgramEnrollmentService extends BaseObjectService<EQAProgramEnrollment, Long> {

    List<EQAProgramEnrollment> findByProgramId(Long programId);

    List<EQAProgramEnrollment> findByProgramIdAndStatus(Long programId, String status);

    EQAProgramEnrollment enrollOrganization(Long programId, Long organizationId, String sysUserId);

    List<EQAProgramEnrollment> bulkEnroll(Long programId, List<Long> organizationIds, String sysUserId);

    EQAProgramEnrollment updateStatus(Long enrollmentId, String newStatus, String reason, String sysUserId);

    List<Map<String, Object>> getEligibleOrganizations(Long programId);

    long countActiveEnrollments(Long programId);
}
