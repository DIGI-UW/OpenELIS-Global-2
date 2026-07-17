package org.openelisglobal.analyzerresults.action;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

public class AnalyzerResultsPagingTest {

    @Test
    public void getResultsUsesAnalyzerSpecificSessionCache() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        TestResultItem genericResult = new TestResultItem();
        genericResult.setAccessionNumber("GEN-001");
        session.setAttribute(IActionConstants.RESULTS_SESSION_CACHE, List.of(List.of(genericResult)));

        AnalyzerResultItem analyzerResult = new AnalyzerResultItem();
        analyzerResult.setAccessionNumber("AN-001");
        session.setAttribute(AnalyzerResultsPaging.ANALYZER_RESULTS_SESSION_CACHE, List.of(List.of(analyzerResult)));

        List<AnalyzerResultItem> results = new AnalyzerResultsPaging().getResults(request);

        assertEquals("Analyzer paging should ignore generic results session cache", 1, results.size());
        assertEquals("AN-001", results.get(0).getAccessionNumber());
    }
}
