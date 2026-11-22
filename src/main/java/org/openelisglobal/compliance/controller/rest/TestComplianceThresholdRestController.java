package org.openelisglobal.compliance.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.compliance.service.ComplianceThresholdService;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing compliance thresholds on test catalog entries.
 *
 * Provides endpoints for linking compliance standards to test catalog entries
 * via threshold configurations. Follows OpenELIS patterns for test-specific
 * configuration endpoints.
 *
 * Implements the Test-Threshold API requirements from FR-3-xxx.
 */
@RestController
@RequestMapping("/rest/tests")
@PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'RECEPTION', 'RESULTS')")
public class TestComplianceThresholdRestController extends BaseRestController {

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    /**
     * Get all compliance thresholds for a specific test.
     */
    @GetMapping("/{testId}/compliance-thresholds")
    public ResponseEntity<List<ComplianceThresholdListItem>> getTestThresholds(@PathVariable String testId) {
        try {
            return ResponseEntity.ok(complianceThresholdService.getThresholdItemsByTestId(testId));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific compliance threshold by ID. The threshold must belong to the
     * test in the URL path; otherwise 404.
     */
    @GetMapping("/{testId}/compliance-thresholds/{thresholdId}")
    public ResponseEntity<ComplianceThresholdListItem> getTestThreshold(@PathVariable String testId,
            @PathVariable String thresholdId) {
        try {
            ComplianceThresholdListItem item = complianceThresholdService.getThresholdItem(thresholdId);
            if (item == null || !testId.equals(item.getTestId())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(item);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new compliance threshold for a test. The path testId wins over any
     * value in the body — we pin it before delegating so the threshold is always
     * parented to the test the URL identifies.
     */
    @PostMapping("/{testId}/compliance-thresholds")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceThresholdListItem> createTestThreshold(@PathVariable String testId,
            @Valid @RequestBody ComplianceThreshold threshold, HttpServletRequest request) {
        try {
            // Pin the test ref to the path id (stub is fine — service swaps
            // it for the managed entity inside the transaction).
            threshold.setTestId(testId);
            ComplianceThresholdListItem saved = complianceThresholdService.createThresholdItem(threshold,
                    getSysUserId(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update an existing compliance threshold. Refuses with 404 if the threshold
     * doesn't already belong to the test in the URL path.
     */
    @PutMapping("/{testId}/compliance-thresholds/{thresholdId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceThresholdListItem> updateTestThreshold(@PathVariable String testId,
            @PathVariable String thresholdId, @Valid @RequestBody ComplianceThreshold threshold,
            HttpServletRequest request) {
        try {
            // Ownership check — must already belong to this test.
            ComplianceThresholdListItem existing = complianceThresholdService.getThresholdItem(thresholdId);
            if (existing == null || !testId.equals(existing.getTestId())) {
                return ResponseEntity.notFound().build();
            }
            threshold.setTestId(testId);
            ComplianceThresholdListItem updated = complianceThresholdService.updateThresholdItem(thresholdId, threshold,
                    getSysUserId(request));
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Remove/archive a compliance threshold.
     */
    @DeleteMapping("/{testId}/compliance-thresholds/{thresholdId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> deleteTestThreshold(@PathVariable String testId, @PathVariable String thresholdId) {
        try {
            ComplianceThreshold threshold = complianceThresholdService.get(thresholdId);
            if (threshold == null || !testId.equals(threshold.getTestId())) {
                return ResponseEntity.notFound().build();
            }
            complianceThresholdService.delete(threshold);
            return ResponseEntity.noContent().build();
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Get compliance thresholds for a test by specific standard.
     */
    @GetMapping("/{testId}/compliance-thresholds/standard/{standardId}")
    public ResponseEntity<List<ComplianceThresholdListItem>> getTestThresholdsByStandard(@PathVariable String testId,
            @PathVariable String standardId) {
        try {
            return ResponseEntity.ok(complianceThresholdService.getThresholdItemsByTestAndStandard(testId, standardId));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}