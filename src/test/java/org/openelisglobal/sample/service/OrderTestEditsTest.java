package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.Test;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;

public class OrderTestEditsTest {

    private static final String CANCELLED = "99";
    private static final Function<String, List<String>> PANELS = panelId -> "P1".equals(panelId) ? List.of("30", "31")
            : List.of();

    @Test
    public void theEnterOrderRowListsACollectedSpecimensTestsWithItsPanelsAndSkipsEmptyOnes() {
        Map<String, Set<String>> requested = OrderTestEdits.requestedTestsBySampleItem(List.of(),
                List.of(row("7", "10,11", "P1"), row(null, "12", ""), row("8", "", "")), PANELS);

        assertEquals(Map.of("7", Set.of("10", "11", "30", "31")), requested);
    }

    @Test
    public void onlyAnActiveOrderedAnalysisOfAListedSpecimenIsTakenOff() {
        Map<String, Set<String>> requested = Map.of("7", Set.of("10"));

        assertTrue(OrderTestEdits.isTakenOff(analysis("7", "11", "1"), requested, CANCELLED));
        assertFalse("still listed", OrderTestEdits.isTakenOff(analysis("7", "10", "1"), requested, CANCELLED));
        assertFalse("already cancelled",
                OrderTestEdits.isTakenOff(analysis("7", "11", CANCELLED), requested, CANCELLED));
        assertFalse("specimen not in the save",
                OrderTestEdits.isTakenOff(analysis("8", "11", "1"), requested, CANCELLED));

        Analysis reflex = analysis("7", "11", "1");
        reflex.setParentResult(new Result());
        assertFalse("reflex", OrderTestEdits.isTakenOff(reflex, requested, CANCELLED));
        Analysis workUp = analysis("7", "11", "1");
        workUp.setParentAnalysis(new Analysis());
        assertFalse("derived", OrderTestEdits.isTakenOff(workUp, requested, CANCELLED));
    }

    private static SampleTypeRequestDTO row(String sampleItemId, String tests, String panels) {
        SampleTypeRequestDTO row = new SampleTypeRequestDTO();
        row.setSampleItemId(sampleItemId);
        row.setRequestedTests(tests);
        row.setRequestedPanels(panels);
        return row;
    }

    private static org.openelisglobal.test.valueholder.Test test(String id) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        return test;
    }

    private static Analysis analysis(String sampleItemId, String testId, String statusId) {
        SampleItem item = new SampleItem();
        item.setId(sampleItemId);
        Analysis analysis = new Analysis();
        analysis.setSampleItem(item);
        analysis.setTest(test(testId));
        analysis.setStatusId(statusId);
        return analysis;
    }

    @Test
    public void aCollectedSampleListedWithNoTestsWhileItHasOrderedTestsIsCaught() {
        List<Analysis> onSeven = List.of(analysis("7", "10", "1"));

        assertTrue(OrderTestEdits.leavesACollectedSampleWithoutTests(List.of(), List.of(row("7", "", "")), onSeven,
                CANCELLED));
        assertFalse("tests listed", OrderTestEdits.leavesACollectedSampleWithoutTests(List.of(),
                List.of(row("7", "10", "")), onSeven, CANCELLED));
        assertFalse("only cancelled tests left", OrderTestEdits.leavesACollectedSampleWithoutTests(List.of(),
                List.of(row("7", "", "")), List.of(analysis("7", "10", CANCELLED)), CANCELLED));
        assertFalse("a sample not yet collected", OrderTestEdits.leavesACollectedSampleWithoutTests(List.of(),
                List.of(row(null, "", "")), onSeven, CANCELLED));
    }
}
