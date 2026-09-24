package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
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
