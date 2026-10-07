package org.openelisglobal.workplan.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.analysis.valueholder.Analysis;

/**
 * OGC-1423: a test referred to a reference laboratory stays out of every
 * worklist.
 */
public class WorklistAnalysesTest {

    private static Analysis analysis(String id, boolean referredOut) {
        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setReferredOut(referredOut);
        return analysis;
    }

    @Test
    public void withoutReferredOut_dropsReferredAnalysesAndKeepsTheOthersInOrder() {
        List<Analysis> result = WorklistAnalyses
                .withoutReferredOut(Arrays.asList(analysis("1", false), analysis("2", true), analysis("3", false)));

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("3", result.get(1).getId());
    }

    @Test
    public void withoutReferredOut_toleratesNull() {
        assertTrue(WorklistAnalyses.withoutReferredOut(null).isEmpty());
    }
}
