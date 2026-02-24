package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.eqa.service.EQAProgramEnrollmentService;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa")
public class EQAEnrollmentRestController extends ControllerUtills {

    @Autowired
    private EQAProgramEnrollmentService enrollmentService;

    @GetMapping(value = "/programs/{programId}/enrollments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> listEnrollments(@PathVariable Long programId) {
        List<EQAProgramEnrollment> enrollments = enrollmentService.findByProgramId(programId);
        List<Map<String, Object>> dtos = enrollments.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping(value = "/programs/{programId}/enrollments", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> createEnrollments(HttpServletRequest request, @PathVariable Long programId,
            @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Number> orgIds = (List<Number>) body.get("organizationIds");
            if (orgIds == null || orgIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "organizationIds list is required"));
            }

            List<Long> organizationIds = orgIds.stream().map(Number::longValue).collect(Collectors.toList());
            String sysUserId = getSysUserId(request);

            List<EQAProgramEnrollment> enrolled = enrollmentService.bulkEnroll(programId, organizationIds, sysUserId);
            List<Map<String, Object>> dtos = enrolled.stream().map(this::toDto).collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/programs/{programId}/enrollments/{enrollmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> updateEnrollmentStatus(HttpServletRequest request, @PathVariable Long programId,
            @PathVariable Long enrollmentId, @RequestBody Map<String, Object> body) {
        try {
            String status = (String) body.get("status");
            String reason = (String) body.get("reason");

            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }

            String sysUserId = getSysUserId(request);
            EQAProgramEnrollment updated = enrollmentService.updateStatus(enrollmentId, status, reason, sysUserId);

            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/eligible-organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getEligibleOrganizations(@RequestParam Long programId) {
        List<Map<String, Object>> orgs = enrollmentService.getEligibleOrganizations(programId);
        return ResponseEntity.ok(orgs);
    }

    private Map<String, Object> toDto(EQAProgramEnrollment enrollment) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", enrollment.getId());
        dto.put("programId", enrollment.getEqaProgram() != null ? enrollment.getEqaProgram().getId() : null);
        dto.put("organizationId", enrollment.getOrganizationId());
        dto.put("enrollmentDate", enrollment.getEnrollmentDate());
        dto.put("status", enrollment.getStatus());
        dto.put("statusChangedDate", enrollment.getStatusChangedDate());
        dto.put("statusChangedBy", enrollment.getStatusChangedBy());
        dto.put("withdrawalReason", enrollment.getWithdrawalReason());
        return dto;
    }
}
