package org.openelisglobal.reports.vectorsurveillance.manualentry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.vectorsurveillance.manualentry.service.ManualEntrySubmissionService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntrySubmissionAudit;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test (full Spring + Liquibase via Testcontainers) for the
 * immutable Manual Entry submission audit (FR-008 / US4-4): submitting a week
 * writes one immutable audit row, and re-submitting the SAME week creates a
 * SECOND distinct row (no silent overwrite of history).
 */
public class ManualEntrySubmissionServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ManualEntrySubmissionService submissionService;

    private static final LocalDate START = LocalDate.of(2026, 2, 2);
    private static final LocalDate END = LocalDate.of(2026, 2, 8);

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void submit_writesImmutableAuditRow_withSnapshotAndUser() {
        ManualEntrySubmissionAudit audit = submissionService.submit(START, END, null,
                Map.of("POOLS_TESTED", "12", "POOLS_POSITIVE", "3"), "1");

        assertNotNull("submit must return the persisted audit row", audit.getId());
        assertEquals(START, audit.getPeriodStart());
        assertEquals(END, audit.getPeriodEnd());
        assertEquals("1", audit.getSubmittedByUserId());
        assertNotNull("submittedAt must be captured", audit.getSubmittedAt());
        assertTrue("snapshot must capture the submitted figures", audit.getValueSnapshot().contains("POOLS_TESTED"));
    }

    @Test
    public void reSubmit_createsSecondDistinctRow_doesNotOverwriteHistory() {
        ManualEntrySubmissionAudit first = submissionService.submit(START, END, null, Map.of("POOLS_TESTED", "12"),
                "1");
        ManualEntrySubmissionAudit second = submissionService.submit(START, END, null, Map.of("POOLS_TESTED", "15"),
                "1");

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals("re-submission must be a distinct audit row", first.getId(), second.getId());

        List<ManualEntrySubmissionAudit> history = submissionService.getAudit(START, END);
        assertTrue("both submissions must be retrievable", history.size() >= 2);
        // The first row's snapshot is preserved unchanged (no overwrite).
        boolean firstPreserved = history.stream()
                .anyMatch(a -> a.getId().equals(first.getId()) && a.getValueSnapshot().contains("12"));
        assertTrue("the original snapshot must be preserved verbatim", firstPreserved);
    }

    @Test
    public void submit_rejectsInvertedPeriod() {
        boolean threw = false;
        try {
            submissionService.submit(END, START, null, Map.of("X", "1"), "1");
        } catch (RuntimeException e) {
            threw = true;
        }
        assertTrue("periodStart > periodEnd must be rejected", threw);
    }

    @Test
    public void submit_rejectsEmptySnapshot() {
        boolean threw = false;
        try {
            submissionService.submit(START, END, null, Map.of(), "1");
        } catch (RuntimeException e) {
            threw = true;
        }
        assertTrue("empty snapshot must be rejected", threw);
    }
}
