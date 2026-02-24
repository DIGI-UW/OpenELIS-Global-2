package org.openelisglobal.eqa.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAProgramEnrollmentDAO;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAProgramEnrollmentServiceImpl extends BaseObjectServiceImpl<EQAProgramEnrollment, Long>
        implements EQAProgramEnrollmentService {

    @Autowired
    private EQAProgramEnrollmentDAO enrollmentDAO;

    @Autowired
    private EQAProgramService programService;

    @Autowired
    private OrganizationService organizationService;

    public EQAProgramEnrollmentServiceImpl() {
        super(EQAProgramEnrollment.class);
    }

    @Override
    protected EQAProgramEnrollmentDAO getBaseObjectDAO() {
        return enrollmentDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramEnrollment> findByProgramId(Long programId) {
        return enrollmentDAO.findByProgramId(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramEnrollment> findByProgramIdAndStatus(Long programId, String status) {
        return enrollmentDAO.findByProgramIdAndStatus(programId, status);
    }

    @Override
    public EQAProgramEnrollment enrollOrganization(Long programId, Long organizationId, String sysUserId) {
        if (enrollmentDAO.existsActiveEnrollment(programId, organizationId)) {
            throw new IllegalArgumentException(
                    "Organization " + organizationId + " is already actively enrolled in program " + programId);
        }

        EQAProgramEnrollment enrollment = new EQAProgramEnrollment();
        enrollment.setEqaProgram(programService.get(programId));
        enrollment.setOrganizationId(organizationId);
        enrollment.setEnrollmentDate(new Date());
        enrollment.setStatus("Active");
        enrollment.setSysUserId(sysUserId);

        Long id = enrollmentDAO.insert(enrollment);
        return enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve created enrollment"));
    }

    @Override
    public List<EQAProgramEnrollment> bulkEnroll(Long programId, List<Long> organizationIds, String sysUserId) {
        List<EQAProgramEnrollment> enrolled = new ArrayList<>();
        for (Long orgId : organizationIds) {
            if (!enrollmentDAO.existsActiveEnrollment(programId, orgId)) {
                enrolled.add(enrollOrganization(programId, orgId, sysUserId));
            }
        }
        return enrolled;
    }

    @Override
    public EQAProgramEnrollment updateStatus(Long enrollmentId, String newStatus, String reason, String sysUserId) {
        EQAProgramEnrollment enrollment = enrollmentDAO.get(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));

        String currentStatus = enrollment.getStatus();
        validateStatusTransition(currentStatus, newStatus);

        enrollment.setStatus(newStatus);
        enrollment.setStatusChangedDate(new Date());
        enrollment.setStatusChangedBy(Long.valueOf(sysUserId));
        enrollment.setSysUserId(sysUserId);

        if ("Withdrawn".equals(newStatus) && reason != null) {
            enrollment.setWithdrawalReason(reason);
        }

        return enrollmentDAO.update(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEligibleOrganizations(Long programId) {
        List<Organization> allOrgs = organizationService.getAll();
        Set<Long> enrolledOrgIds = findByProgramIdAndStatus(programId, "Active").stream()
                .map(EQAProgramEnrollment::getOrganizationId).collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Organization org : allOrgs) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", org.getId());
            dto.put("organizationName", org.getOrganizationName());
            dto.put("shortName", org.getShortName());
            dto.put("isActive", org.getIsActive());
            dto.put("alreadyEnrolled", enrolledOrgIds.contains(Long.valueOf(org.getId())));
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveEnrollments(Long programId) {
        return enrollmentDAO.findByProgramIdAndStatus(programId, "Active").size();
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        boolean valid = false;
        switch (currentStatus) {
        case "Active":
            valid = "Suspended".equals(newStatus) || "Withdrawn".equals(newStatus);
            break;
        case "Suspended":
            valid = "Active".equals(newStatus) || "Withdrawn".equals(newStatus);
            break;
        case "Withdrawn":
            valid = false;
            break;
        default:
            break;
        }
        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition: " + currentStatus + " → " + newStatus);
        }
    }
}
