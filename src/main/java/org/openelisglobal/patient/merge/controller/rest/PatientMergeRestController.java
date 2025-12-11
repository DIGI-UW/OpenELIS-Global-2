package org.openelisglobal.patient.merge.controller.rest;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.patient.merge.dto.PatientMergeDetailsDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeExecutionResultDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeValidationResultDTO;
import org.openelisglobal.patient.merge.service.PatientMergeService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Patient Merge operations. Provides endpoints for
 * retrieving merge details, validating merge requests, and executing patient
 * merges.
 *
 * Security: All endpoints require ROLE_GLOBAL_ADMIN enforced programmatically.
 */
@RestController
@RequestMapping("/rest/patient/merge")
public class PatientMergeRestController extends BaseRestController {

    @Autowired
    private PatientMergeService patientMergeService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    private String globalAdminRoleId;

    @PostConstruct
    private void initialize() {
        globalAdminRoleId = roleService.getRoleByName(Constants.ROLE_GLOBAL_ADMIN).getId();
    }

    /**
     * Checks if the current user has Global Administrator role.
     *
     * @param request HTTP request
     * @return true if user has Global Administrator role, false otherwise
     */
    private boolean hasGlobalAdminRole(HttpServletRequest request) {
        String loggedInUserId = getSysUserId(request);
        if (loggedInUserId == null) {
            return false;
        }

        List<String> rolesForLoggedInUser = userRoleService.getRoleIdsForUser(loggedInUserId);
        return rolesForLoggedInUser.contains(globalAdminRoleId);
    }

    /**
     * GET /api/patient/merge/details/{patientId} Retrieves merge details for a
     * specific patient.
     *
     * @param patientId The ID of the patient to get merge details for.
     * @param request   HTTP request for security checks
     * @return PatientMergeDetailsDTO with patient demographics and data summary.
     */
    @GetMapping("/details/{patientId}")
    public ResponseEntity<PatientMergeDetailsDTO> getMergeDetails(@PathVariable String patientId,
            HttpServletRequest request) {
        // Security check: Only Global Administrators can access merge functionality
        if (!hasGlobalAdminRole(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PatientMergeDetailsDTO details = patientMergeService.getMergeDetails(patientId);
        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

    /**
     * POST /api/patient/merge/validate Validates a patient merge request without
     * executing it.
     *
     * @param mergeRequest The merge request containing patient IDs and merge
     *                     parameters.
     * @param httpRequest  HTTP request for security checks
     * @return PatientMergeValidationResultDTO with validation result and any
     *         errors/warnings.
     */
    @PostMapping("/validate")
    public ResponseEntity<PatientMergeValidationResultDTO> validateMerge(
            @Valid @RequestBody PatientMergeRequestDTO mergeRequest, HttpServletRequest httpRequest) {
        // Security check: Only Global Administrators can access merge functionality
        if (!hasGlobalAdminRole(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Validate required fields
        if (mergeRequest.getPatient1Id() == null || mergeRequest.getPatient2Id() == null
                || mergeRequest.getPrimaryPatientId() == null) {
            return ResponseEntity.badRequest().build();
        }

        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(mergeRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/patient/merge/execute Executes a patient merge operation.
     *
     * @param mergeRequest The merge request containing patient IDs and merge
     *                     parameters.
     * @param httpRequest  HTTP request for security checks
     * @return PatientMergeExecutionResultDTO with execution result.
     */
    @PostMapping("/execute")
    public ResponseEntity<PatientMergeExecutionResultDTO> executeMerge(
            @Valid @RequestBody PatientMergeRequestDTO mergeRequest, HttpServletRequest httpRequest) {
        // Security check: Only Global Administrators can access merge functionality
        if (!hasGlobalAdminRole(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Validate required fields (see PatientMergeRequestDTO for design rationale)
        if (mergeRequest.getPatient1Id() == null || mergeRequest.getPatient2Id() == null
                || mergeRequest.getPrimaryPatientId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Check confirmation
        if (!Boolean.TRUE.equals(mergeRequest.getConfirmed())) {
            return ResponseEntity.badRequest().build();
        }

        // Get current user ID from session for audit trail
        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(mergeRequest, sysUserId);

        // Handle failure cases
        if (!result.isSuccess()) {
            String message = result.getMessage() != null ? result.getMessage().toLowerCase() : "";
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}
