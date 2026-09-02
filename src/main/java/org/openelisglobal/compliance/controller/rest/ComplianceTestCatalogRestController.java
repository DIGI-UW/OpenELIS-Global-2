package org.openelisglobal.compliance.controller.rest;

import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.compliance.service.ComplianceTestCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only catalog endpoint for the compliance UI. Augments the basic
 * /rest/test-list response with per-test sample types, result type, and (for
 * dictionary-backed tests) the predefined select options. The compliance Link
 * Test form needs all of these to render the right input (multi-limit numeric
 * vs select-list mapping) without forcing the admin to retype the data.
 *
 * <p>
 * Assembly logic and the read-only transactional boundary live in
 * {@link ComplianceTestCatalogService} so the controller stays a thin
 * request/response mapper.
 */
@RestController
@RequestMapping("/rest/compliance")
@PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'RECEPTION', 'RESULTS')")
public class ComplianceTestCatalogRestController extends BaseRestController {

    @Autowired
    private ComplianceTestCatalogService complianceTestCatalogService;

    /**
     * Catalog rows for tests that have at least one compliance threshold linked.
     * Each row carries the same fields as {@code /test-catalog} plus
     * {@code thresholdCount} and {@code standardCount}. Backs the Tab 2 overview
     * table — assembling the list server-side avoids the brittle "match by id
     * across two arrays" pattern in the client.
     */
    @GetMapping(value = "/test-catalog/with-compliance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TestCatalogEntryWithCompliance>> getCatalogWithCompliance() {
        return ResponseEntity.ok(complianceTestCatalogService.getCatalogWithCompliance());
    }

    @GetMapping(value = "/test-catalog", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TestCatalogEntry>> getCatalog() {
        return ResponseEntity.ok(complianceTestCatalogService.getCatalog());
    }

    /**
     * Distinct sample-type category names available in the system. Drives the
     * Applicable Sample Types dropdown on the Standard form so the admin can only
     * pick categories that actually exist in the test catalog.
     */
    @GetMapping(value = "/sample-type-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getSampleTypeCategories() {
        return ResponseEntity.ok(complianceTestCatalogService.getSampleTypeCategories());
    }
}
