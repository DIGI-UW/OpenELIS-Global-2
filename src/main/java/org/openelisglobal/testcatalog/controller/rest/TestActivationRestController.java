package org.openelisglobal.testcatalog.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testactivation.service.TestActivationAcknowledgmentService;
import org.openelisglobal.testactivation.valueholder.TestActivationAcknowledgment;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-949 M7 / OGC-973 — test activation gated on reference-range coverage (the
 * H-03 patient-safety gate). Separate from the section-CRUD editor controller
 * because activation is a distinct concern; shares the
 * {@code /rest/test-catalog} base + ROLE_ADMIN gate.
 */
@RestController
@RequestMapping("/rest/test-catalog")
@PreAuthorize("hasRole('ADMIN')")
public class TestActivationRestController {

    private final TestService testService;

    private final ResultLimitService resultLimitService;

    private final RangeCoverageValidationService coverageService;

    private final TestActivationAcknowledgmentService ackService;

    public TestActivationRestController(TestService testService, ResultLimitService resultLimitService,
            RangeCoverageValidationService coverageService, TestActivationAcknowledgmentService ackService) {
        this.testService = testService;
        this.resultLimitService = resultLimitService;
        this.coverageService = coverageService;
        this.ackService = ackService;
    }

    /** Acknowledgment payload: the coverage-gap report the user is accepting. */
    public static class ActivateRequest {
        public String gapsAcknowledged;
    }

    /**
     * Activates a test, gated on reference-range coverage. Uncovered age windows +
     * no acknowledgment → 409 with the coverage report; with an acknowledgment, an
     * audit row is written and the test is activated. No gaps → activates directly.
     * Returns the coverage report either way.
     */
    @PostMapping(value = "/tests/{testId}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RangeCoverageValidationService.CoverageReport> activateTest(@PathVariable String testId,
            @RequestBody(required = false) ActivateRequest body, HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        RangeCoverageValidationService.CoverageReport report = coverageService
                .validate(resultLimitService.getAllResultLimitsForTest(testId));

        boolean acknowledged = body != null && body.gapsAcknowledged != null && !body.gapsAcknowledged.isBlank();
        if (report.hasGaps() && !acknowledged) {
            // Uncovered age windows + no acknowledgment → block with the gap report.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(report);
        }
        if (report.hasGaps()) {
            TestActivationAcknowledgment ack = new TestActivationAcknowledgment();
            ack.setTestId(testId);
            ack.setUserId(sysUserId);
            ack.setGapsAcknowledged(body.gapsAcknowledged);
            ack.setSysUserId(sysUserId);
            ackService.insert(ack);
        }
        test.setIsActive("Y");
        test.setSysUserId(sysUserId);
        testService.update(test);
        return ResponseEntity.ok(report);
    }
}
