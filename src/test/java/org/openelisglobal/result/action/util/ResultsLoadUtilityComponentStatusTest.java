package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Results Entry shows each part of a multi-part test with its own status while
 * the test awaits validation, since the parts are entered and validated one by
 * one. Fixture: {@code testdata/component-units.xml} — analysis 100 in
 * technical acceptance, a viral load (result 100) and its Ct (result 101).
 */
@Transactional
public class ResultsLoadUtilityComponentStatusTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultsLoadUtility resultsLoadUtility;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/component-units.xml");
        jdbcTemplate.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, name,"
                + " is_active, lastupdated) VALUES (9406, 'Finalized', '6', 'ANALYSIS', 'Finalized', 'Y', NOW()),"
                + " (9408, 'Not Tested', '1', 'ANALYSIS', 'Not Tested', 'Y', NOW())");
        statusService.refreshCache();
    }

    @Test
    public void eachPartShowsItsOwnStatus() {
        jdbcTemplate.update("UPDATE clinlims.result SET validated_date = NOW() WHERE id = 100");
        jdbcTemplate.update("UPDATE clinlims.result SET value = '' WHERE id = 101");

        Map<String, String> statusByPart = statusByPart();

        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), statusByPart.get("vcu-load"));
        assertEquals(statusService.getStatusID(AnalysisStatus.NotStarted), statusByPart.get("vcu-ct"));
    }

    @Test
    public void anEnteredPartAwaitsValidation() {
        Map<String, String> statusByPart = statusByPart();

        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance), statusByPart.get("vcu-load"));
        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance), statusByPart.get("vcu-ct"));
    }

    /** A released test is released in every part, whatever each part's mark. */
    @Test
    public void aReleasedTestIsFinalizedInEveryPart() {
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 9406 WHERE id = 100");
        jdbcTemplate.update("UPDATE clinlims.result SET value = '' WHERE id = 101");

        Map<String, String> statusByPart = statusByPart();

        assertEquals("9406", statusByPart.get("vcu-load"));
        assertEquals("9406", statusByPart.get("vcu-ct"));
    }

    private Map<String, String> statusByPart() {
        List<TestResultItem> items = ReflectionTestUtils.invokeMethod(resultsLoadUtility,
                "getTestResultItemFromAnalysis", analysisService.get("100"), " ", " ", "");
        return items.stream().collect(
                Collectors.toMap(TestResultItem::getTestResultComponentId, TestResultItem::getAnalysisStatusId));
    }
}
