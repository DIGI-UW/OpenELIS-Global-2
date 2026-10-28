package org.openelisglobal.testalertrule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.service.ResultValidationService;
import org.openelisglobal.resultvalidation.util.ResultValidationSaveService;
import org.openelisglobal.testalertrule.service.TestAlertEvaluationService;
import org.openelisglobal.testalertrule.service.TestAlertRuleService;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.beans.factory.annotation.Autowired;

public class TestAlertEvaluationIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String RULE = "AlertEvalIT Rule";
    private static final String USER = "1";
    private static final String SUBJECT_KEY = "notification.testAlert.subject";
    private static final String DICTIONARY_ID = "990001";

    @Autowired
    private TestAlertEvaluationService alertEvaluationService;

    @Autowired
    private TestAlertRuleService alertRuleService;

    @Autowired
    private ResultValidationService resultValidationService;

    @Autowired
    private LogbookResultsPersistService logbookPersistService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private IStatusService statusService;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/result-validation-service.xml");
        cleanup();
        jdbcTemplate.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, lastupdated,"
                + " name, display_key, is_active) VALUES (7, 'Biologist Rejection', 7, 'ANALYSIS', NOW(),"
                + " 'Biologist Rejection', 'status.analysis.biologist.rejected', 'Y')");
        statusService.refreshCache();

        TestAlertRule rule = new TestAlertRule();
        rule.setTestId("1");
        rule.setName(RULE);
        rule.setTriggerType("SPECIFIC_VALUE");
        rule.setTriggerValue("90");
        rule.setEnabled(true);
        rule.setNotifyEmail(true);
        rule.setNotifyCustomEmail("officer@example.org");
        rule.setSysUserId(USER);
        alertRuleService.insert(rule);
    }

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_attempt WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE rule_name = ?)", RULE);
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_channel WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE rule_name = ?)", RULE);
        jdbcTemplate.update("DELETE FROM clinlims.notification_log WHERE rule_name = ?", RULE);
        jdbcTemplate.update("DELETE FROM clinlims.test_alert_rule WHERE name = ?", RULE);
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE id = ?", Integer.valueOf(DICTIONARY_ID));
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 5 WHERE status_id = 7");
        jdbcTemplate.update("DELETE FROM clinlims.status_of_sample WHERE id = 7");
        statusService.refreshCache();
    }

    @Test
    public void resultEntry_queuesNothingOutsideTheLab() {
        Result result = resultService.getResultById("1");
        result.setValue("90.0");

        alertEvaluationService.evaluateAndDispatch(result, USER);

        assertEquals(0, externalAlerts().size());
    }

    @Test
    public void finalValidation_queuesTheMessageOnceWithTheValidatedValueInTranslatedText() {
        validate("90.0", AnalysisStatus.Finalized);

        List<Map<String, Object>> alerts = externalAlerts();
        assertEquals(1, alerts.size());
        assertNotEquals("the subject key must exist", SUBJECT_KEY, MessageUtil.getMessage(SUBJECT_KEY, "TB"));
        assertEquals(MessageUtil.getMessage(SUBJECT_KEY, "TB"), alerts.get(0).get("rendered_subject"));
        assertTrue(String.valueOf(alerts.get(0).get("rendered_message")).contains("90.0"));
        assertEquals("officer@example.org", alerts.get(0).get("recipient_email"));
        assertEquals("12345", alerts.get(0).get("reference_accession"));
    }

    @Test
    public void selectListResult_queuesTheMessageWithTheEntryLabelNotItsId() {
        jdbcTemplate.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, lastupdated)"
                + " VALUES (?, 'Négatif', 'Y', NOW())", Integer.valueOf(DICTIONARY_ID));
        jdbcTemplate.update("UPDATE clinlims.test_alert_rule SET trigger_value = ? WHERE name = ?", DICTIONARY_ID,
                RULE);
        jdbcTemplate.update("UPDATE clinlims.result SET result_type = 'D' WHERE id = 1");

        validate(DICTIONARY_ID, AnalysisStatus.Finalized);

        List<Map<String, Object>> alerts = externalAlerts();
        assertEquals(1, alerts.size());
        String message = String.valueOf(alerts.get(0).get("rendered_message"));
        assertTrue(message, message.contains("Négatif"));
        assertFalse(message, message.contains(DICTIONARY_ID));
    }

    @Test
    public void resultCorrectedBeforeValidation_queuesNothing() {
        Result entered = resultService.getResultById("1");
        entered.setValue("90.0");
        alertEvaluationService.evaluateAndDispatch(entered, USER);

        validate("85.0", AnalysisStatus.Finalized);

        assertEquals(0, externalAlerts().size());
    }

    @Test
    public void rejectedResult_queuesNothing() {
        validate("90.0", AnalysisStatus.BiologistRejected);

        assertEquals(0, externalAlerts().size());
    }

    @Test
    public void returnedResult_queuesNothing() {
        validate("90.0", AnalysisStatus.NotStarted);

        assertEquals(0, externalAlerts().size());
    }

    @Test
    public void resultClearedAtEntry_queuesTheMessage() {
        Result result = resultService.getResultById("1");
        result.setValue("90.0");
        result.setSysUserId(USER);
        resultService.update(result);
        Analysis analysis = analysisService.get("1");
        analysis.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
        analysis.setSysUserId(USER);
        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet(USER);
        dataSet.getModifiedAnalysis().add(analysis);

        logbookPersistService.persistDataSet(dataSet, new ArrayList<>(), USER);

        assertEquals(1, externalAlerts().size());
    }

    private void validate(String value, AnalysisStatus status) {
        Analysis analysis = analysisService.get("1");
        analysis.setStatusId(statusService.getStatusID(status));
        List<Analysis> analyses = new ArrayList<>(List.of(analysis));
        Result result = resultService.getResultById("1");
        result.setValue(value);
        ArrayList<Result> results = new ArrayList<>(List.of(result));
        AnalysisItem item = new AnalysisItem();
        item.setAccessionNumber("12345");

        resultValidationService.persistdata(new ArrayList<>(), analyses, results, new ArrayList<>(List.of(item)),
                new ArrayList<>(), new ArrayList<>(), new ResultValidationSaveService(), new ArrayList<>(), USER);
    }

    private List<Map<String, Object>> externalAlerts() {
        return jdbcTemplate.queryForList("SELECT rendered_subject, rendered_message, recipient_email,"
                + " reference_accession FROM clinlims.notification_log WHERE product = 'ALERT' AND rule_name = ?",
                RULE);
    }
}
