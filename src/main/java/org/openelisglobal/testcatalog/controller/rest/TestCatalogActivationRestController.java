package org.openelisglobal.testcatalog.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testactivation.service.TestActivationAcknowledgmentService;
import org.openelisglobal.testactivation.valueholder.TestActivationAcknowledgment;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
 *
 * <p>
 * Named {@code TestCatalog...} (not just {@code TestActivation...}) to avoid a
 * Spring bean-name clash with the legacy
 * {@code testconfiguration.controller.rest.TestActivationRestController}, which
 * scans into the same context.
 */
@RestController
@RequestMapping("/rest/test-catalog")
@PreAuthorize("hasRole('ADMIN')")
public class TestCatalogActivationRestController {

    private final TestService testService;

    private final ResultLimitService resultLimitService;

    private final RangeCoverageValidationService coverageService;

    private final TestActivationAcknowledgmentService ackService;

    private final TestResultComponentService componentService;

    private final TestResultService testResultService;

    public TestCatalogActivationRestController(TestService testService, ResultLimitService resultLimitService,
            RangeCoverageValidationService coverageService, TestActivationAcknowledgmentService ackService,
            TestResultComponentService componentService, TestResultService testResultService) {
        this.testService = testService;
        this.resultLimitService = resultLimitService;
        this.coverageService = coverageService;
        this.ackService = ackService;
        this.componentService = componentService;
        this.testResultService = testResultService;
    }

    /** Acknowledgment payload: the coverage-gap report the user is accepting. */
    public static class ActivateRequest {
        public String gapsAcknowledged;
    }

    /**
     * FR-57 completeness report — the structured reason an activation was refused.
     * Returned with 422 so the UI can render a checklist instead of failing
     * silently (FR-58/FR-59). {@code missing} lists machine-readable issue codes;
     * {@code messages} the human-readable equivalents.
     */
    public static class CompletenessReport {
        public boolean complete;
        public List<String> missing = new ArrayList<>();
        public List<String> messages = new ArrayList<>();

        void add(String code, String message) {
            missing.add(code);
            messages.add(message);
        }
    }

    /**
     * FR-57 — a test may only go Active when it is safe to order and result: it
     * must have a name, at least one active PRIMARY component carrying a result
     * type, and every dictionary-backed active component must have at least one
     * result option. Returns a {@link CompletenessReport} listing every gap;
     * {@code complete} is true only when nothing is missing.
     */
    private CompletenessReport checkCompleteness(Test test) {
        CompletenessReport rep = new CompletenessReport();
        String name = test.getName();
        if (name == null || name.isBlank()) {
            rep.add("NO_NAME", "The test has no name.");
        }

        List<TestResultComponent> components = componentService.getActiveComponentsByTestId(test.getId());
        boolean hasTypedPrimary = components.stream()
                .anyMatch(c -> c.getIsPrimary() && c.getResultType() != null && !c.getResultType().isBlank());
        if (!hasTypedPrimary) {
            rep.add("NO_PRIMARY_RESULT_TYPE", "The test needs an active primary result component with a result type.");
        }

        List<TestResult> options = testResultService.getActiveTestResultsByTest(test.getId());
        for (TestResultComponent c : components) {
            String type = c.getResultType();
            if (type != null && TypeOfTestResultServiceImpl.ResultType.isDictionaryVarientById(type)) {
                boolean hasOption = options.stream().anyMatch(o -> c.getId().equals(o.getComponentId()));
                if (!hasOption) {
                    rep.add("NO_DICTIONARY_OPTIONS",
                            "Result component \"" + c.getLabel() + "\" has no result options defined.");
                }
            }
        }

        rep.complete = rep.missing.isEmpty();
        return rep;
    }

    /**
     * FR-58 — the completeness checklist for a test, so the editor can show what
     * still blocks activation before the user even tries. Same evaluation as the
     * activation gate; always 200 with the report.
     */
    @GetMapping(value = "/tests/{testId}/completeness", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompletenessReport> getCompleteness(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(checkCompleteness(test));
    }

    /**
     * Activates a test, gated on reference-range coverage. Uncovered age windows +
     * no acknowledgment → 409 with the coverage report; with an acknowledgment, an
     * audit row is written and the test is activated. No gaps → activates directly.
     * Returns the coverage report either way.
     */
    @PostMapping(value = "/tests/{testId}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> activateTest(@PathVariable String testId,
            @RequestBody(required = false) ActivateRequest body, HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        // FR-57 — refuse activation of an incomplete test up front, before the
        // range-coverage gate, so the user gets the structured checklist (FR-58)
        // rather than a downstream failure. 422 = the request was well-formed but
        // the test isn't in an activatable state.
        CompletenessReport completeness = checkCompleteness(test);
        if (!completeness.complete) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(completeness);
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
        // Per the FRS lifecycle (Active ⇒ orderable & importable), activating makes
        // the test orderable so it appears under its sample type on Add Order — that
        // picker filters to orderable tests, so activation alone wasn't enough
        // (OGC-1116).
        test.setOrderable(Boolean.TRUE);
        test.setSysUserId(sysUserId);
        testService.update(test);

        refreshTestCaches();
        return ResponseEntity.ok(report);
    }

    /**
     * Rebuild the cached order-picker lists so an activated test is orderable now.
     */
    private void refreshTestCaches() {
        testService.refreshTestNames();
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.ALL_TESTS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.ORDERABLE_TESTS);
        SpringContext.getBean(TypeOfSampleService.class).clearCache();
    }
}
