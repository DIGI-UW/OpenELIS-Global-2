package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1608: a results entry save that would release a test while one of its
 * required components has no result leaves the test awaiting validation.
 * Fixture: {@code testdata/component-units.xml}, analysis 100 with a viral load
 * (component vcu-load, result 100) and its Ct (component vcu-ct, result 101).
 */
@Transactional
public class RequiredComponentHoldTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/component-units.xml");
        jdbcTemplate.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, name,"
                + " is_active, lastupdated) VALUES (9406, 'Finalized', '6', 'ANALYSIS', 'Finalized', 'Y', NOW())");
        statusService.refreshCache();
    }

    @Test
    public void aRequiredComponentWithoutAResultHoldsTheTestOpen() {
        clearCt();
        require("vcu-ct", true);

        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                statusAfterSaving(item("vcu-load", "1010")));
    }

    @Test
    public void anOptionalComponentWithoutAResultDoesNotHoldTheTest() {
        clearCt();
        require("vcu-ct", false);

        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), statusAfterSaving(item("vcu-load", "1010")));
    }

    @Test
    public void aValueEnteredInTheSameSaveCountsForTheRequiredComponent() {
        clearCt();
        require("vcu-ct", true);

        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized),
                statusAfterSaving(item("vcu-load", "1010"), item("vcu-ct", "33.0")));
    }

    @Test
    public void clearingARequiredComponentInTheSaveHoldsTheTestOpen() {
        require("vcu-ct", true);

        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                statusAfterSaving(item("vcu-ct", "")));
    }

    private String statusAfterSaving(TestResultItem... items) {
        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet("1");
        for (TestResultItem item : items) {
            dataSet.getModifiedItems().add(item);
        }
        Analysis analysis = analysisService.get("100");
        analysis.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
        dataSet.getModifiedAnalysis().add(analysis);

        ResultUtil.holdOpenWhileRequiredComponentsBlank(dataSet);

        return analysis.getStatusId();
    }

    private static TestResultItem item(String componentId, String value) {
        TestResultItem item = new TestResultItem();
        item.setAnalysisId("100");
        item.setTestResultComponentId(componentId);
        item.setResultType("N");
        item.setResultValue(value);
        item.setShadowResultValue(value);
        return item;
    }

    private void clearCt() {
        jdbcTemplate.update("UPDATE clinlims.result SET value = '' WHERE id = 101");
    }

    private void require(String componentId, boolean required) {
        jdbcTemplate.update("UPDATE clinlims.test_result_component SET is_required = ? WHERE id = ?", required,
                componentId);
    }
}
