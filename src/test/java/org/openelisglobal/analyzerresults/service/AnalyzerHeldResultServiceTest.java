package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerHeldResultServiceTest {

    @Mock
    private AnalyzerResultsService analyzerResultsService;

    private AnalyzerHeldResultService service;

    @Before
    public void setUp() {
        service = new AnalyzerHeldResultServiceImpl(analyzerResultsService);
    }

    @Test
    public void aHeldResultIsDismissedAsTheReviewerWhoeverItWasHeldFor() {
        AnalyzerResults held = row("9001", true, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY);
        when(analyzerResultsService.get("9001")).thenReturn(held);

        service.dismiss("9001", "17");

        verify(analyzerResultsService).delete("9001", "17");
    }

    @Test
    public void aResultHeldForPlacementIsDismissedToo() {
        when(analyzerResultsService.get("9003"))
                .thenReturn(row("9003", false, AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT));

        service.dismiss("9003", "17");

        verify(analyzerResultsService).delete("9003", "17");
    }

    @Test
    public void aResultWaitingForAcceptanceIsNotDismissedFromHere() {
        when(analyzerResultsService.get("9002")).thenReturn(row("9002", false, null));

        assertThrows(IllegalStateException.class, () -> service.dismiss("9002", "17"));

        verify(analyzerResultsService, never()).delete(any(String.class), any(String.class));
    }

    /**
     * OGC-1662: removing a test from review removed its main row only; its
     * component rows stayed held and the test came straight back.
     */
    @Test
    public void aHeldTestLeavesReviewWithItsComponentRowsOnTheSameTube() {
        AnalyzerResults main = rowOfTest("9101", null, AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT);
        AnalyzerResults part = rowOfTest("9102", null, AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT);
        AnalyzerResults otherTube = rowOfTest("9103", "ACC-9-2", AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT);
        AnalyzerResults waiting = rowOfTest("9104", null, null);
        when(analyzerResultsService.get("9101")).thenReturn(main);
        when(analyzerResultsService.getAllMatching(anyMap())).thenReturn(List.of(main, part, otherTube, waiting));

        service.dismiss("9101", "17");

        verify(analyzerResultsService).delete("9101", "17");
        verify(analyzerResultsService).delete("9102", "17");
        verify(analyzerResultsService, never()).delete("9103", "17");
        verify(analyzerResultsService, never()).delete("9104", "17");
    }

    private static AnalyzerResults rowOfTest(String id, String tube, String reason) {
        AnalyzerResults row = row(id, false, reason);
        row.setAnalyzerId("11");
        row.setAccessionNumber("ACC-9");
        row.setTestId("380");
        row.setInstrumentSpecimenId(tube);
        return row;
    }

    private static AnalyzerResults row(String id, boolean held, String reason) {
        AnalyzerResults row = new AnalyzerResults();
        row.setId(id);
        row.setReadOnly(held);
        row.setImportIssueReason(reason);
        return row;
    }
}
