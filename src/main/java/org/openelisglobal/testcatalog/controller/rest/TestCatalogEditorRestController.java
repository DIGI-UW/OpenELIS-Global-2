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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private static final List<String> DOMAINS = List.of("CLINICAL", "ENVIRONMENTAL", "VECTOR");

    /** OGC-748 Basic Info — identity + domain + AMR flag + status. */
    public static class BasicInfo {
        public String testId;
        public String name;
        public String code;
        public String description;
        public String domain;
        public boolean antimicrobialResistance;
        public boolean active;
        public boolean orderable;
    }

    @GetMapping(value = "/tests/{testId}/basic-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BasicInfo> getBasicInfo(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toBasicInfo(test));
    }

    @PutMapping(value = "/tests/{testId}/basic-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BasicInfo> saveBasicInfo(@PathVariable String testId, @RequestBody BasicInfo body) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.domain != null && !DOMAINS.contains(body.domain)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (body.domain != null) {
            test.setDomain(body.domain);
        }
        test.setAntimicrobialResistance(body.antimicrobialResistance);
        test.setOrderable(body.orderable);
        test.setIsActive(body.active ? "Y" : "N");
        // Name/code/description and the coverage-gated activation modal land with
        // OGC-950 / OGC-953 (the latter wires to Ranges/M7); this slice persists
        // the v2.5-new fields (domain, AMR) + status safely.
        Test updated = testService.update(test);
        return ResponseEntity.ok(toBasicInfo(updated));
    }

    private BasicInfo toBasicInfo(Test test) {
        BasicInfo info = new BasicInfo();
        info.testId = test.getId();
        info.name = test.getName();
        info.code = test.getLocalCode();
        info.description = test.getDescription();
        info.domain = test.getDomain();
        info.antimicrobialResistance = Boolean.TRUE.equals(test.getAntimicrobialResistance());
        info.active = test.isActive();
        info.orderable = Boolean.TRUE.equals(test.getOrderable());
        return info;
    }
}
