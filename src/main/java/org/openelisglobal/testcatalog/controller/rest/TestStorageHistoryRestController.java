package org.openelisglobal.testcatalog.controller.rest;

import java.util.Collections;
import java.util.List;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingHistoryService;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingService;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandlingHistory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-766 (OGC-1005) — read the Sample Storage change history for a
 * test (newest first). Read-only; gated by ROLE_ADMIN like the other editor
 * endpoints. Returns an empty list when the test has no storage config yet.
 */
@RestController
@RequestMapping("/rest/test-catalog/{testId}/storage/history")
@PreAuthorize("hasRole('ADMIN')")
public class TestStorageHistoryRestController {

    private final TestSampleHandlingService handlingService;

    private final TestSampleHandlingHistoryService historyService;

    private final TestService testService;

    public TestStorageHistoryRestController(TestSampleHandlingService handlingService,
            TestSampleHandlingHistoryService historyService, TestService testService) {
        this.handlingService = handlingService;
        this.historyService = historyService;
        this.testService = testService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TestSampleHandlingHistory> getHistory(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found: " + testId);
        }
        TestSampleHandling handling = handlingService.getByTestId(testId);
        if (handling == null) {
            return Collections.emptyList();
        }
        return historyService.getByHandlingId(handling.getId());
    }
}
