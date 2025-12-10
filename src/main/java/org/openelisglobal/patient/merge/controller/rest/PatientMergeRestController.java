package org.openelisglobal.patient.merge.controller.rest;

import javax.validation.Valid;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.patient.merge.dto.PatientMergeDetailsDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeExecutionResultDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeValidationResultDTO;
import org.openelisglobal.patient.merge.service.PatientMergeService;
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
 * REST Controller for Patient Merge operations.
 * Provides endpoints for retrieving merge details, validating merge requests,
 * and executing patient merges.
 *
 * Security: All endpoints require ROLE_GLOBAL_ADMIN (to be enforced via Spring
 * Security config).
 */
@RestController
@RequestMapping("/rest/patient/merge")
public class PatientMergeRestController extends BaseRestController {

    @Autowired
    private PatientMergeService patientMergeService;

    /**
     * GET /api/patient/merge/details/{patientId}
     * Retrieves merge details for a specific patient.
     *
     * @param patientId The ID of the patient to get merge details for.
     * @return PatientMergeDetailsDTO with patient demographics and data summary.
     */
    @GetMapping("/details/{patientId}")
    public ResponseEntity<PatientMergeDetailsDTO> getMergeDetails(@PathVariable String patientId) {
        PatientMergeDetailsDTO details = patientMergeService.getMergeDetails(patientId);
        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

    /**
     * POST /api/patient/merge/validate
     * Validates a patient merge request without executing it.
     *
     * @param request The merge request containing patient IDs and merge parameters.
     * @return PatientMergeValidationResultDTO with validation result and any
     *         errors/warnings.
     */
    @PostMapping("/validate")
    public ResponseEntity<PatientMergeValidationResultDTO> validateMerge(
            @Valid @RequestBody PatientMergeRequestDTO request) {
        // Validate required fields
        if (request.getPatient1Id() == null || request.getPatient2Id() == null
                || request.getPrimaryPatientId() == null) {
            return ResponseEntity.badRequest().build();
        }

        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(request);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/patient/merge/execute
     * Executes a patient merge operation.
     *
     * @param request The merge request containing patient IDs and merge parameters.
     * @return PatientMergeExecutionResultDTO with execution result.
     */
    @PostMapping("/execute")
    public ResponseEntity<PatientMergeExecutionResultDTO> executeMerge(
            @Valid @RequestBody PatientMergeRequestDTO request) {
        // Validate required fields
        if (request.getPatient1Id() == null || request.getPatient2Id() == null
                || request.getPrimaryPatientId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Check confirmation
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            return ResponseEntity.badRequest().build();
        }

        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(request);

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
