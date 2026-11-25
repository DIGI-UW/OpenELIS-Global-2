package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
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
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.qc.dao.TestQcThresholdDAO;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;

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
    private TestQcThresholdDAO testQcThresholdDAO;
    @Mock
    private TestService testService;
    @Mock
    private TypeOfSampleTestService typeOfSampleTestService;
    @Mock
    private HttpServletRequest request;

    private SampleEntryTestsForTypeProviderRestController controller;

    @Before
    public void setUp() {
        controller = new SampleEntryTestsForTypeProviderRestController(panelService, testSectionService,
                samplePanelService, panelItemService, typeOfSampleService, userService, roleService, programService,
                testMethodService, testQcThresholdDAO, testService, typeOfSampleTestService);
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(17);
        when(request.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(userSessionData);
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

    /**
     * OGC-781 FR-6: the picker never offers a deactivated program, and with the
     * order's domain given it offers only that domain's programs.
     */
    @Test
    public void userProgramsSkipDeactivatedProgramsAndFilterByOrderDomain() throws Exception {
        Program clinical = program("8", "MICROBIOLOGY", "CLINICAL", "Y");
        Program retiredEnvironmental = program("9", "OLD_WATER", "ENVIRONMENTAL", "N");
        Program environmental = program("10", "WATER", "ENVIRONMENTAL", "Y");
        when(userService.getUserPrograms("17", Constants.ROLE_RECEPTION))
                .thenReturn(List.of(new IdValuePair("8", "Microbiology"), new IdValuePair("9", "Old water"),
                        new IdValuePair("10", "Water")));
        when(programService.get("8")).thenReturn(clinical);
        when(programService.get("9")).thenReturn(retiredEnvironmental);
        when(programService.get("10")).thenReturn(environmental);

        List<SampleEntryTestsForTypeProviderRestController.ProgramOption> anyDomain = controller
                .getUserSPrograms(request, null);
        assertEquals(List.of("8", "10"), anyDomain.stream().map(option -> option.getId()).toList());

        List<SampleEntryTestsForTypeProviderRestController.ProgramOption> environmentalOnly = controller
                .getUserSPrograms(request, null, "ENVIRONMENTAL");
        assertEquals(1, environmentalOnly.size());
        assertEquals("10", environmentalOnly.get(0).getId());
        assertEquals("ENVIRONMENTAL", environmentalOnly.get(0).getDomain());

        List<SampleEntryTestsForTypeProviderRestController.ProgramOption> legacyCode = controller
                .getUserSPrograms(request, null, "E");
        assertEquals(List.of("10"), legacyCode.stream().map(option -> option.getId()).toList());
    }

    private static Program program(String id, String code, String domain, String isActive) {
        Program program = new Program();
        program.setId(id);
        program.setCode(code);
        program.setProgramName(code);
        program.setDomain(domain);
        program.setIsActive(isActive);
        return program;
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
        SampleEntryTestsForTypeProviderRestController.SampleEntryTests result = (SampleEntryTestsForTypeProviderRestController.SampleEntryTests) controller
                .processRequest(request, null).getBody();

        assertEquals("5", result.getSampleTypeId());
        assertEquals("7", result.getTests().get(0).getMethods().get(0).methodId);
        assertEquals("Blood Culture Standard", result.getTests().get(0).getMethods().get(0).methodName);
    }

}
