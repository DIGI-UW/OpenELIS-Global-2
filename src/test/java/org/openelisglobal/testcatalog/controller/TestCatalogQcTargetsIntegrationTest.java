package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.OptionDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.ResultComponentDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.SampleResults;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogQcTargetsRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogQcTargetsRestController.EffectiveTarget;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogQcTargetsRestController.QcTargetDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogQcTargetsRestController.QcTargets;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-1148 — QC targets per control level with per-lot overrides, the
 * prefill-precedence read, and the LOD/LOQ detection limits on a component,
 * round-tripped against a real database.
 */
public class TestCatalogQcTargetsIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95321L;
    private static final String LOT_ID = "qc-target-it-lot-0001";
    private static final String ANALYZER_NAME = "QcTargetIT";
    private static final String CODED_ANSWER = "QcTargetIT positive";

    @Autowired
    private org.openelisglobal.testcatalog.service.TestQcTargetService targetService;
    @Autowired
    private org.openelisglobal.qc.service.QCControlLotService controlLotService;
    @Autowired
    private org.openelisglobal.dictionary.service.DictionaryService dictionaryService;
    @Autowired
    private org.openelisglobal.unitofmeasure.service.UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultComponentService componentService;
    @Autowired
    private TestResultInterpretationService interpretationService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;
    @Autowired
    private org.openelisglobal.testcatalog.service.RangeCoverageValidationService coverageService;
    @Autowired
    private org.openelisglobal.testsamplehandling.service.TestSampleHandlingService handlingService;
    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerService analyzerService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private org.openelisglobal.testterminology.service.TestTerminologyMappingService terminologyService;
    @Autowired
    private org.openelisglobal.panel.service.PanelService panelService;
    @Autowired
    private org.openelisglobal.panelitem.service.PanelItemService panelItemService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogQcTargetsRestController controller;
    private TestCatalogEditorRestController editor;
    private JdbcTemplate jdbc;
    private String componentId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        // The test context scans services and DAOs, not this controller package,
        // so the controllers are built directly (same as the other editor ITs).
        controller = new TestCatalogQcTargetsRestController(testService, componentService, testResultService,
                targetService, controlLotService, dictionaryService, unitOfMeasureService);
        editor = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                typeOfSampleService, typeOfSampleTestService, terminologyService, panelService, panelItemService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "QcTargetIT", "QcTargetIT desc", UUID.randomUUID().toString());
        componentId = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO clinlims.test_result_component"
                        + " (id, test_id, code, label, display_order, result_type, allow_multiple_readings,"
                        + " is_primary, show_on_report, is_active, lastupdated, last_updated)"
                        + " VALUES (?, ?, 'PRIMARY', 'Glucose', 0, 'N', false, true, true, 'Y', NOW(), NOW())",
                componentId, TEST_ID);
        Long analyzerId = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM clinlims.analyzer", Long.class);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, last_updated) VALUES (?, ?, true, NOW())",
                analyzerId, ANALYZER_NAME);
        jdbc.update("INSERT INTO qc_control_lot (id, product_name, lot_number, control_level, test_id, instrument_id,"
                + " sys_user_id, status) VALUES (?, 'Glucose control', 'LOT-0001', 'NORMAL', ?, ?, 1, 'ACTIVE')",
                LOT_ID, TEST_ID, analyzerId);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_qc_target WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM qc_control_lot WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE name = ?", ANALYZER_NAME);
        // The component save materializes a test_result row per numeric component.
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dict_entry = ?", CODED_ANSWER);
    }

    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    private static QcTargetDto target(String level, String lotId, double expected, double uncertainty) {
        QcTargetDto dto = new QcTargetDto();
        dto.controlLevel = level;
        dto.qcControlLotId = lotId;
        dto.expectedValue = BigDecimal.valueOf(expected);
        dto.uncertainty = BigDecimal.valueOf(uncertainty);
        return dto;
    }

    private static QcTargets body(QcTargetDto... targets) {
        QcTargets body = new QcTargets();
        body.targets = List.of(targets);
        return body;
    }

    private QcTargets saved(QcTargetDto... targets) {
        ResponseEntity<Object> resp = controller.saveTargets(testId(), body(targets), authedRequest());
        assertEquals(String.valueOf(resp.getBody()), 200, resp.getStatusCode().value());
        return (QcTargets) resp.getBody();
    }

    private EffectiveTarget effective(String level, String lotId) {
        return (EffectiveTarget) controller.effectiveTarget(testId(), level, lotId, null).getBody();
    }

    @org.junit.Test
    public void emptyTest_isQuantitative_listsTheLevelsAndTheTestsLots() {
        QcTargets loaded = controller.getTargets(testId()).getBody();
        assertTrue(loaded.quantitative);
        assertEquals(List.of("LOW", "NORMAL", "HIGH"), loaded.levels);
        assertTrue(loaded.targets.isEmpty());
        assertEquals(1, loaded.lots.size());
        assertEquals(LOT_ID, loaded.lots.get(0).id);
        assertEquals("Glucose control LOT-0001", loaded.lots.get(0).label);
    }

    @org.junit.Test
    public void levelDefault_roundTrips_andPrefillsWhenNoLotOverrides() {
        QcTargets after = saved(target("NORMAL", null, 5.5d, 0.4d));
        assertEquals(1, after.targets.size());
        QcTargetDto written = after.targets.get(0);
        assertEquals("NORMAL", written.controlLevel);
        assertNull(written.qcControlLotId);
        assertEquals(0, new BigDecimal("5.5").compareTo(written.expectedValue));
        assertTrue(written.active);

        EffectiveTarget forLot = effective("NORMAL", LOT_ID);
        assertEquals("LEVEL", forLot.source);
        assertEquals(written.id, forLot.target.id);
        assertEquals("NONE", effective("LOW", null).source);
    }

    @org.junit.Test
    public void lotOverride_winsForItsLot_andFallsBackWhenDeactivated() {
        QcTargets after = saved(target("NORMAL", null, 5.5d, 0.4d), target("NORMAL", LOT_ID, 5.9d, 0.3d));
        QcTargetDto override = after.targets.stream().filter(t -> LOT_ID.equals(t.qcControlLotId)).findFirst().get();
        assertEquals("Glucose control LOT-0001", override.lotLabel);

        EffectiveTarget forLot = effective("NORMAL", LOT_ID);
        assertEquals("LOT", forLot.source);
        assertEquals(0, new BigDecimal("5.9").compareTo(forLot.target.expectedValue));
        assertEquals("LEVEL", effective("NORMAL", null).source);

        override.active = false;
        saved(override);
        EffectiveTarget fallback = effective("NORMAL", LOT_ID);
        assertEquals("the deactivated override no longer applies", "LEVEL", fallback.source);
        assertEquals("deactivated, never deleted", 2, controller.getTargets(testId()).getBody().targets.size());
    }

    @org.junit.Test
    public void rejectedTargets_are422_andWriteNothing() {
        assertEquals(422, controller.saveTargets(testId(), body(target("NORMAL", null, 5.5d, -0.1d)), authedRequest())
                .getStatusCode().value());
        QcTargetDto noValue = target("NORMAL", null, 5.5d, 0.4d);
        noValue.expectedValue = null;
        assertEquals(422, controller.saveTargets(testId(), body(noValue), authedRequest()).getStatusCode().value());
        assertEquals(422,
                controller.saveTargets(testId(), body(target("NORMAL", "no-such-lot", 5.5d, 0.4d)), authedRequest())
                        .getStatusCode().value());
        assertEquals(422, controller.saveTargets(testId(), body(target("MEDIUM", null, 5.5d, 0.4d)), authedRequest())
                .getStatusCode().value());
        assertEquals(422,
                controller.saveTargets(testId(),
                        body(target("NORMAL", null, 5.5d, 0.4d), target("NORMAL", null, 6d, 0.4d)), authedRequest())
                        .getStatusCode().value());
        assertTrue(controller.getTargets(testId()).getBody().targets.isEmpty());
    }

    private void insertTarget(String id, String level, Double expected, Double uncertainty, String dictId) {
        jdbc.update(
                "INSERT INTO clinlims.test_qc_target (id, test_id, control_level, expected_value, uncertainty,"
                        + " expected_dict_result_id, is_active, last_updated) VALUES (?, ?, ?, ?, ?, ?, true, NOW())",
                id, TEST_ID, level, expected, uncertainty, dictId == null ? null : Long.valueOf(dictId));
    }

    private boolean targetActive(String id) {
        return jdbc.queryForObject("SELECT is_active FROM clinlims.test_qc_target WHERE id = ?", Boolean.class, id);
    }

    private void runUpgradeRetire() throws Exception {
        String xml = new String(getClass().getClassLoader()
                .getResourceAsStream("liquibase/3.5.x.x/137-qc-targets-retire-stale.xml").readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        jdbc.update(xml.substring(xml.indexOf("<sql>") + 5, xml.indexOf("</sql>")));
    }

    /**
     * OGC-1462: a test whose result type changed before the fix kept its old
     * targets active; the upgrade retires them once, keeping their values, and
     * leaves the targets that fit.
     */
    @org.junit.Test
    public void theUpgradeRetiresTargetsOfTheOldShape_andKeepsTheirValues() throws Exception {
        String answerId = makeCoded();
        jdbc.update("DELETE FROM clinlims.test_qc_target WHERE test_id = ?", TEST_ID);
        insertTarget("upgrade-numeric", "LOW", 5d, 1d, null);
        insertTarget("upgrade-coded", "NORMAL", null, null, answerId);

        runUpgradeRetire();

        assertFalse("a numeric target on a coded test is retired", targetActive("upgrade-numeric"));
        assertEquals(0,
                new BigDecimal("5").compareTo(jdbc.queryForObject(
                        "SELECT expected_value FROM clinlims.test_qc_target WHERE id = 'upgrade-numeric'",
                        BigDecimal.class)));
        assertTrue("a coded target on a coded test stays", targetActive("upgrade-coded"));

        jdbc.update("UPDATE clinlims.test_result_component SET result_type = 'N' WHERE test_id = ? AND is_primary",
                TEST_ID);
        jdbc.update("UPDATE clinlims.test_qc_target SET is_active = true WHERE id = 'upgrade-numeric'");

        runUpgradeRetire();

        assertTrue("a numeric target on a numeric test stays", targetActive("upgrade-numeric"));
        assertFalse("a coded target on a numeric test is retired", targetActive("upgrade-coded"));
    }

    /**
     * OGC-1467: a value wider than numeric(15,5) and an expected answer that is not
     * a dictionary entry are refused instead of failing the write with a 500.
     */
    @org.junit.Test
    public void valuesTheColumnsCannotHold_andUnknownAnswers_are422() {
        assertEquals(422,
                controller.saveTargets(testId(), body(target("NORMAL", null, 12345678901d, 0.4d)), authedRequest())
                        .getStatusCode().value());
        assertEquals(422,
                controller.saveTargets(testId(), body(target("NORMAL", null, 5.5d, 12345678901d)), authedRequest())
                        .getStatusCode().value());
        for (String answer : new String[] { "abc", "99999999" }) {
            QcTargetDto coded = target("NORMAL", null, 5.5d, 0.4d);
            coded.expectedDictResultId = answer;
            assertEquals(answer, 422,
                    controller.saveTargets(testId(), body(coded), authedRequest()).getStatusCode().value());
        }
        assertTrue(controller.getTargets(testId()).getBody().targets.isEmpty());

        QcTargets widest = saved(target("NORMAL", null, 9999999999d, 0.4d));
        assertEquals(0, new BigDecimal("9999999999").compareTo(widest.targets.get(0).expectedValue));
    }

    /** Switches the primary component to a coded result with one answer. */
    private String makeCoded() {
        SampleResults current = editor.getSampleResults(testId()).getBody();
        ResultComponentDto primary = current.components.get(0);
        primary.resultType = "D";
        OptionDto answer = new OptionDto();
        answer.value = CODED_ANSWER;
        answer.valueName = CODED_ANSWER;
        answer.freeText = true;
        answer.resultType = "D";
        answer.sortOrder = 1;
        answer.normal = false;
        primary.options = List.of(answer);
        ResponseEntity<?> resp = editor.saveSampleResults(testId(), current, authedRequest());
        assertEquals(String.valueOf(resp.getBody()), 200, resp.getStatusCode().value());
        QcTargets loaded = controller.getTargets(testId()).getBody();
        assertFalse("the test is coded now", loaded.quantitative);
        return loaded.dictionaryOptions.get(0).id;
    }

    /** OGC-1462: a row added and switched off before it was filled. */
    @org.junit.Test
    public void aBlankTargetDeactivatedBeforeItWasFilled_savesAndStoresNothing() {
        QcTargetDto blank = new QcTargetDto();
        blank.controlLevel = "HIGH";
        blank.active = false;

        QcTargets after = saved(blank);

        assertTrue(after.targets.isEmpty());
    }

    /**
     * OGC-1462: changing the result type retires the targets of the old shape (so
     * Results Entry stops prefilling them) and keeps their values; the section can
     * then save targets of the new shape, sending the retired rows back empty.
     */
    @org.junit.Test
    public void afterTheResultTypeChanges_oldTargetsAreRetired_andNewOnesSave() {
        QcTargetDto numeric = saved(target("LOW", null, 5d, 1d)).targets.get(0);
        assertEquals("LEVEL", effective("LOW", null).source);

        String answerId = makeCoded();

        QcTargetDto retired = controller.getTargets(testId()).getBody().targets.get(0);
        assertEquals(numeric.id, retired.id);
        assertFalse("a numeric target does not fit a coded test", retired.active);
        assertEquals(0, new BigDecimal("5").compareTo(retired.expectedValue));
        assertEquals("NONE", effective("LOW", null).source);

        retired.expectedValue = null;
        retired.uncertainty = null;
        QcTargetDto coded = new QcTargetDto();
        coded.controlLevel = "NORMAL";
        coded.expectedDictResultId = answerId;
        QcTargets after = saved(retired, coded);

        QcTargetDto kept = after.targets.stream().filter(t -> numeric.id.equals(t.id)).findFirst().get();
        assertFalse(kept.active);
        assertEquals("a retired target keeps what it held", 0, new BigDecimal("5").compareTo(kept.expectedValue));
        assertEquals(answerId, effective("NORMAL", null).target.expectedDictResultId);
    }

    /**
     * OGC-1462: reactivating a retired target needs values of the current shape.
     */
    @org.junit.Test
    public void aRetiredTargetReactivatedWithoutValuesOfTheNewShape_isRefused() {
        saved(target("LOW", null, 5d, 1d));
        makeCoded();
        QcTargetDto retired = controller.getTargets(testId()).getBody().targets.get(0);
        retired.active = true;

        assertEquals(422, controller.saveTargets(testId(), body(retired), authedRequest()).getStatusCode().value());
        assertFalse(controller.getTargets(testId()).getBody().targets.get(0).active);
    }

    @org.junit.Test
    public void detectionLimits_roundTripOnTheComponent_andLodAboveLoqIsRejected() {
        SampleResults current = editor.getSampleResults(testId()).getBody();
        ResultComponentDto component = current.components.get(0);
        component.lod = new BigDecimal("0.1");
        component.loq = new BigDecimal("0.3");
        assertEquals(200, editor.saveSampleResults(testId(), current, authedRequest()).getStatusCode().value());

        ResultComponentDto reloaded = editor.getSampleResults(testId()).getBody().components.get(0);
        assertEquals(0, new BigDecimal("0.1").compareTo(reloaded.lod));
        assertEquals(0, new BigDecimal("0.3").compareTo(reloaded.loq));

        reloaded.lod = new BigDecimal("0.5");
        SampleResults invalid = editor.getSampleResults(testId()).getBody();
        invalid.components.get(0).lod = new BigDecimal("0.5");
        assertEquals(422, editor.saveSampleResults(testId(), invalid, authedRequest()).getStatusCode().value());
        assertEquals("a rejected save leaves the stored limits alone", 0,
                new BigDecimal("0.1").compareTo(editor.getSampleResults(testId()).getBody().components.get(0).lod));
    }
}
