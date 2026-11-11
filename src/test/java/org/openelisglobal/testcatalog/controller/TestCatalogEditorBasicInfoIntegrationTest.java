package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.BasicInfo;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-748 Basic Info — round-trip against a real DB: load a test's basic-info,
 * change Domain + AMR + status, save, reload, and confirm the M1 columns
 * (TEST.DOMAIN, TEST.ANTIMICROBIAL_RESISTANCE) and status persisted.
 *
 * <p>
 * Also pins the /tests list endpoint's filters (domain, status, amr, search),
 * case-insensitive name sort, and pagination — the behavior contract that a
 * later move to a DB-side projection query must preserve.
 */
public class TestCatalogEditorBasicInfoIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95001L;
    private static final long LAB_UNIT_ID = 95090L;

    @Autowired
    private TestService testService;

    @Autowired
    private StaleSaveGuard staleSaveGuard;

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
    private javax.sql.DataSource dataSource;

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

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        // Controllers live in the servlet context, not the test's root context; build
        // one with the real (autowired) services via constructor injection so the
        // save logic hits a real DB. Constructor injection means a new controller
        // dependency is a compile error here, not a runtime NPE.
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                typeOfSampleService, typeOfSampleTestService, terminologyService, panelService, panelItemService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, antimicrobial_resistance,"
                        + " orderable, lastupdated) VALUES (?, ?, ?, 'Y', ?, 'CLINICAL', false, true, NOW())",
                TEST_ID, "BasicInfoIT", "BasicInfoIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    /**
     * A request carrying the audit user (id 1, seeded by the test base) for
     * saveBasicInfo.
     */
    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    @org.junit.Test
    public void basicInfo_roundTrips_domainAmrAndStatus() {
        ResponseEntity<BasicInfo> loaded = controller.getBasicInfo(String.valueOf(TEST_ID));
        assertEquals(200, loaded.getStatusCode().value());
        assertEquals("CLINICAL", loaded.getBody().domain);
        assertTrue(!loaded.getBody().antimicrobialResistance);

        BasicInfo update = new BasicInfo();
        update.domain = "VECTOR";
        update.antimicrobialResistance = true;
        update.active = true;
        update.orderable = false;
        ResponseEntity<BasicInfo> saved = controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest());
        assertEquals(200, saved.getStatusCode().value());
        assertEquals("VECTOR", saved.getBody().domain);
        assertTrue(saved.getBody().antimicrobialResistance);

        // Reload from DB through the service to confirm persistence.
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("VECTOR", reloaded.getDomain());
        assertTrue(Boolean.TRUE.equals(reloaded.getAntimicrobialResistance()));
        assertTrue(!Boolean.TRUE.equals(reloaded.getOrderable()));
    }

    /**
     * OGC-1376: an editor opened before someone else saved the test used to save
     * everything it showed, silently undoing that change.
     */
    @org.junit.Test
    public void basicInfo_aSaveFromAStaleEditorIsRefusedAndChangesNothing() {
        ReflectionTestUtils.setField(controller, "staleSaveGuard", staleSaveGuard);
        String loaded = controller.getBasicInfo(String.valueOf(TEST_ID)).getBody().lastupdated;
        BasicInfo stale = new BasicInfo();
        stale.orderable = false;
        stale.lastupdated = String.valueOf(Long.parseLong(loaded) - 60_000);

        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), stale, authedRequest());

        assertEquals(409, resp.getStatusCode().value());
        assertEquals("stale", resp.getBody().conflict);
        assertEquals("error.testCatalog.staleSave", resp.getBody().messageKey);
        assertEquals(loaded, resp.getBody().lastupdated);
        assertTrue(Boolean.TRUE.equals(testService.getTestById(String.valueOf(TEST_ID)).getOrderable()));
    }

    @org.junit.Test
    public void basicInfo_consecutiveSavesWithTheReturnedVersionAreAccepted() {
        ReflectionTestUtils.setField(controller, "staleSaveGuard", staleSaveGuard);
        BasicInfo first = new BasicInfo();
        first.orderable = false;
        first.lastupdated = controller.getBasicInfo(String.valueOf(TEST_ID)).getBody().lastupdated;
        ResponseEntity<BasicInfo> saved = controller.saveBasicInfo(String.valueOf(TEST_ID), first, authedRequest());
        assertEquals(200, saved.getStatusCode().value());

        BasicInfo second = new BasicInfo();
        second.orderable = true;
        second.lastupdated = saved.getBody().lastupdated;
        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), second, authedRequest()).getStatusCode().value());
        assertTrue(Boolean.TRUE.equals(testService.getTestById(String.valueOf(TEST_ID)).getOrderable()));
    }

    @org.junit.Test
    public void basicInfo_rejectsInvalidDomain() {
        BasicInfo bad = new BasicInfo();
        bad.domain = "NONSENSE";
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void basicInfo_rejectsImmutableFieldChange() {
        // Name/code/description are read-only in v1 (OGC-950); submitting a
        // changed value for one must be rejected (422), not silently ignored.
        BasicInfo bad = new BasicInfo();
        bad.name = "Renamed-" + TEST_ID;
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void basicInfo_unknownTestReturns404() {
        ResponseEntity<BasicInfo> resp = controller.getBasicInfo("99999999");
        assertEquals(404, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void getEditorEnvelope_returns200WithApplicableSections() {
        ResponseEntity<TestCatalogEditorRestController.EditorEnvelope> resp = controller
                .getEditorEnvelope(String.valueOf(TEST_ID));
        assertEquals(200, resp.getStatusCode().value());
        TestCatalogEditorRestController.EditorEnvelope env = resp.getBody();
        assertEquals(String.valueOf(TEST_ID), env.testId);
        assertEquals("CLINICAL", env.domain);
        // The full v1 section set, in order, is the whole point of the envelope (M2).
        assertEquals(java.util.List.of("basic-info", "sample-results", "methods", "ranges", "qc-targets", "storage",
                "panels", "terminology", "analyzers", "display-order"), env.applicableSections);
    }

    @org.junit.Test
    public void basicInfo_partialPut_preservesUnsentFlags() {
        // Arrange: AMR + active + orderable all true.
        BasicInfo setup = new BasicInfo();
        setup.antimicrobialResistance = true;
        setup.active = true;
        setup.orderable = true;
        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), setup, authedRequest()).getStatusCode().value());

        // Act: PUT only the domain — every flag field is null.
        BasicInfo partial = new BasicInfo();
        partial.domain = "VECTOR";
        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), partial, authedRequest()).getStatusCode().value());

        // Assert: the unsent flags are preserved, NOT silently reset to false
        // (the boxed-Boolean apply-if-present contract).
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("VECTOR", reloaded.getDomain());
        assertTrue(Boolean.TRUE.equals(reloaded.getAntimicrobialResistance()));
        assertTrue(reloaded.isActive());
        assertTrue(Boolean.TRUE.equals(reloaded.getOrderable()));
    }

    @org.junit.Test
    public void basicInfo_cannotActivateAnInactiveTest_mustUseTheGatedEndpoint() {
        // Basic-info CAN deactivate.
        BasicInfo off = new BasicInfo();
        off.active = false;
        controller.saveBasicInfo(String.valueOf(TEST_ID), off, authedRequest());
        assertTrue(!testService.getTestById(String.valueOf(TEST_ID)).isActive());

        // But it must NOT flip an inactive test back to active — activation is gated
        // on reference-range coverage via POST .../activate (the H-03 safety gate).
        BasicInfo on = new BasicInfo();
        on.active = true;
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), on, authedRequest());
        // The request is refused rather than answered 200 and dropped: a caller that
        // asked for activation and got a success it did not receive has no way to
        // notice the flag never moved.
        assertEquals(409, resp.getStatusCode().value());
        assertTrue("basic-info must not bypass the activation coverage gate",
                !testService.getTestById(String.valueOf(TEST_ID)).isActive());
    }

    /**
     * Re-sending active=true for a test that is already active is not a change, so
     * a client round-tripping the whole form must still be accepted.
     */
    @org.junit.Test
    public void basicInfo_acceptsActiveTrueWhenTheTestIsAlreadyActive() {
        BasicInfo on = new BasicInfo();
        on.active = true;
        on.description = "still active";
        assertTrue(testService.getTestById(String.valueOf(TEST_ID)).isActive());

        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), on, authedRequest());

        assertEquals(200, resp.getStatusCode().value());
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertTrue(reloaded.isActive());
        assertEquals("still active", reloaded.getDescription());
    }

    @org.junit.Test
    public void basicInfo_persistsCodeChange() {
        // OGC-1112 dependency 8: Code is editable in Basic Info now.
        BasicInfo body = new BasicInfo();
        body.code = "NEWCODE" + TEST_ID;
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), body, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("NEWCODE" + TEST_ID, testService.getTestById(String.valueOf(TEST_ID)).getLocalCode());
    }

    @org.junit.Test
    public void basicInfo_persistsDescriptionChange() {
        // OGC-1112 dependency 8: Description is editable in Basic Info now.
        BasicInfo body = new BasicInfo();
        body.description = "Changed description";
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), body, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Changed description", testService.getTestById(String.valueOf(TEST_ID)).getDescription());
    }

    @org.junit.Test
    public void listTests_filtersByDomainAndPaginates() {
        // Seed three catalog rows: 2 CLINICAL, 1 VECTOR (ids in a cleaned range).
        for (long id = 95010L; id <= 95012L; id++) {
            String dom = id == 95012L ? "VECTOR" : "CLINICAL";
            jdbc.update(
                    "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain,"
                            + " antimicrobial_resistance, orderable, lastupdated)"
                            + " VALUES (?, ?, ?, 'Y', ?, ?, false, true, NOW())",
                    id, "ListIT-" + id, "ListIT-" + id, UUID.randomUUID().toString(), dom);
        }

        TestCatalogEditorRestController.TestListPage vector = controller.listTests("VECTOR", "all", null, null, null,
                "ListIT-", false, 1, 25);
        assertEquals(1, vector.total);
        assertEquals("VECTOR", vector.rows.get(0).domain);

        TestCatalogEditorRestController.TestListPage all = controller.listTests(null, "all", null, null, null,
                "ListIT-", false, 1, 2);
        assertEquals(3, all.total);
        assertEquals(2, all.rows.size()); // page size 2 of 3 total

        TestCatalogEditorRestController.TestListPage page2 = controller.listTests(null, "all", null, null, null,
                "ListIT-", false, 2, 2);
        assertEquals(1, page2.rows.size());
    }

    @org.junit.Test
    public void listTests_filtersByStatus() {
        seedTest(95020L, "StatusIT-active1", "CLINICAL", true, false);
        seedTest(95021L, "StatusIT-active2", "CLINICAL", true, false);
        seedTest(95022L, "StatusIT-inactive", "CLINICAL", false, false);

        assertEquals(3, controller.listTests(null, "all", null, null, null, "StatusIT-", false, 1, 25).total);
        assertEquals(2, controller.listTests(null, "active", null, null, null, "StatusIT-", false, 1, 25).total);
        assertEquals(1, controller.listTests(null, "inactive", null, null, null, "StatusIT-", false, 1, 25).total);
    }

    @org.junit.Test
    public void listTests_filtersByAmr() {
        seedTest(95023L, "AmrIT-yes", "CLINICAL", true, true);
        seedTest(95024L, "AmrIT-no1", "CLINICAL", true, false);
        seedTest(95025L, "AmrIT-no2", "CLINICAL", true, false);

        assertEquals(3, controller.listTests(null, "all", null, null, null, "AmrIT-", false, 1, 25).total);
        assertEquals(1, controller.listTests(null, "all", true, null, null, "AmrIT-", false, 1, 25).total);
        assertEquals(2, controller.listTests(null, "all", false, null, null, "AmrIT-", false, 1, 25).total);
    }

    @org.junit.Test
    public void listTests_searchIsCaseInsensitiveSubstring() {
        seedTest(95026L, "ZebraSrchIT", "CLINICAL", true, false);
        seedTest(95027L, "alphaSrchIT", "CLINICAL", true, false);

        // a lowercase query matches both mixed-case names (case-insensitive)
        assertEquals(2, controller.listTests(null, "all", null, null, null, "srchit", false, 1, 25).total);
        // a distinct fragment narrows to one
        assertEquals(1, controller.listTests(null, "all", null, null, null, "ZEBRA", false, 1, 25).total);
    }

    @org.junit.Test
    public void listTests_sortsByNameCaseInsensitive() {
        seedTest(95028L, "banana SortIT", "CLINICAL", true, false);
        seedTest(95029L, "Apple SortIT", "CLINICAL", true, false);
        seedTest(95030L, "cherry SortIT", "CLINICAL", true, false);

        java.util.List<TestCatalogEditorRestController.TestListRow> rows = controller.listTests(null, "all", null, null,
                null, "SortIT", false, 1, 25).rows;
        assertEquals(3, rows.size());
        assertEquals("Apple SortIT", rows.get(0).name);
        assertEquals("banana SortIT", rows.get(1).name);
        assertEquals("cherry SortIT", rows.get(2).name);
    }

    @org.junit.Test
    public void listTests_filtersByLabUnit() {
        seedLabUnit();
        seedTest(95031L, "Hema LabUnitIT", "CLINICAL", true, false);
        seedTest(95032L, "Other LabUnitIT", "CLINICAL", true, false);
        jdbc.update("UPDATE clinlims.test SET test_section_id = ? WHERE id = ?", LAB_UNIT_ID, 95031L);

        TestCatalogEditorRestController.TestListPage page = controller.listTests(null, "all", null, null,
                String.valueOf(LAB_UNIT_ID), "LabUnitIT", false, 1, 25);

        assertEquals(1, page.total);
        assertEquals("Hema LabUnitIT", page.rows.get(0).name);
        assertEquals(2, controller.listTests(null, "all", null, null, null, "LabUnitIT", false, 1, 25).total);
    }

    @org.junit.Test
    public void basicInfo_roundTripsHoldingTimeInLabOnlyAndNotifyResults() {
        BasicInfo update = new BasicInfo();
        update.timeHolding = "120";
        update.inLabOnly = true;
        update.notifyResults = true;

        ResponseEntity<BasicInfo> saved = controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest());

        assertEquals(200, saved.getStatusCode().value());
        assertEquals("120", saved.getBody().timeHolding);
        assertTrue(saved.getBody().inLabOnly);
        assertTrue(saved.getBody().notifyResults);
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("120", reloaded.getTimeHolding());
        assertTrue(reloaded.isInLabOnly());
        assertTrue(reloaded.isNotifyResults());
    }

    @org.junit.Test
    public void basicInfo_aSaveWithoutTheNewFieldsLeavesThemAlone() {
        jdbc.update(
                "UPDATE clinlims.test SET time_holding = 90, in_lab_only = true, notify_results = true WHERE id = ?",
                TEST_ID);
        BasicInfo update = new BasicInfo();
        update.orderable = false;

        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest()).getStatusCode().value());

        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("90", reloaded.getTimeHolding());
        assertTrue(reloaded.isInLabOnly());
        assertTrue(reloaded.isNotifyResults());
    }

    @org.junit.Test
    public void basicInfo_aBlankHoldingTimeClearsItAndFalseFlagsTurnThemOff() {
        jdbc.update(
                "UPDATE clinlims.test SET time_holding = 90, in_lab_only = true, notify_results = true WHERE id = ?",
                TEST_ID);
        BasicInfo update = new BasicInfo();
        update.timeHolding = "";
        update.inLabOnly = false;
        update.notifyResults = false;

        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest()).getStatusCode().value());

        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals(null, reloaded.getTimeHolding());
        assertTrue(!reloaded.isInLabOnly());
        assertTrue(!reloaded.isNotifyResults());
    }

    @org.junit.Test
    public void basicInfo_refusesAHoldingTimeThatIsNotAWholeNumberOfMinutes() {
        jdbc.update("UPDATE clinlims.test SET time_holding = 90 WHERE id = ?", TEST_ID);
        for (String bad : new String[] { "-5", "1.5", "two hours", "99999999999" }) {
            BasicInfo update = new BasicInfo();
            update.timeHolding = bad;
            update.orderable = false;

            ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest());

            assertEquals(bad, 422, resp.getStatusCode().value());
            assertEquals(bad, "timeHolding", resp.getBody().invalidField);
        }
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("90", reloaded.getTimeHolding());
        assertTrue(Boolean.TRUE.equals(reloaded.getOrderable()));
    }

    @org.junit.Test
    public void basicInfo_savesUpdatesAndClearsQcThresholds() {
        BasicInfo create = new BasicInfo();
        create.qcBlankThreshold = "0.5";
        create.qcRpdThreshold = "20";
        create.qcRecoveryWindowPct = "";

        ResponseEntity<BasicInfo> saved = controller.saveBasicInfo(String.valueOf(TEST_ID), create, authedRequest());

        assertEquals(200, saved.getStatusCode().value());
        assertEquals("0.5", saved.getBody().qcBlankThreshold);
        assertEquals("20", saved.getBody().qcRpdThreshold);
        assertEquals("", saved.getBody().qcRecoveryWindowPct);
        assertEquals(1, thresholdRows());

        BasicInfo change = new BasicInfo();
        change.qcBlankThreshold = "0.5";
        change.qcRpdThreshold = "15";
        change.qcRecoveryWindowPct = "10";
        controller.saveBasicInfo(String.valueOf(TEST_ID), change, authedRequest());
        assertEquals("15", testService.getQcThreshold(String.valueOf(TEST_ID)).get().getRpdThreshold()
                .stripTrailingZeros().toPlainString());
        assertEquals("10", testService.getQcThreshold(String.valueOf(TEST_ID)).get().getRecoveryWindowPct()
                .stripTrailingZeros().toPlainString());

        BasicInfo untouched = new BasicInfo();
        untouched.orderable = true;
        controller.saveBasicInfo(String.valueOf(TEST_ID), untouched, authedRequest());
        assertEquals(1, thresholdRows());

        BasicInfo clear = new BasicInfo();
        clear.qcBlankThreshold = "";
        clear.qcRpdThreshold = " ";
        clear.qcRecoveryWindowPct = "";
        ResponseEntity<BasicInfo> cleared = controller.saveBasicInfo(String.valueOf(TEST_ID), clear, authedRequest());
        assertEquals(200, cleared.getStatusCode().value());
        assertEquals(0, thresholdRows());
        assertEquals("", cleared.getBody().qcBlankThreshold);
    }

    @org.junit.Test
    public void basicInfo_refusesANegativeOrNonNumericQcThreshold() {
        for (String bad : new String[] { "-1", "abc" }) {
            BasicInfo update = new BasicInfo();
            update.qcRpdThreshold = bad;
            update.orderable = false;

            ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest());

            assertEquals(bad, 422, resp.getStatusCode().value());
            assertEquals(bad, "qcRpdThreshold", resp.getBody().invalidField);
        }
        assertEquals(0, thresholdRows());
        assertTrue(Boolean.TRUE.equals(testService.getTestById(String.valueOf(TEST_ID)).getOrderable()));
    }

    private int thresholdRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.test_qc_threshold WHERE test_id = ?", Integer.class,
                TEST_ID);
    }

    private void seedLabUnit() {
        jdbc.update("INSERT INTO clinlims.localization (id, description) VALUES (?, 'LabUnitIT')", LAB_UNIT_ID);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                        + " name_localization_id) VALUES (?, 'LabUnitIT', 'LabUnitIT', 'N', ?, ?)",
                LAB_UNIT_ID, LAB_UNIT_ID, LAB_UNIT_ID);
    }

    private void seedTest(long id, String name, String domain, boolean active, boolean amr) {
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, domain,"
                + " antimicrobial_resistance, orderable, lastupdated)" + " VALUES (?, ?, ?, ?, ?, ?, ?, true, NOW())",
                id, name, name, active ? "Y" : "N", UUID.randomUUID().toString(), domain, amr);
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE FROM clinlims.test_qc_threshold WHERE test_id = ?", TEST_ID);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ? OR (id >= 95010 AND id <= 95099)", TEST_ID);
            jdbc.update("DELETE FROM clinlims.test_section WHERE id = ?", LAB_UNIT_ID);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", LAB_UNIT_ID);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
