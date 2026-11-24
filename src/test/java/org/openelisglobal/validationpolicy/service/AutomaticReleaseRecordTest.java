package org.openelisglobal.validationpolicy.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.AnalysisTimelineService;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.util.ResultsValidationUtility;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * OGC-1017: a result that result entry releases without a validator is recorded
 * when it happens, with who saved it and the rule that released it, and the
 * auto-validated list and the result's history read that record.
 */
public class AutomaticReleaseRecordTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ValidationPolicyService validationPolicyService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private ResultsValidationUtility resultsValidationUtility;
    @Autowired
    private AnalysisTimelineService timelineService;
    @Autowired
    private LogbookResultsPersistService persistService;
    @Autowired
    private AnalysisAutoReleaseService autoReleaseService;

    private MockHttpSession session;
    private String flagBefore;
    private String validateAllBefore;
    private String blockSelfValidationBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        String[][] statuses = { { "9101", "Not Tested", "4", "ANALYSIS" }, { "9102", "Test Canceled", "5", "ANALYSIS" },
                { "9103", "Technical Acceptance", "7", "ANALYSIS" }, { "9104", "Technical Rejected", "8", "ANALYSIS" },
                { "9105", "Biologist Rejection", "9", "ANALYSIS" }, { "9106", "Sample Rejected", "10", "ANALYSIS" },
                { "9107", "NonConforming", "11", "ANALYSIS" }, { "9108", "Test Entered", "12", "ORDER" },
                { "9109", "Testing Started", "13", "ORDER" }, { "9110", "Testing finished", "14", "ORDER" } };
        for (String[] status : statuses) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.status_of_sample (id, name, code, status_type, is_active, display_key,"
                            + " description, lastupdated) VALUES (CAST(? AS numeric), ?, CAST(? AS numeric), ?, 'Y',"
                            + " ?, ?, NOW()) ON CONFLICT DO NOTHING",
                    status[0], status[1], status[2], status[3], "status." + status[0], status[1]);
        }
        jdbcTemplate.update("UPDATE clinlims.panel SET lastupdated = NOW() WHERE lastupdated IS NULL");
        jdbcTemplate.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (1, NOW())"
                + " ON CONFLICT (system_user_id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit) VALUES (-9311,"
                + " 'AllLabUnits') ON CONFLICT (lab_unit_role_map_id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (1,"
                + " -9311) ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role) VALUES (-9311, '5')"
                + " ON CONFLICT DO NOTHING");
        statusService.refreshCache();
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 9101 WHERE id = 1");

        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        flagBefore = configuration.getPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION);
        validateAllBefore = configuration.getPropertyValue(Property.ALWAYS_VALIDATE_RESULTS);
        blockSelfValidationBefore = configuration.getPropertyValue(Property.BLOCK_SELF_VALIDATION);
        configuration.setPropertyValue(Property.BLOCK_SELF_VALIDATION, "false");
        session = adminSession();
    }

    @After
    public void restoreConfiguration() {
        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, flagBefore);
        configuration.setPropertyValue(Property.ALWAYS_VALIDATE_RESULTS, validateAllBefore);
        configuration.setPropertyValue(Property.BLOCK_SELF_VALIDATION, blockSelfValidationBefore);
    }

    @Test
    public void aResultTheNoReviewPolicyReleasesIsRecordedWithThatPolicy() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "true");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("NONE", 0, null), "1");

        save("1", "3", "1", "90.0");

        assertEquals(finalized(), analysisService.get("1").getStatusId());
        Map<String, Object> record = onlyRecordOf("1");
        assertEquals("NO_REVIEW", record.get("release_rule"));
        assertEquals("LAB_WIDE", record.get("policy_scope"));
        assertEquals("NONE", record.get("review_trigger"));
        assertEquals(0, ((Number) record.get("review_levels")).intValue());
        assertEquals(1, ((Number) record.get("released_by")).intValue());
        assertEquals(jdbcTemplate
                .queryForObject("SELECT id FROM clinlims.validation_policy WHERE test_section_id IS NULL", Long.class),
                Long.valueOf(((Number) record.get("policy_id")).longValue()));
        assertEquals("the version of the policy that released it",
                jdbcTemplate.queryForObject(
                        "SELECT last_updated FROM clinlims.validation_policy WHERE test_section_id IS NULL",
                        java.sql.Timestamp.class),
                record.get("policy_version"));

        AnalysisItem row = autoValidatedRow("1");
        assertEquals("Lab-wide policy: No review", row.getAutoReleaseRule());
        assertTrue(row.getAutoReleasedBy(), !row.getAutoReleasedBy().isBlank());
        assertTrue(timelineService.getTimeline(analysisService.get("1")).stream()
                .anyMatch(event -> "AUTO_RELEASE".equals(event.getType())
                        && "Lab-wide policy: No review".equals(event.getDetail())));
    }

    @Test
    public void aResultThatWaitsForReviewIsNotRecorded() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "true");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 1, null), "1");

        save("1", "3", "1", "90.0");

        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get("1").getStatusId());
        assertEquals(0, recordsOf("1").size());
    }

    @Test
    public void aLabUnitOverrideThatReleasesIsRecordedWithTheOverride() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "true");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 1, null), "1");
        var override = validationPolicyService.addLabUnitOverride(
                new org.openelisglobal.validationpolicy.form.LabUnitPolicyForm("1", "NONE", 0, null), "1");

        save("1", "3", "1", "90.0");

        Map<String, Object> record = onlyRecordOf("1");
        assertEquals("NO_REVIEW", record.get("release_rule"));
        assertEquals("LAB_UNIT", record.get("policy_scope"));
        assertEquals(1, ((Number) record.get("test_section_id")).intValue());
        assertEquals(override.id(), Long.valueOf(((Number) record.get("policy_id")).longValue()));
        assertEquals(override.labUnitName() + " policy: No review", autoValidatedRow("1").getAutoReleaseRule());
    }

    @Test
    public void withThePolicyOffTheSettingIsWhatReleasedIt() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "false");
        ConfigurationProperties.getInstance().setPropertyValue(Property.ALWAYS_VALIDATE_RESULTS, "false");
        jdbcTemplate.update(
                "UPDATE clinlims.analysis SET status_id = CAST(? AS numeric), released_date = NULL" + " WHERE id = 1",
                finalized());

        autoReleaseService.record(List.of(analysisService.get("1")), "1");

        Map<String, Object> record = onlyRecordOf("1");
        assertEquals("CLEAR", record.get("release_rule"));
        assertEquals("SETTING", record.get("policy_scope"));
        assertNull(record.get("review_trigger"));
        assertNull(record.get("policy_id"));
        assertEquals("\"Validate all results\" is off; the result was clear",
                autoValidatedRow("1").getAutoReleaseRule());
    }

    @Test
    public void aValidatorReleaseAfterTheRecordTakesTheResultOffTheAutoValidatedList() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "true");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("NONE", 0, null), "1");
        save("1", "3", "1", "90.0");
        assertEquals("Lab-wide policy: No review", autoValidatedRow("1").getAutoReleaseRule());

        jdbcTemplate.update("UPDATE clinlims.analysis SET released_date = now() + interval '1 minute' WHERE id = 1");

        assertTrue(autoValidatedRows().stream().noneMatch(row -> "1".equals(row.getAnalysisId())));
    }

    @Test
    public void aFinalizedStatusSetOutsideResultEntryIsNotRecorded() {
        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet("1");
        Analysis analysis = ResultUtil.resolveModifiedAnalysis(dataSet, "1");
        analysis.setStatusId(finalized());
        analysis.setSysUserId("1");

        persistService.persistDataSet(dataSet, List.of(), "1");

        assertEquals(finalized(), analysisService.get("1").getStatusId());
        assertEquals("a sign-out that sets Finalized itself is not an automatic release", 0, recordsOf("1").size());
    }

    private void save(String analysisId, String resultId, String testId, String value) throws Exception {
        String token = String.valueOf(analysisService.get(analysisId).getLastupdated().getTime());
        String testDate = org.openelisglobal.common.util.DateUtil.formatDateAsText(new java.util.Date()) + " 09:15";
        String body = "{\"testResult\":{\"analysisId\":\"" + analysisId + "\",\"accessionNumber\":\"12345\","
                + "\"resultId\":\"" + resultId + "\",\"testId\":\"" + testId + "\",\"resultType\":\"N\","
                + "\"resultValue\":\"" + value + "\",\"testDate\":\"" + testDate + "\",\"isModified\":true,"
                + "\"valid\":true,\"reportable\":true,\"analysisLastupdated\":\"" + token + "\",\"note\":\"\"}}";
        mockMvc.perform(post("/rest/results-entry/analysis/" + analysisId + "/result")
                .contentType(MediaType.APPLICATION_JSON).content(body).session(session)).andExpect(status().isOk());
    }

    private String finalized() {
        return statusService.getStatusID(AnalysisStatus.Finalized);
    }

    private List<Map<String, Object>> recordsOf(String analysisId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM clinlims.analysis_auto_release WHERE analysis_id = CAST(? AS" + " numeric) ORDER BY id",
                analysisId);
    }

    private Map<String, Object> onlyRecordOf(String analysisId) {
        List<Map<String, Object>> records = recordsOf(analysisId);
        assertEquals(records.toString(), 1, records.size());
        return records.get(0);
    }

    private List<AnalysisItem> autoValidatedRows() {
        return resultsValidationUtility.getAutoValidatedAnalysisBySample(sampleService.get("1"));
    }

    private AnalysisItem autoValidatedRow(String analysisId) {
        return autoValidatedRows().stream().filter(row -> analysisId.equals(row.getAnalysisId())).findFirst()
                .orElseThrow(() -> new AssertionError("analysis " + analysisId + " is not on the auto-validated list"));
    }

    private MockHttpSession adminSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
                .build();
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return httpSession;
    }
}
