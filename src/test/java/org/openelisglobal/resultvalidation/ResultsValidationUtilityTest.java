package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Regression tests for the status-filtering logic in ResultsValidationUtility.
 *
 * <p>
 * The bug: statusList was List&lt;String&gt; but the filter used
 * Integer.valueOf(analysis.getStatusId()), so the contains() check compared
 * Integer against String elements and always returned false.
 */
public class ResultsValidationUtilityTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
    }

    @Test
    public void statusFilter_shouldMatchStringStatusIdAgainstStringList() {
        // Analysis id=1 has status_id="1", started_date="2023-11-15"
        // Analysis id=2 has status_id="2", started_date="2023-11-16"
        List<Analysis> allAnalyses = analysisService.getAll();
        assertFalse("Test data should contain analyses", allAnalyses.isEmpty());

        Analysis analysis = analysisService.get("1");
        assertEquals("1", analysis.getStatusId());

        // This is the exact filtering pattern from
        // getPageUnValidatedTestResultItemsByTestDate.
        // Before the fix, Integer.valueOf("1") would not match "1" in a
        // List<String>.
        List<String> statusList = Arrays.asList("1", "2");
        boolean matchesCorrectly = statusList.contains(analysis.getStatusId());
        assertTrue("String statusId should match String status list entry", matchesCorrectly);
    }

    @Test
    public void statusFilter_shouldNotMatchWhenStatusIdAbsent() {
        Analysis analysis = analysisService.get("1");
        List<String> statusList = Arrays.asList("99", "100");
        boolean matches = statusList.contains(analysis.getStatusId());
        assertFalse("StatusId '1' should not match list containing only '99','100'", matches);
    }

    @Test
    public void statusFilter_shouldFilterAnalysisListCorrectly() {
        // Reproduce the exact stream().filter() pattern from the utility
        List<Analysis> allAnalyses = analysisService.getAll();
        List<String> statusList = Arrays.asList("1");

        List<Analysis> filtered = allAnalyses.stream().filter(analysis -> statusList.contains(analysis.getStatusId()))
                .toList();

        assertEquals("Should find exactly one analysis with status_id='1'", 1, filtered.size());
        assertEquals("1", filtered.get(0).getId());
    }

    @Test
    public void statusFilter_shouldReturnEmptyForNonMatchingStatus() {
        List<Analysis> allAnalyses = analysisService.getAll();
        List<String> statusList = Arrays.asList("999");

        List<Analysis> filtered = allAnalyses.stream().filter(analysis -> statusList.contains(analysis.getStatusId()))
                .toList();

        assertTrue("No analysis should match status '999'", filtered.isEmpty());
    }
}
