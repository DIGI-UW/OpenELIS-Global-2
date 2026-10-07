package org.openelisglobal.testcatalog.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.BasicInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TestCatalogEditorMicrobiologyTest extends BaseWebContextSensitiveTest {

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
    private MicrobiologyTestFixtures fixtures;

    private TestCatalogEditorRestController controller;
    private String catalogTestId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                typeOfSampleService, typeOfSampleTestService, terminologyService, panelService, panelItemService);
        catalogTestId = fixtures.createCatalogTest().getId();
    }

    @Test
    public void basicInfoRoundTripsCaseOpeningRoleAndSetsWithoutChangingSurveillanceFlag() {
        boolean surveillance = testService.get(catalogTestId).getAntimicrobialResistance();
        BasicInfo update = new BasicInfo();
        update.opensMicrobiologyCase = true;
        update.microbiologyCaseRole = "CULTURE";
        update.collectedInSets = true;
        var saved = controller.saveBasicInfo(catalogTestId, update, authedRequest());
        assertEquals(200, saved.getStatusCode().value());
        var reloaded = testService.getTestById(catalogTestId);
        assertTrue(reloaded.isOpensMicrobiologyCase());
        assertEquals("CULTURE", reloaded.getMicrobiologyCaseRole());
        assertTrue(reloaded.isCollectedInSets());
        assertEquals(surveillance, reloaded.getAntimicrobialResistance());

        BasicInfo clear = new BasicInfo();
        clear.opensMicrobiologyCase = false;
        clear.collectedInSets = false;
        clear.microbiologyCaseRole = "DIRECT";
        assertEquals(200, controller.saveBasicInfo(catalogTestId, clear, authedRequest()).getStatusCode().value());
        assertFalse(testService.getTestById(catalogTestId).isOpensMicrobiologyCase());
        assertEquals(surveillance, testService.getTestById(catalogTestId).getAntimicrobialResistance());
    }

    @Test
    public void basicInfoRejectsInvalidCaseRoleWithoutChangingMaster() {
        BasicInfo bad = new BasicInfo();
        bad.microbiologyCaseRole = "IMPLEMENTATION_DETAIL";
        assertEquals(422, controller.saveBasicInfo(catalogTestId, bad, authedRequest()).getStatusCode().value());
        assertEquals("DIRECT", testService.get(catalogTestId).getMicrobiologyCaseRole());
        assertFalse(testService.get(catalogTestId).isOpensMicrobiologyCase());
    }

    @Test
    public void collectedInSetsRequiresAnOpenCultureCase() {
        BasicInfo bad = new BasicInfo();
        bad.opensMicrobiologyCase = true;
        bad.microbiologyCaseRole = "DIRECT";
        bad.collectedInSets = true;
        assertEquals(422, controller.saveBasicInfo(catalogTestId, bad, authedRequest()).getStatusCode().value());
        assertFalse(testService.get(catalogTestId).isCollectedInSets());
        assertFalse(testService.get(catalogTestId).isOpensMicrobiologyCase());
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
