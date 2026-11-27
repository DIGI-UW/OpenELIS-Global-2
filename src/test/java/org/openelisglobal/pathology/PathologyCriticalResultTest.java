package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.common.services.historyservices.ProgramSampleHistoryService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.event.CriticalResultEvent;
import org.openelisglobal.program.service.CriticalResultAlertListener;
import org.openelisglobal.program.service.PathologyConclusionSearchService;
import org.openelisglobal.program.service.PathologyCriticalResultService;
import org.openelisglobal.program.service.PathologyCriticalResultService.CriticalResult;
import org.openelisglobal.program.service.PathologyReportService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.util.DiagnosisCodeStarts;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntry;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * What makes a pathology case critical: a coded diagnosis whose code starts
 * with one of the laboratory's starts, or the pathologist's own flag; and where
 * the flag, the alert and the setting live. The setting is set on the shared
 * configuration and put back after each test.
 */
public class PathologyCriticalResultTest extends PathologyCaseTestSupport {

    private static final int FIRST_OWN_ROW = 9601;
    private static final int NO_CODE_ENTRY = 9105;
    private static final String SECOND_PATHOLOGIST_ID = "1003";

    @Autowired
    private PathologyCriticalResultService criticalResultService;

    @Autowired
    private PathologyConclusionSearchService searchService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private PathologyFindingsDictionary dictionary;
    private StageSwitches switches;
    private String codesSetting;

    @Before
    public void seedTheDictionary() {
        switches = StageSwitches.allOn();
        dictionary = PathologyFindingsDictionary.seed(jdbcTemplate);
        int category = DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.CONCLUSION_CATEGORY);
        DictionaryRows.insertEntry(jdbcTemplate, NO_CODE_ENTRY, category, "No code yet", null, true);
        codesSetting = ConfigurationProperties.getInstance().getPropertyValue(Property.PATHOLOGY_CRITICAL_RESULT_CODES);
        codes("");
    }

    @After
    public void putEverythingBack() {
        codes(codesSetting == null ? "" : codesSetting);
        jdbcTemplate.update("DELETE FROM clinlims.alert WHERE alert_entity_type = 'PATHOLOGY_SAMPLE'");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE id >= ? AND id < ?", FIRST_OWN_ROW,
                FIRST_OWN_ROW + 10);
        DictionaryRows.removeEntries(jdbcTemplate, NO_CODE_ENTRY);
        dictionary.close();
        switches.close();
    }

    @Test
    public void aCase_isCritical_byACodeOrByTheFlag() {
        // K2: C80.1 starts with C, K29.5 does not
        codes("C");
        addConclusions(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.CHRONIC_GASTRITIS);
        CriticalResult byCode = criticalResult();
        assertTrue("a matching code makes the case critical", byCode.critical());
        assertFalse("the pathologist did not flag it", byCode.flagged());
        assertEquals("only the matching entry is named", List.of(PathologyFindingsDictionary.ADENOCARCINOMA),
                byCode.conclusionDictionaryIds());
        assertTrue(DiagnosisCodeStarts.isCritical("c80.1"));
        assertFalse(DiagnosisCodeStarts.isCritical("K29.5"));

        // K4: an ICD-O code and an entry with no code are not caught by a letter start
        removeConclusions();
        addConclusions(PathologyFindingsDictionary.CARCINOME, String.valueOf(NO_CODE_ENTRY));
        CriticalResult uncoded = criticalResult();
        assertFalse(uncoded.critical());
        assertEquals(List.of(), uncoded.conclusionDictionaryIds());

        // K1: the setting is read on each call, so emptying it ends the match
        removeConclusions();
        addConclusions(PathologyFindingsDictionary.ADENOCARCINOMA);
        codes("");
        assertFalse("an empty setting marks nothing", criticalResult().critical());

        // K5: the flag alone makes the case critical, with no code named
        setStoredFlag(true);
        CriticalResult flagged = criticalResult();
        assertTrue(flagged.critical());
        assertTrue(flagged.flagged());
        assertEquals(List.of(), flagged.conclusionDictionaryIds());
    }

    @Test
    public void theSearch_marksCriticalEntries() {
        codes("C");

        List<ConclusionEntry> entries = searchService.search("carcin").items();

        Map<String, ConclusionEntry> byId = entries.stream()
                .collect(Collectors.toMap(ConclusionEntry::id, entry -> entry));
        assertTrue("the entry coded C80.1 is critical",
                byId.get(PathologyFindingsDictionary.ADENOCARCINOMA).critical());
        assertFalse("the entry coded 8500/3 is not", byId.get(PathologyFindingsDictionary.CARCINOME).critical());
        for (ConclusionEntry entry : entries) {
            assertEquals("every entry says what its code says: " + entry.code(),
                    entry.code().toUpperCase().startsWith("C"), entry.critical());
        }

        codes("");
        assertTrue("with no setting nothing is marked",
                searchService.search("carcin").items().stream().noneMatch(ConclusionEntry::critical));
    }

    @Test
    public void theFlag_survivesAnAssignment_aMove_andASaveWithoutIt() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        setStoredFlag(true);
        assertTrue("setup: the flag is stored", storedFlag(CASE_ID));

        PathologySampleForm assign = caseViewForm();
        assign.setAssignedPathologistId(SECOND_PATHOLOGIST_ID);
        save(assign);
        assertEquals("the assignment was written", SECOND_PATHOLOGIST_ID, jdbcTemplate.queryForObject(
                "SELECT pathologist_id FROM clinlims.pathology_sample WHERE id = ?", String.class, CASE_ID));
        assertTrue("the flag survives an assignment", storedFlag(CASE_ID));

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.UNDER_REVIEW, "Needs deeper levels",
                TECHNICIAN_ID);
        assertEquals("READY_PATHOLOGIST", storedStage());
        assertTrue("the flag survives a return to the queue", storedFlag(CASE_ID));

        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, TECHNICIAN_ID);
        assertEquals("UNDER_REVIEW", storedStage());
        assertTrue("the flag survives beginning again", storedFlag(CASE_ID));

        save(caseViewForm());
        assertTrue("a save that does not carry the flag leaves it", storedFlag(CASE_ID));
    }

    @Test
    public void theListener_postsTheAlert_whenTheEventIsPublishedAndCommitted() {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> eventPublisher.publishEvent(new CriticalResultEvent(this, "PATHOLOGY",
                        CASE_ID, accessionNumber, TECHNICIAN_ID, OffsetDateTime.now())));

        List<Map<String, Object>> alerts = jdbcTemplate.queryForList(
                "SELECT alert_type, severity, status, message, context_data::text AS context FROM clinlims.alert"
                        + " WHERE alert_entity_type = 'PATHOLOGY_SAMPLE' AND alert_entity_id = ?",
                CASE_ID);
        assertEquals("one alert on the case", 1, alerts.size());
        Map<String, Object> alert = alerts.get(0);
        assertEquals("CRITICAL_RESULT", alert.get("alert_type"));
        assertEquals("CRITICAL", alert.get("severity"));
        assertEquals("OPEN", alert.get("status"));
        assertTrue("the message names the lab number", String.valueOf(alert.get("message")).contains(accessionNumber));
        assertTrue("the context names the lab number and the case",
                String.valueOf(alert.get("context")).contains(accessionNumber)
                        && String.valueOf(alert.get("context")).contains("\"pathologySampleId\""));
    }

    @Test
    public void theFlag_isNamedOnTheCasesHistoryRow() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        save(flaggedCritical(true));

        List<AuditTrailItem> items = new TransactionTemplate(transactionManager).execute(
                status -> new ProgramSampleHistoryService(pathologySampleService.get(CASE_ID)).getAuditTrailItems());

        List<String> lines = items.stream().filter(item -> "criticalResultFlagged".equals(item.getIdentifier()))
                .map(item -> item.getOldValue() + ">" + item.getNewValue()).collect(Collectors.toList());
        assertEquals("the flag reads as one line, false to true", List.of("false>true"), lines);
    }

    // K7
    @Test
    public void signingOutACriticalCase_emitsOnce_andRaisesAnAlert_withNoDiagnosis() {
        codes("C");

        signOut(PathologyFindingsDictionary.ADENOCARCINOMA);

        assertEquals("the sign-out says it raised a critical result", Boolean.TRUE,
                entries(PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT).get(0).get("criticalResultEmitted"));
        assertEquals("one critical result entry, naming the diagnosis and no flag",
                List.of(Map.of("verb", PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED,
                        "conclusionDictionaryIds", List.of(PathologyFindingsDictionary.ADENOCARCINOMA),
                        "flaggedByPathologist", false)),
                entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED));
        List<Map<String, Object>> alerts = alerts();
        assertEquals("after the commit, one alert on the case", 1, alerts.size());
        Map<String, Object> alert = alerts.get(0);
        assertEquals("CRITICAL_RESULT", alert.get("alert_type"));
        assertEquals("OPEN", alert.get("status"));
        String message = String.valueOf(alert.get("message"));
        assertTrue("its message names the lab number", message.contains(accessionNumber));
        assertFalse("and no diagnosis", message.contains("Adenocarcinoma") || message.contains("C80.1"));
    }

    // K8
    @Test
    public void signingOutAnOrdinaryCase_emitsNothing() {
        codes("C");

        signOut(PathologyFindingsDictionary.CHRONIC_GASTRITIS);

        assertEquals(Boolean.FALSE,
                entries(PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT).get(0).get("criticalResultEmitted"));
        assertTrue("no critical result entry",
                entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED).isEmpty());
        assertTrue("and no alert", alerts().isEmpty());
    }

    // K9: the answer is 200 and the case is signed out
    @Test
    public void aFailingAlert_neverUndoesTheSignOut() throws Exception {
        codes("C");
        closeTheFixtureRequest();
        storeAt(PathologyStatus.UNDER_REVIEW);
        Object listener = AopTestUtils
                .getUltimateTargetObject(webApplicationContext.getBean(CriticalResultAlertListener.class));
        Object realAlerts = ReflectionTestUtils.getField(listener, "alertService");
        AlertService failing = Mockito.mock(AlertService.class);
        Mockito.when(failing.createAlert(Mockito.any(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(),
                Mockito.anyString(), Mockito.anyString())).thenThrow(new IllegalStateException("alerts are down"));
        ReflectionTestUtils.setField(listener, "alertService", failing);
        PathologyReportService reportService = webApplicationContext.getBean(PathologyReportService.class);
        StubReportRenderer renderer = StubReportRenderer.installIn(reportService);
        AnnotationConfigWebApplicationContext securityContext = new AnnotationConfigWebApplicationContext();
        try {
            securityContext.setParent(webApplicationContext);
            securityContext.setServletContext(webApplicationContext.getServletContext());
            securityContext.register(PathologyControllerTestSupport.TestConfig.class);
            securityContext.refresh();
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();

            MockHttpServletResponse response = mockMvc.perform(post("/rest/pathology/caseView/" + CASE_ID)
                    .with(user("technician1")).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"grossExam\":\"Normal tissues observed\",\"blocks\":[],\"reports\":[],"
                            + "\"slides\":[],\"release\":true,\"conclusions\":[\""
                            + PathologyFindingsDictionary.ADENOCARCINOMA + "\"]}"))
                    .andReturn().getResponse();

            assertEquals("the sign-out is answered as done", HttpStatus.OK.value(), response.getStatus());
            Mockito.verify(failing).createAlert(Mockito.any(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyString());
        } finally {
            ReflectionTestUtils.setField(listener, "alertService", realAlerts);
            renderer.restore();
            securityContext.close();
            jdbcTemplate.update("DELETE FROM clinlims.pathology_report WHERE pathology_sample_id = ? AND id <> 601",
                    CASE_ID);
        }
        assertEquals("the case is signed out", "COMPLETED", storedStage());
        assertEquals("with its critical result recorded", 1,
                entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED).size());
        assertTrue("though no alert was posted", alerts().isEmpty());
    }

    // K10
    @Test
    public void aSecondSignOut_emitsAgain_andTheOpenAlertStaysOne() {
        codes("C");
        signOut(PathologyFindingsDictionary.ADENOCARCINOMA);
        pathologySampleService.reopen(CASE_ID, PathologyStatus.COMPLETED, "Amended", TECHNICIAN_ID);

        signOut(PathologyFindingsDictionary.ADENOCARCINOMA);

        assertEquals("emitted at each sign-out", 2,
                entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED).size());
        assertEquals("the open alert stays one row", 1, alerts().size());
    }

    // K11
    @Test
    public void changingTheSetting_doesNotRewriteTheTrail() {
        codes("C");
        signOut(PathologyFindingsDictionary.ADENOCARCINOMA);
        List<Map<String, Object>> written = entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED);

        codes("");

        assertFalse("the read says not critical", criticalResult().critical());
        assertEquals("the critical result entry is as written", written,
                entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED));
        assertEquals("and so is the sign-out's", Boolean.TRUE,
                entries(PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT).get(0).get("criticalResultEmitted"));
    }

    // L9
    @Test
    public void theFlag_clearedAfterAReopen_emitsNothingTheSecondTime() {
        closeTheFixtureRequest();
        storeAt(PathologyStatus.UNDER_REVIEW);
        save(flaggedCritical(true));
        signOut();
        assertEquals("the first sign-out emits, flagged by the pathologist", List.of(true),
                entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED).stream()
                        .map(entry -> entry.get("flaggedByPathologist")).collect(Collectors.toList()));
        pathologySampleService.reopen(CASE_ID, PathologyStatus.COMPLETED, "Not malignant", TECHNICIAN_ID);
        save(flaggedCritical(false));

        signOut();

        assertEquals("the second does not", 1,
                entries(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED).size());
        assertEquals(List.of(true, false), entries(PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT).stream()
                .map(entry -> entry.get("criticalResultEmitted")).collect(Collectors.toList()));
        assertFalse("the flag is stored false", storedFlag(CASE_ID));
    }

    /** Signs the case out from Under review, with the codes given if any. */
    private void signOut(String... codes) {
        closeTheFixtureRequest();
        if (!"UNDER_REVIEW".equals(storedStage())) {
            storeAt(PathologyStatus.UNDER_REVIEW);
        }
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        if (codes.length > 0) {
            form.setConclusions(List.of(codes));
        }
        save(form);
        assertEquals("setup: the case is signed out", "COMPLETED", storedStage());
    }

    private PathologySampleForm flaggedCritical(boolean flagged) {
        PathologySampleForm form = caseViewForm();
        form.setCriticalResultFlagged(flagged);
        return form;
    }

    /** The case's entries of one verb, oldest first. */
    private List<Map<String, Object>> entries(String verb) {
        return caseAudit().stream().sorted((a, b) -> Long.compare(Long.parseLong(a.getId()), Long.parseLong(b.getId())))
                .filter(history -> changesOf(history).trim().startsWith("{")).map(this::payloadOf)
                .filter(payload -> verb.equals(payload.get("verb"))).collect(Collectors.toList());
    }

    private List<Map<String, Object>> alerts() {
        return jdbcTemplate.queryForList("SELECT alert_type, status, message FROM clinlims.alert"
                + " WHERE alert_entity_type = 'PATHOLOGY_SAMPLE' AND alert_entity_id = ?", CASE_ID);
    }

    private CriticalResult criticalResult() {
        return new TransactionTemplate(transactionManager)
                .execute(status -> criticalResultService.of(pathologySampleService.get(CASE_ID)));
    }

    private void addConclusions(String... dictionaryIds) {
        for (int i = 0; i < dictionaryIds.length; i++) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.pathology_conclusion (id, pathology_sample_id, type, value,"
                            + " last_updated) VALUES (?, ?, 'DICTIONARY', ?, now())",
                    FIRST_OWN_ROW + i, CASE_ID, dictionaryIds[i]);
        }
    }

    private void removeConclusions() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE id >= ? AND id < ?", FIRST_OWN_ROW,
                FIRST_OWN_ROW + 10);
    }

    private void setStoredFlag(boolean flagged) {
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET critical_result_flagged = ? WHERE id = ?", flagged,
                CASE_ID);
    }

    private boolean storedFlag(int caseId) {
        return jdbcTemplate.queryForObject("SELECT critical_result_flagged FROM clinlims.pathology_sample WHERE id = ?",
                Boolean.class, caseId);
    }

    private static void codes(String setting) {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_CRITICAL_RESULT_CODES, setting);
    }
}
