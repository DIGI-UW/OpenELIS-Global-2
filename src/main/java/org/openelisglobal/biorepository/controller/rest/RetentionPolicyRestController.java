package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.service.RetentionPolicyService;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy.PeriodUnit;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for retention policy management.
 */
@RestController
@RequestMapping("/rest/biorepository/retention-policies")
public class RetentionPolicyRestController extends BaseRestController {

    @Autowired
    private RetentionPolicyService retentionPolicyService;

    @Autowired
    private HttpServletRequest request;

    /**
     * Get all active retention policies.
     */
    @GetMapping
    public ResponseEntity<List<RetentionPolicyDTO>> getAllPolicies() {
        List<RetentionPolicy> policies = retentionPolicyService.getAllActive();
        List<RetentionPolicyDTO> dtos = policies.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a single retention policy by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPolicy(@PathVariable Integer id) {
        try {
            RetentionPolicy policy = retentionPolicyService.get(id);
            if (policy == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(toDTO(policy));
        } catch (Exception e) {
            // Handle case where entity is not found (throws exception in some DAO impls)
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new retention policy.
     */
    @PostMapping
    public ResponseEntity<?> createPolicy(@RequestBody RetentionPolicyDTO dto) {
        try {
            String sysUserId = getSysUserId();
            if (sysUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
            }

            RetentionPolicy policy = fromDTO(dto);
            policy.setSysUserId(sysUserId);
            policy.setIsActive(true);

            RetentionPolicy saved = retentionPolicyService.save(policy);
            return ResponseEntity.ok(Map.of("success", true, "policy", toDTO(saved)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update an existing retention policy.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable Integer id, @RequestBody RetentionPolicyDTO dto) {
        try {
            String sysUserId = getSysUserId();
            if (sysUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
            }

            RetentionPolicy existing = retentionPolicyService.get(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            // Update fields
            existing.setPolicyName(dto.getPolicyName());
            existing.setProjectId(dto.getProjectId());
            existing.setProjectName(dto.getProjectName());
            existing.setSampleTypeId(dto.getSampleTypeId());
            existing.setSampleTypeName(dto.getSampleTypeName());
            existing.setPeriodValue(dto.getPeriodValue());
            existing.setPeriodUnit(
                    dto.getPeriodUnit() != null ? PeriodUnit.valueOf(dto.getPeriodUnit()) : PeriodUnit.YEARS);
            existing.setDescription(dto.getDescription());
            existing.setSysUserId(sysUserId);

            retentionPolicyService.update(existing);
            return ResponseEntity.ok(Map.of("success", true, "policy", toDTO(existing)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Deactivate (soft delete) a retention policy.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePolicy(@PathVariable Integer id) {
        try {
            RetentionPolicy existing = retentionPolicyService.get(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            retentionPolicyService.deactivate(id);
            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Import retention policies from CSV file.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFromCsv(
            @org.springframework.web.bind.annotation.RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String sysUserId = getSysUserId();
            if (sysUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
            }

            // Read CSV content from uploaded file
            String csvContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);

            List<RetentionPolicy> imported = retentionPolicyService.importFromCsv(csvContent, sysUserId);
            List<RetentionPolicyDTO> dtos = imported.stream().map(this::toDTO).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "imported", dtos.size(), "policies", dtos));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get CSV template for import.
     */
    @GetMapping(value = "/template")
    public void getCsvTemplate(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String template = "Policy Name,Project,Sample Type,Period\n" + "HIV Study Policy,HIV Study 2024,,10 years\n"
                + "Blood Default,,Whole Blood,5 years\n" + "DNA Long-term,,DNA,25 years\n";
        byte[] bytes = template.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        response.setContentType("text/csv");
        response.setContentLength(bytes.length);
        response.setHeader("Content-Disposition", "attachment; filename=retention_policy_template.csv");
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    // ========================================
    // Helper methods
    // ========================================

    private String getSysUserId() {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }

    private RetentionPolicyDTO toDTO(RetentionPolicy policy) {
        RetentionPolicyDTO dto = new RetentionPolicyDTO();
        dto.setId(policy.getId());
        dto.setPolicyName(policy.getPolicyName());
        dto.setProjectId(policy.getProjectId());
        dto.setProjectName(policy.getProjectName());
        dto.setSampleTypeId(policy.getSampleTypeId());
        dto.setSampleTypeName(policy.getSampleTypeName());
        dto.setPeriodValue(policy.getPeriodValue());
        dto.setPeriodUnit(policy.getPeriodUnit() != null ? policy.getPeriodUnit().name() : null);
        dto.setPeriodDisplay(policy.getPeriodDisplay());
        dto.setIsActive(policy.getIsActive());
        dto.setDescription(policy.getDescription());
        return dto;
    }

    private RetentionPolicy fromDTO(RetentionPolicyDTO dto) {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(dto.getPolicyName());
        policy.setProjectId(dto.getProjectId());
        policy.setProjectName(dto.getProjectName());
        policy.setSampleTypeId(dto.getSampleTypeId());
        policy.setSampleTypeName(dto.getSampleTypeName());
        policy.setPeriodValue(dto.getPeriodValue());
        policy.setPeriodUnit(dto.getPeriodUnit() != null ? PeriodUnit.valueOf(dto.getPeriodUnit()) : PeriodUnit.YEARS);
        policy.setDescription(dto.getDescription());
        return policy;
    }

    // ========================================
    // DTO class
    // ========================================

    public static class RetentionPolicyDTO {
        private Integer id;
        private String policyName;
        private Integer projectId;
        private String projectName;
        private Integer sampleTypeId;
        private String sampleTypeName;
        private Integer periodValue;
        private String periodUnit;
        private String periodDisplay;
        private Boolean isActive;
        private String description;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public Integer getProjectId() {
            return projectId;
        }

        public void setProjectId(Integer projectId) {
            this.projectId = projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public Integer getSampleTypeId() {
            return sampleTypeId;
        }

        public void setSampleTypeId(Integer sampleTypeId) {
            this.sampleTypeId = sampleTypeId;
        }

        public String getSampleTypeName() {
            return sampleTypeName;
        }

        public void setSampleTypeName(String sampleTypeName) {
            this.sampleTypeName = sampleTypeName;
        }

        public Integer getPeriodValue() {
            return periodValue;
        }

        public void setPeriodValue(Integer periodValue) {
            this.periodValue = periodValue;
        }

        public String getPeriodUnit() {
            return periodUnit;
        }

        public void setPeriodUnit(String periodUnit) {
            this.periodUnit = periodUnit;
        }

        public String getPeriodDisplay() {
            return periodDisplay;
        }

        public void setPeriodDisplay(String periodDisplay) {
            this.periodDisplay = periodDisplay;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
