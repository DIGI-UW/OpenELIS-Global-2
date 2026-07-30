package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-745 follow-up — server-side audit-trail invariant for unconditional
 * acceptance. The UI's {@code AcceptUnconditionallyGuard} requires a non-blank
 * justification before {@code Confirm} is enabled, but a scripted client could
 * still POST {@code forceTechApproval=true} with a blank
 * {@code forceTechApprovalNote} and slip a force-accept through without an
 * audit row. The guard added in {@link ResultUtil#createResultsFromItems}
 * rejects that case at the BAD_REQUEST level before any persistence runs.
 */
public class ResultUtilForceAcceptanceGuardTest {

    @Test
    public void forceAcceptWithBlankNote_throwsBadRequest_beforePersistence() {
        ResultsUpdateDataSet dataSet = mock(ResultsUpdateDataSet.class);
        TestResultItem item = new TestResultItem();
        item.setAnalysisId("42");
        item.setForceTechApproval("true"); // isForcedToAcceptance = true
        item.setForceTechApprovalNote(""); // blank — invariant violation
        when(dataSet.getModifiedItems()).thenReturn(Collections.singletonList(item));

        try {
            ResultUtil.createResultsFromItems(dataSet, false, false, false, "", mock(HttpServletRequest.class));
            fail("expected ResponseStatusException(BAD_REQUEST) for blank justification on force-accept");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            assertTrue("error message must mention forceTechApprovalNote so API consumers can locate the field",
                    e.getReason() != null && e.getReason().contains("forceTechApprovalNote"));
        }
    }

    @Test
    public void forceAcceptWithBlankNote_failsBatch_evenIfOtherItemsAreClean() {
        // Lock the "fail fast for the whole batch" behavior — one bad item must
        // reject all, so a partial persistence can't leak the bypass.
        ResultsUpdateDataSet dataSet = mock(ResultsUpdateDataSet.class);
        TestResultItem clean = new TestResultItem();
        clean.setAnalysisId("1");
        clean.setForceTechApproval(""); // not a force-accept
        TestResultItem bypass = new TestResultItem();
        bypass.setAnalysisId("2");
        bypass.setForceTechApproval("true");
        bypass.setForceTechApprovalNote(null);
        when(dataSet.getModifiedItems()).thenReturn(Arrays.asList(clean, bypass));

        try {
            ResultUtil.createResultsFromItems(dataSet, false, false, false, "", mock(HttpServletRequest.class));
            fail("expected ResponseStatusException(BAD_REQUEST) when ANY item violates the invariant");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }
}
