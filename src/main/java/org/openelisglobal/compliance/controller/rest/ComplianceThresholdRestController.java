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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Group-scoped CRUD on compliance thresholds. Used by the standalone Compliance
 * Standards Admin UI (FRS S-01 v2.3 Screen 1) where thresholds are managed
 * inside a parameter group's accordion, not under a specific test page.
 *
 * Test-scoped reads remain on TestComplianceThresholdRestController.
 */
@RestController
@RequestMapping("/rest/compliance/thresholds")
@PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'RECEPTION', 'RESULTS')")
public class ComplianceThresholdRestController extends BaseRestController {

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    @GetMapping
    public ResponseEntity<List<ComplianceThresholdListItem>> listByGroup(@RequestParam("groupId") String groupId) {
        try {
            return ResponseEntity.ok(complianceThresholdService.getThresholdItemsByGroupId(groupId));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * One row per test that has at least one threshold, with counts. Drives the Tab
     * 2 overview table — it lists tests that already have compliance thresholds so
     * the admin can drill into one without typing into the search box.
     */
    @GetMapping("/summary")
    public ResponseEntity<List<TestThresholdSummary>> summaryByTest() {
        try {
            List<Object[]> rows = complianceThresholdService.getTestThresholdSummary();
            List<TestThresholdSummary> out = new java.util.ArrayList<>(rows == null ? 0 : rows.size());
            if (rows != null) {
                for (Object[] row : rows) {
                    String testId = row[0] == null ? null : row[0].toString();
                    int thresholdCount = row[1] == null ? 0 : ((Number) row[1]).intValue();
                    int standardCount = row[2] == null ? 0 : ((Number) row[2]).intValue();
                    out.add(new TestThresholdSummary(testId, thresholdCount, standardCount));
                }
            }
            return ResponseEntity.ok(out);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static class TestThresholdSummary {
        private final String testId;
        private final int thresholdCount;
        private final int standardCount;

        public TestThresholdSummary(String testId, int thresholdCount, int standardCount) {
            this.testId = testId;
            this.thresholdCount = thresholdCount;
            this.standardCount = standardCount;
        }

        public String getTestId() {
            return testId;
        }

        public int getThresholdCount() {
            return thresholdCount;
        }

        public int getStandardCount() {
            return standardCount;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplianceThresholdListItem> get(@PathVariable String id) {
        try {
            ComplianceThresholdListItem item = complianceThresholdService.getThresholdItem(id);
            if (item == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(item);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceThresholdListItem> create(@Valid @RequestBody ComplianceThreshold threshold,
            HttpServletRequest request) {
        try {
            ComplianceThresholdListItem saved = complianceThresholdService.createThresholdItem(threshold,
                    getSysUserId(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceThresholdListItem> update(@PathVariable String id,
            @Valid @RequestBody ComplianceThreshold threshold, HttpServletRequest request) {
        try {
            ComplianceThresholdListItem updated = complianceThresholdService.updateThresholdItem(id, threshold,
                    getSysUserId(request));
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            ComplianceThreshold threshold = complianceThresholdService.get(id);
            if (threshold == null) {
                return ResponseEntity.notFound().build();
            }
            complianceThresholdService.delete(threshold);
            return ResponseEntity.noContent().build();
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
