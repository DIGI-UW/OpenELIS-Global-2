package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the upsert / dedupe contract in
 * {@link AnalyzerResultsServiceImpl#insertAnalyzerResults}.
 * <p>
 * This is the single persistence method shared by every analyzer import path —
 * legacy plugins (GenericASTM, GenericHL7, GenericFile, per-device plugins) all
 * route through {@code AnalyzerLineInserter.persistImport()} →
 * {@code insertAnalyzerResults}, and the new FHIR bridge path
 * ({@code AnalyzerFhirImportController.importFhirBundle}) calls it directly.
 * The three-case contract this method implements (keyed on
 * {@code (analyzerId, accessionNumber, testName)}) is the authoritative upsert
 * semantics for the whole system, so it gets exercised here rather than being
 * duplicated per import path.
 * </p>
 * <p>
 * Three cases:
 * <ol>
 * <li><b>No previous row</b> — fresh insert.</li>
 * <li><b>Exact re-import</b> — same key AND same completeDate OR same result
 * value: skip silently (idempotent).</li>
 * <li><b>Corrected re-export</b> — same key but DIFFERENT completeDate AND
 * DIFFERENT result value: insert the new row as a linked correction
 * ({@code readOnly=true}, {@code duplicateAnalyzerResultId} backlink on both
 * rows).</li>
 * </ol>
 * This layout matches the plan mellow-honking-cascade Phase 1.6 upsert
 * invariants ({@code sampleAccession}, {@code testCode}, {@code analyzerId}) as
 * the dedupe key.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerResultsServiceImplTest {

    @Mock
    private AnalyzerResultsDAO baseObjectDAO;

    @InjectMocks
    private AnalyzerResultsServiceImpl service;

    private static final String USER_ID = "42";
    private static final String ANALYZER_ID = "208";
    private static final String ACCESSION = "DEV0126000000001";
    private static final String TEST_NAME = "VIH-1";

    private AnalyzerResults newIncoming(String result, Timestamp completeDate) {
        AnalyzerResults ar = new AnalyzerResults();
        ar.setAnalyzerId(ANALYZER_ID);
        ar.setAccessionNumber(ACCESSION);
        ar.setTestName(TEST_NAME);
        ar.setResult(result);
        ar.setCompleteDate(completeDate);
        return ar;
    }

    private AnalyzerResults existingRow(String id, String result, Timestamp completeDate) {
        AnalyzerResults ar = newIncoming(result, completeDate);
        ar.setId(id);
        ar.setLastupdated(new Timestamp(System.currentTimeMillis()));
        return ar;
    }

    @Before
    public void setUp() {
        // insert(T) on the base service delegates to baseObjectDAO.insert(T);
        // return a stable fake id for any invocation.
        when(baseObjectDAO.insert(any(AnalyzerResults.class))).thenReturn("new-id-777");
        when(baseObjectDAO.update(any(AnalyzerResults.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // These tests cover the upsert/dedupe contract; the inherited audit-emit
        // path needs a Spring-wired AuditTrailService + DAO.get(id) stubbing that
        // doesn't apply here. Disable it so save/update stays focused.
        ReflectionTestUtils.setField(service, "auditTrailLog", false);
    }

    // ------------------------------------------------------------------
    // Case 1 — no previous row: fresh insert
    // ------------------------------------------------------------------

    @Test
    public void noPreviousRow_insertsFreshResult() {
        AnalyzerResults incoming = newIncoming("1520.5", new Timestamp(1_700_000_000_000L));
        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(incoming)).thenReturn(null);

        service.insertAnalyzerResults(List.of(incoming), USER_ID);

        verify(baseObjectDAO, times(1)).insert(incoming);
        verify(baseObjectDAO, never()).update(any(AnalyzerResults.class));
        assertEquals("sysUserId must be stamped onto the inserted row", USER_ID, incoming.getSysUserId());
        assertFalse("fresh insert should not be flagged read-only", incoming.isReadOnly());
        assertNull("fresh insert must not back-link", incoming.getDuplicateAnalyzerResultId());
    }

    // ------------------------------------------------------------------
    // Case 2 — exact re-import: skip silently (idempotent)
    // ------------------------------------------------------------------

    @Test
    public void reImport_sameCompleteDate_skipsInsertAndUpdate() {
        Timestamp when = new Timestamp(1_700_000_000_000L);
        AnalyzerResults existing = existingRow("42", "1520.5", when);
        AnalyzerResults incoming = newIncoming("9999.9", when); // different value, SAME date
        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(incoming)).thenReturn(List.of(existing));

        service.insertAnalyzerResults(List.of(incoming), USER_ID);

        verify(baseObjectDAO, never()).insert(any(AnalyzerResults.class));
        verify(baseObjectDAO, never()).update(any(AnalyzerResults.class));
    }

    @Test
    public void reImport_sameResultValue_skipsInsertAndUpdate() {
        AnalyzerResults existing = existingRow("42", "1520.5", new Timestamp(1_700_000_000_000L));
        AnalyzerResults incoming = newIncoming("1520.5", new Timestamp(1_800_000_000_000L)); // same value, different
                                                                                             // date
        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(incoming)).thenReturn(List.of(existing));

        service.insertAnalyzerResults(List.of(incoming), USER_ID);

        verify(baseObjectDAO, never()).insert(any(AnalyzerResults.class));
        verify(baseObjectDAO, never()).update(any(AnalyzerResults.class));
    }

    // ------------------------------------------------------------------
    // Case 3 — corrected re-export: insert linked correction row
    // ------------------------------------------------------------------

    @Test
    public void correctedReExport_insertsNewLinkedRow_andBacklinksPrevious() {
        AnalyzerResults existing = existingRow("old-id-42", "1520.5", new Timestamp(1_700_000_000_000L));
        AnalyzerResults incoming = newIncoming("1750.0", new Timestamp(1_800_000_000_000L));
        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(incoming)).thenReturn(List.of(existing));

        service.insertAnalyzerResults(List.of(incoming), USER_ID);

        // New row is inserted…
        ArgumentCaptor<AnalyzerResults> insertCaptor = ArgumentCaptor.forClass(AnalyzerResults.class);
        verify(baseObjectDAO, times(1)).insert(insertCaptor.capture());
        AnalyzerResults inserted = insertCaptor.getValue();
        assertEquals("new row links back to the old row's id", "old-id-42", inserted.getDuplicateAnalyzerResultId());
        assertTrue("new corrected row is flagged read-only for staging review", inserted.isReadOnly());
        assertEquals(USER_ID, inserted.getSysUserId());

        // …and the previous row is updated to point forward at the new row
        ArgumentCaptor<AnalyzerResults> updateCaptor = ArgumentCaptor.forClass(AnalyzerResults.class);
        verify(baseObjectDAO, times(1)).update(updateCaptor.capture());
        AnalyzerResults updated = updateCaptor.getValue();
        assertEquals("previous row is back-linked to the new row's id", "new-id-777",
                updated.getDuplicateAnalyzerResultId());
        assertEquals(USER_ID, updated.getSysUserId());
    }

    // ------------------------------------------------------------------
    // Null-safety edge case (documents current behavior)
    // ------------------------------------------------------------------

    @Test
    public void reImport_bothCompleteDateAndResultNull_isTreatedAsCorrection() {
        // Pathological case: a previously-staged row with null completeDate AND
        // null result value. The loop never satisfies the "duplicate" predicate,
        // so the incoming row is inserted as a linked correction. This test
        // documents current behavior — if we want null+null to be a no-op skip
        // instead, the fix is a null-guarded early-return in the loop.
        AnalyzerResults existing = existingRow("old-id-99", null, null);
        AnalyzerResults incoming = newIncoming("new-value", new Timestamp(1_800_000_000_000L));
        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(incoming)).thenReturn(List.of(existing));

        service.insertAnalyzerResults(List.of(incoming), USER_ID);

        verify(baseObjectDAO, times(1)).insert(any(AnalyzerResults.class));
        verify(baseObjectDAO, times(1)).update(any(AnalyzerResults.class));
    }

    // ------------------------------------------------------------------
    // Multiple results in one batch
    // ------------------------------------------------------------------

    @Test
    public void mixedBatch_eachResultEvaluatedIndependently() {
        AnalyzerResults freshA = newIncoming("100", new Timestamp(1L));
        AnalyzerResults freshB = newIncoming("200", new Timestamp(2L));
        AnalyzerResults dupeC = newIncoming("300", new Timestamp(3L));

        AnalyzerResults existingForC = existingRow("existing-c", "300", new Timestamp(999L));

        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(freshA)).thenReturn(null);
        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(freshB)).thenReturn(null);
        when(baseObjectDAO.getDuplicateResultByAccessionAndTest(dupeC)).thenReturn(List.of(existingForC));

        List<AnalyzerResults> batch = new ArrayList<>();
        Collections.addAll(batch, freshA, freshB, dupeC);
        service.insertAnalyzerResults(batch, USER_ID);

        // Two fresh inserts, zero for the dupe
        verify(baseObjectDAO, times(2)).insert(any(AnalyzerResults.class));
        verify(baseObjectDAO, never()).update(any(AnalyzerResults.class));
    }

    @Test
    public void emptyBatch_isNoOp() {
        service.insertAnalyzerResults(Collections.emptyList(), USER_ID);
        verify(baseObjectDAO, never()).insert(any(AnalyzerResults.class));
        verify(baseObjectDAO, never()).update(any(AnalyzerResults.class));
    }

    /**
     * A4 — Import Issues panel backend: the service must pass-through to the DAO so
     * the REST endpoint reaches the {@code import_issue_reason IS NOT NULL} query
     * without an extra layer of filtering that could silently drop rows.
     */
    @Test
    public void findWithImportIssues_delegatesToDaoWithSameLimit() {
        AnalyzerResults orphan = new AnalyzerResults();
        orphan.setAccessionNumber("ACC-42");
        orphan.setTestName("CT");
        orphan.setImportIssueReason("unmapped_code:CT");
        when(baseObjectDAO.findWithImportIssues(25)).thenReturn(List.of(orphan));

        List<AnalyzerResults> rows = service.findWithImportIssues(25);
        assertEquals(1, rows.size());
        assertEquals("unmapped_code:CT", rows.get(0).getImportIssueReason());
        verify(baseObjectDAO, times(1)).findWithImportIssues(25);
    }
}
