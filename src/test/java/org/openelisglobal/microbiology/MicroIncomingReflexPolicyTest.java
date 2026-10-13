package org.openelisglobal.microbiology;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.service.MicroIncomingReflexPolicy;
import org.openelisglobal.microbiology.valueholder.*;

public class MicroIncomingReflexPolicyTest {
    @Test
    public void incomingTargetSuppressesReflexOnlyForItsOwningCase() {
        var links = mock(MicroCaseAnalysisDAO.class);
        var queue = mock(MicroIncomingItemDAO.class);
        var policy = new MicroIncomingReflexPolicy(links, queue);
        var owner = new MicroCaseAnalysis();
        owner.setCaseId("case");
        when(links.getActiveByAnalysisId("trigger")).thenReturn(owner);
        var waiting = new MicroIncomingItem();
        waiting.setTestId("12");
        waiting.setState("WAITING");
        when(queue.forCase("case")).thenReturn(List.of(waiting));
        assertTrue(policy.alreadyOnCaseOrWaiting("trigger", "12"));
        assertFalse(policy.alreadyOnCaseOrWaiting("trigger", "13"));
        assertFalse(policy.alreadyOnCaseOrWaiting("other-case-trigger", "12"));
        waiting.setState("PLACED");
        assertFalse(policy.alreadyOnCaseOrWaiting("trigger", "12"));
        var analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        var test = new org.openelisglobal.test.valueholder.Test();
        test.setId("12");
        analysis.setTest(test);
        when(links.getAnalyses("case")).thenReturn(List.of(analysis));
        assertTrue(policy.alreadyOnCaseOrWaiting("trigger", "12"));
    }
}
