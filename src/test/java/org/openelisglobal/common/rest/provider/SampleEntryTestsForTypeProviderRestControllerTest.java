package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

@RunWith(MockitoJUnitRunner.class)
public class SampleEntryTestsForTypeProviderRestControllerTest {

    @Mock
    private PanelService panelService;
    @Mock
    private TestSectionService testSectionService;
    @Mock
    private TypeOfSamplePanelService samplePanelService;
    @Mock
    private PanelItemService panelItemService;
    @Mock
    private TypeOfSampleService typeOfSampleService;
    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private ProgramService programService;
    @Mock
    private TestMethodService testMethodService;
    @Mock
    private MicrobiologyReferenceService microbiologyReferenceService;
    @Mock
    private HttpServletRequest request;

    private SampleEntryTestsForTypeProviderRestController controller;

    @Before
    public void setUp() {
        controller = new SampleEntryTestsForTypeProviderRestController(panelService, testSectionService,
                samplePanelService, panelItemService, typeOfSampleService, userService, roleService, programService,
                testMethodService, microbiologyReferenceService);
        HttpSession session = mock(HttpSession.class);
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(17);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(userSessionData);
    }

    @Test
    public void userProgramsExposeStableProgramCode() throws Exception {
        Program program = new Program();
        program.setId("8");
        program.setCode("MICROBIOLOGY");
        program.setProgramName("Microbiology");
        when(userService.getUserPrograms("17", Constants.ROLE_RECEPTION))
                .thenReturn(List.of(new IdValuePair("8", "Microbiology")));
        when(programService.get("8")).thenReturn(program);

        List<SampleEntryTestsForTypeProviderRestController.ProgramOption> result = controller.getUserSPrograms(request,
                null);

        assertEquals(1, result.size());
        assertEquals("8", result.get(0).getId());
        assertEquals("Microbiology", result.get(0).getValue());
        assertEquals("MICROBIOLOGY", result.get(0).getCode());
    }

    @Test
    public void cultureTestsExposeLinkedMethodChoices() throws Exception {
        Role reception = new Role();
        reception.setId("3");
        when(roleService.getRoleByName(Constants.ROLE_RECEPTION)).thenReturn(reception);
        when(userService.getUserTestSections("17", "3")).thenReturn(List.of(new IdValuePair("9", "Microbiology")));
        when(request.getParameter("sampleType")).thenReturn("5");

        TestSection testSection = new TestSection();
        testSection.setId("9");
        when(testSectionService.getTestSectionByName("user")).thenReturn(testSection);
        org.openelisglobal.test.valueholder.Test cultureTest = new org.openelisglobal.test.valueholder.Test();
        cultureTest.setId("42");
        cultureTest.setTestSection(testSection);
        cultureTest.setCultureWorkflowType("BACTERIOLOGY");
        cultureTest.setSortOrder("1");
        Localization testName = new Localization();
        testName.setLocalizedValue("en", "Blood culture");
        cultureTest.setLocalizedTestName(testName);
        when(typeOfSampleService.getActiveTestsBySampleTypeIdAndTestUnit("5", true, List.of("9")))
                .thenReturn(List.of(cultureTest));
        when(samplePanelService.getTypeOfSamplePanelsForSampleType("5")).thenReturn(List.of());

        TestMethodDto method = new TestMethodDto();
        method.methodId = "7";
        method.methodName = "Blood Culture Standard";
        method.methodCode = "BCSTD";
        method.isDefault = true;
        when(testMethodService.getLinkedMethodDtos("42")).thenReturn(List.of(method));
        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMediaDefaults("BAP + CHOC");
        setup.setIncubationDefaults("5 days at 35 C");
        setup.setAtmosphereDefaults("aerobic + anaerobic");
        when(microbiologyReferenceService.getActiveCultureSetupForMethod("7", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(setup);

        SampleEntryTestsForTypeProviderRestController.SampleEntryTests result = controller.processRequest(request,
                null);

        assertEquals("5", result.getSampleTypeId());
        assertEquals("BACTERIOLOGY", result.getTests().get(0).getCultureWorkflowType());
        assertEquals("7", result.getTests().get(0).getMethods().get(0).methodId);
        assertEquals("Blood Culture Standard", result.getTests().get(0).getMethods().get(0).methodName);
        assertEquals("BAP + CHOC", result.getTests().get(0).getMethods().get(0).mediaDefaults);
        assertEquals("5 days at 35 C", result.getTests().get(0).getMethods().get(0).incubationDefaults);
        assertEquals("aerobic + anaerobic", result.getTests().get(0).getMethods().get(0).atmosphereDefaults);
    }
}
