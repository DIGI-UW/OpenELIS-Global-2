package org.openelisglobal.qc.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.form.QCViolationForm;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for QC Rule Violations (T111).
 *
 * Provides endpoints for viewing and managing QC rule violations. Following
 * Constitution IV.5: NO @Transactional (belongs in service layer)
 */
@RestController
@RequestMapping("/rest/qc/violations")
public class QCViolationRestController {

    @Autowired
    private QCRuleViolationService violationService;

    /**
     * Get all violations with optional filtering.
     *
     * @param instrumentId Optional filter by instrument
     * @param severity     Optional filter by severity (WARNING, REJECTION)
     * @param unresolved   If true, return only unresolved violations
     * @return List of violations
     */
    @GetMapping
    public ResponseEntity<List<QCViolationForm>> getViolations(@RequestParam(required = false) Integer instrumentId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false, defaultValue = "false") Boolean unresolved) {

        List<QCRuleViolation> violations;

        if (instrumentId != null && unresolved) {
            violations = violationService.findUnresolvedByInstrument(instrumentId);
        } else if (instrumentId != null) {
            violations = violationService.findByInstrument(instrumentId);
        } else if (severity != null) {
            violations = violationService.findBySeverity(severity);
        } else if (unresolved) {
            violations = violationService.findUnresolved();
        } else {
            // Default: return unresolved violations
            violations = violationService.findUnresolved();
        }

        List<QCViolationForm> forms = violations.stream().map(this::toForm).collect(Collectors.toList());

        return ResponseEntity.ok(forms);
    }

    /**
     * Get a specific violation by ID.
     *
     * @param id The violation ID
     * @return The violation or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<QCViolationForm> getViolation(@PathVariable String id) {
        QCRuleViolation violation = violationService.getById(id);

        if (violation == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toForm(violation));
    }

    /**
     * Resolve a violation.
     *
     * @param id      The violation ID
     * @param request The resolution request containing notes
     * @return The resolved violation
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<QCViolationForm> resolveViolation(@PathVariable String id,
            @Valid @RequestBody QCViolationForm.ResolveRequest request) {

        // In a real implementation, get user ID from security context
        Integer userId = getCurrentUserId();

        QCRuleViolation violation = violationService.resolveViolation(id, userId, request.getNotes());

        if (violation == null) {
            return ResponseEntity.notFound().build();
        }

        LogEvent.logInfo(this.getClass().getName(), "resolveViolation",
                "Violation " + id + " resolved by user " + userId);

        return ResponseEntity.ok(toForm(violation));
    }

    /**
     * Acknowledge a warning violation (marks as seen but not fully resolved).
     *
     * @param id The violation ID
     * @return The acknowledged violation
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<QCViolationForm> acknowledgeViolation(@PathVariable String id) {
        // In a real implementation, get user ID from security context
        Integer userId = getCurrentUserId();

        QCRuleViolation violation = violationService.acknowledgeViolation(id, userId);

        if (violation == null) {
            return ResponseEntity.notFound().build();
        }

        LogEvent.logInfo(this.getClass().getName(), "acknowledgeViolation",
                "Violation " + id + " acknowledged by user " + userId);

        return ResponseEntity.ok(toForm(violation));
    }

    /**
     * Get count of unresolved violations by severity.
     *
     * @return Counts by severity
     */
    @GetMapping("/counts")
    public ResponseEntity<ViolationCounts> getViolationCounts() {
        ViolationCounts counts = new ViolationCounts();
        counts.setRejectionCount(violationService.getUnresolvedCountBySeverity("REJECTION"));
        counts.setWarningCount(violationService.getUnresolvedCountBySeverity("WARNING"));
        counts.setTotalCount(counts.getRejectionCount() + counts.getWarningCount());

        return ResponseEntity.ok(counts);
    }

    /**
     * Convert entity to form DTO.
     */
    private QCViolationForm toForm(QCRuleViolation violation) {
        QCViolationForm form = new QCViolationForm();
        form.setId(violation.getId());
        form.setTriggeringResultId(violation.getTriggeringResultId());
        form.setRuleCode(violation.getRuleCode());
        form.setViolationDateTime(violation.getViolationDateTime());
        form.setSeverity(violation.getSeverity());
        form.setInstrumentId(violation.getInstrumentId());
        form.setTestId(violation.getTestId());
        form.setResolutionStatus(violation.getResolutionStatus());
        form.setResolvedDateTime(violation.getResolvedDateTime());
        form.setResolvedByUserId(violation.getResolvedByUserId());
        form.setResolutionNotes(violation.getResolutionNotes());

        // Add rule description for common rules
        form.setRuleDescription(getRuleDescription(violation.getRuleCode()));

        return form;
    }

    /**
     * Get human-readable description for a rule code.
     */
    private String getRuleDescription(String ruleCode) {
        switch (ruleCode) {
        case "1₂ₛ":
            return "Single result exceeds ±2SD (Warning)";
        case "1₃ₛ":
            return "Single result exceeds ±3SD (Rejection)";
        case "2₂ₛ":
            return "Two consecutive results exceed same ±2SD limit";
        case "R₄ₛ":
            return "Range between consecutive results exceeds 4SD";
        case "4₁ₛ":
            return "Four consecutive results exceed same ±1SD limit";
        case "10ₓ":
            return "Ten consecutive results on same side of mean";
        case "3₁ₛ":
            return "Three consecutive results exceed same ±1SD limit (Warning)";
        case "7ₜ":
            return "Seven consecutive results showing consistent trend";
        default:
            return ruleCode;
        }
    }

    /**
     * Get current user ID from security context. In a real implementation, this
     * would get from authentication.
     */
    private Integer getCurrentUserId() {
        // Placeholder - in real implementation, get from Spring Security
        return 1;
    }

    /**
     * DTO for violation counts.
     */
    public static class ViolationCounts {
        private int totalCount;
        private int rejectionCount;
        private int warningCount;

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getRejectionCount() {
            return rejectionCount;
        }

        public void setRejectionCount(int rejectionCount) {
            this.rejectionCount = rejectionCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public void setWarningCount(int warningCount) {
            this.warningCount = warningCount;
        }
    }
}
