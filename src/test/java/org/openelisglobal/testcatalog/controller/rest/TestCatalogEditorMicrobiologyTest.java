package org.openelisglobal.testcatalog.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.BasicInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

public class TestCatalogEditorMicrobiologyTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 78201L;

    @Autowired
    private TestService testService;

    @Autowired
    private org.openelisglobal.testresultcomponent.service.TestResultComponentService componentService;

    @Autowired
    private org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService interpretationService;

    @Autowired
    private org.openelisglobal.testresult.service.TestResultService testResultService;

    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;

    @Autowired
    private org.openelisglobal.testcatalog.service.RangeCoverageValidationService coverageService;

    @Autowired
    private org.openelisglobal.testsamplehandling.service.TestSampleHandlingService handlingService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerService analyzerService;

    @Autowired
    private org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService analyzerTestMappingService;

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

    private JdbcTemplate jdbc;
    private TestCatalogEditorRestController controller;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                analyzerTestMappingService, typeOfSampleService, typeOfSampleTestService, terminologyService,
                panelService, panelItemService);
        cleanup();
        jdbc.update("INSERT INTO clinlims.test"
                + " (id, name, description, is_active, guid, domain, antimicrobial_resistance, orderable,"
                + " culture_workflow_type, lastupdated)"
                + " VALUES (?, 'MicroCatalogIT', 'MicroCatalogIT desc', 'Y', ?, 'CLINICAL', true, true, null, NOW())",
                TEST_ID, UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void basicInfoRoundTripsCultureWorkflowType() {
        BasicInfo update = new BasicInfo();
        update.cultureWorkflowType = "BACTERIOLOGY";

        ResponseEntity<BasicInfo> saved = controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest());

        assertEquals(200, saved.getStatusCode().value());
        assertEquals("BACTERIOLOGY", saved.getBody().cultureWorkflowType);
        org.openelisglobal.test.valueholder.Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("BACTERIOLOGY", reloaded.getCultureWorkflowType());

        BasicInfo clear = new BasicInfo();
        clear.cultureWorkflowType = "";
        ResponseEntity<BasicInfo> cleared = controller.saveBasicInfo(String.valueOf(TEST_ID), clear, authedRequest());

        assertEquals(200, cleared.getStatusCode().value());
        assertNull(cleared.getBody().cultureWorkflowType);
        assertNull(testService.getTestById(String.valueOf(TEST_ID)).getCultureWorkflowType());
    }

    @Test
    public void basicInfoRejectsInvalidCultureWorkflowType() {
        BasicInfo bad = new BasicInfo();
        bad.cultureWorkflowType = "IMPLEMENTATION_DETAIL";

        ResponseEntity<BasicInfo> response = controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest());

        assertEquals(422, response.getStatusCode().value());
    }

    private void cleanup() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        }
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
}
