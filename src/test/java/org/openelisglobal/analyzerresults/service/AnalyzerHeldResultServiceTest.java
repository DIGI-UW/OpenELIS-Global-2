package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private static AnalyzerResults row(String id, boolean held, String reason) {
        AnalyzerResults row = new AnalyzerResults();
        row.setId(id);
        row.setReadOnly(held);
        row.setImportIssueReason(reason);
        return row;
    }
}
