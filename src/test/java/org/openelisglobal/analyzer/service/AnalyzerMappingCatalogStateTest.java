package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;

public class AnalyzerMappingCatalogStateTest {

    private final AnalyzerMappingCatalogService catalog = mock(AnalyzerMappingCatalogService.class);

    @Test
    public void aMappedAnswerIsCurrentOnlyOnTheComponentItsRecordLandsOn() {
        when(catalog.searchActiveTests(null))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.TestOption("t1", "Viral load", null, List.of())));
        when(catalog.getActiveResultOptions("t1")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("on-call", "1", "Detected", List.of(), "c-call"),
                new AnalyzerMappingCatalogService.ResultOption("on-other", "2", "Detected", List.of(), "c-other")));
        AnalyzerMapping mapping = new AnalyzerMapping();
        mapping.setId("61");
        AnalyzerMappingTest main = new AnalyzerMappingTest();
        main.setId(new AnalyzerMappingTestPK("61", "HIVVL", ""));
        main.setMappingState(AnalyzerMappingState.BOUND);
        main.setTestId("t1");
        main.setCallComponentId("c-call");

        var validation = AnalyzerMappingCatalogState.load(catalog).validate(new AnalyzerMappingSnapshot(mapping,
                List.of(main), List.of(answer("DETECTED", "on-call"), answer("POSITIVE", "on-other"))));

        assertTrue(validation.isCurrentBoundResult("HIVVL", "DETECTED"));
        assertFalse("an answer of another component is not this record's", validation.isCurrentBoundResult("HIVVL",
                "POSITIVE"));
    }

    private static AnalyzerMappingResult answer(String rawValue, String optionId) {
        AnalyzerMappingResult row = new AnalyzerMappingResult();
        row.setId(new AnalyzerMappingResultPK("61", "HIVVL", "", rawValue));
        row.setMappingState(AnalyzerMappingState.BOUND);
        row.setTestResultId(optionId);
        return row;
    }
}
