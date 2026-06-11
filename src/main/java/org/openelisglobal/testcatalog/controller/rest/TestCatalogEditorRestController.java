package org.openelisglobal.testcatalog.controller.rest;

import java.util.List;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-949 M2 / OGC-927 — unified Test Catalog editor shell backend.
 *
 * Foundation envelope only: loads the identity + which sections apply for a
 * test's domain, which the SideNav-routed editor shell hydrates from.
 * Per-section load/save lands in the section milestones (M4+). Gated by
 * ROLE_ADMIN (FR-004) — matches existing OE admin REST controllers; non-admins
 * get 403.
 *
 * Base path /rest/test-catalog avoids colliding with the existing singular
 * /rest/test/{testId}/methods namespace (research.md R10).
 */
@RestController
@RequestMapping("/rest/test-catalog")
@PreAuthorize("hasRole('ADMIN')")
public class TestCatalogEditorRestController {

    /**
     * v1 editor sections in SideNav order. Compliance (v2) is hidden entirely in
     * v1, so the v1 set is domain-independent (FR-007); the field is kept on the
     * envelope so the shell can branch once v2 lights up domain-conditional
     * visibility.
     */
    private static final List<String> V1_SECTIONS = List.of("basic-info", "sample-results", "methods", "ranges",
            "storage", "panels", "terminology", "analyzers", "display-order");

    @Autowired
    private TestService testService;

    public static class EditorEnvelope {
        public String testId;
        public String name;
        public String code;
        public String domain;
        public List<String> applicableSections;
    }

    @GetMapping(value = "/tests/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EditorEnvelope> getEditorEnvelope(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        EditorEnvelope envelope = new EditorEnvelope();
        envelope.testId = test.getId();
        // Test.getName() resolves the localized name, falling back to description.
        envelope.name = test.getName();
        envelope.code = test.getLocalCode();
        envelope.domain = test.getDomain();
        envelope.applicableSections = V1_SECTIONS;
        return ResponseEntity.ok(envelope);
    }
}
