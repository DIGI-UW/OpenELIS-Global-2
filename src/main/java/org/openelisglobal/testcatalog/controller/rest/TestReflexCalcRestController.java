package org.openelisglobal.testcatalog.controller.rest;

import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.service.ReflexCalcViewService;
import org.openelisglobal.testcatalog.service.ReflexCalcViewService.ReflexCalcView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-764 — read-only Reflex &amp; Calc cross-links for a test. The
 * Test Catalog editor never edits reflex rules or calculations here; this only
 * surfaces what touches the test and deep-links to Master Lists. Gated by
 * ROLE_ADMIN, matching the other editor controllers.
 */
@RestController
@RequestMapping("/rest/test-catalog/{testId}/reflex-calc")
@PreAuthorize("hasRole('ADMIN')")
public class TestReflexCalcRestController {

    private final ReflexCalcViewService reflexCalcViewService;

    private final TestService testService;

    public TestReflexCalcRestController(ReflexCalcViewService reflexCalcViewService, TestService testService) {
        this.reflexCalcViewService = reflexCalcViewService;
        this.testService = testService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ReflexCalcView get(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found: " + testId);
        }
        return reflexCalcViewService.getForTest(testId);
    }
}
