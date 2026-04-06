package org.openelisglobal.workplan.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkPlanByTestControllerTest extends BaseWebContextSensitiveTest {

    private static final String TEST_DATA_FILE = "testdata/workplan-by-test-controller.xml";
    private static final String VALID_TEST_ID = "70001";
    private static final String INVALID_TEST_ID = "-1";

    @Autowired
    private UserService realUserService;

    private WorkPlanByTestController controller;
    private UserService mockUserService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        controller = webApplicationContext.getBean(WorkPlanByTestController.class);

        executeDataSetWithStateManagement(TEST_DATA_FILE);

        // Reset static statusList so initialize() re-runs with fresh DB data
        resetStatusList();

        // Trigger re-initialization
        java.lang.reflect.Method initMethod = BaseWorkplanController.class.getDeclaredMethod("initialize");
        initMethod.setAccessible(true);
        initMethod.invoke(controller);

        // Mock filterResultsByLabUnitRoles to return results unchanged
        mockUserService = mock(UserService.class);
        doAnswer(invocation -> invocation.getArgument(1)).when(mockUserService).filterResultsByLabUnitRoles(anyString(),
                anyList(), anyString());
        when(mockUserService.getAllDisplayUserTestsByLabUnit(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        injectField(controller, WorkPlanByTestController.class, "userService", mockUserService);
    }

    @After
    public void tearDown() throws Exception {
        injectField(controller, WorkPlanByTestController.class, "userService", realUserService);
        resetStatusList();
    }

    private void resetStatusList() throws Exception {
        Field statusListField = BaseWorkplanController.class.getDeclaredField("statusList");
        statusListField.setAccessible(true);
        statusListField.set(null, null);

        Field nfsTestIdListField = BaseWorkplanController.class.getDeclaredField("nfsTestIdList");
        nfsTestIdListField.setAccessible(true);
        nfsTestIdListField.set(null, null);
    }

    private void injectField(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * TEST: Valid test ID returns workplan with analyses. EXPECTED: Non-empty
     * workplan with analyses sorted by accession number.
     */
    @Test
    public void testShowWorkPlanByTest_withValidTestId_returnsAnalyses() throws Exception {
        MvcResult result = mockMvc.perform(get("/WorkPlanByTest").param("selectedSearchID", VALID_TEST_ID)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createSessionData())
                .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, createSecurityContext()))
                .andExpect(status().isOk()).andReturn();

        WorkplanForm form = (WorkplanForm) result.getModelAndView().getModel().get("form");
        assertNotNull("Form should not be null", form);
        assertTrue("Search should be marked finished", form.getSearchFinished());
        assertNotNull("Workplan tests should not be null", form.getWorkplanTests());
        assertFalse("Workplan should not be empty for valid test ID", form.getWorkplanTests().isEmpty());
    }

    /**
     * TEST: Workplan items have correct accession numbers sorted in order.
     */
    @Test
    public void testShowWorkPlanByTest_resultsAreSortedByAccession() throws Exception {
        MvcResult result = mockMvc.perform(get("/WorkPlanByTest").param("selectedSearchID", VALID_TEST_ID)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createSessionData())
                .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, createSecurityContext()))
                .andExpect(status().isOk()).andReturn();

        WorkplanForm form = (WorkplanForm) result.getModelAndView().getModel().get("form");
        List<TestResultItem> tests = form.getWorkplanTests();

        assertTrue("Should have at least 2 analyses", tests.size() >= 2);
        assertEquals("First accession should be 33000000001", "33000000001", tests.get(0).getAccessionNumber());
        assertEquals("Second accession should be 33000000002", "33000000002", tests.get(1).getAccessionNumber());
    }

    /**
     * TEST: Empty/blank test ID returns empty workplan without error.
     */
    @Test
    public void testShowWorkPlanByTest_withNoTestId_returnsEmptyWorkplan() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/WorkPlanByTest").sessionAttr(IActionConstants.USER_SESSION_DATA, createSessionData()).sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, createSecurityContext()))
                .andExpect(status().isOk()).andReturn();

        WorkplanForm form = (WorkplanForm) result.getModelAndView().getModel().get("form");
        assertNotNull("Form should not be null", form);
        assertFalse("Search should not be marked finished", form.getSearchFinished());
        assertTrue("Workplan should be empty when no test selected", form.getWorkplanTests().isEmpty());
    }

    /**
     * TEST: Invalid test ID returns empty workplan without error.
     */
    @Test
    public void testShowWorkPlanByTest_withInvalidTestId_returnsEmptyWorkplan() throws Exception {
        MvcResult result = mockMvc.perform(get("/WorkPlanByTest").param("selectedSearchID", INVALID_TEST_ID)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createSessionData())
                .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, createSecurityContext()))
                .andExpect(status().isOk()).andReturn();

        WorkplanForm form = (WorkplanForm) result.getModelAndView().getModel().get("form");
        assertNotNull("Form should not be null", form);
        assertTrue("Workplan should be empty for invalid test ID",
                form.getWorkplanTests() == null || form.getWorkplanTests().isEmpty());
    }

    /**
     * TEST: Workplan items grouped correctly by accession number.
     */
    @Test
    public void testShowWorkPlanByTest_itemsGroupedByAccessionNumber() throws Exception {
        MvcResult result = mockMvc.perform(get("/WorkPlanByTest").param("selectedSearchID", VALID_TEST_ID)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createSessionData())
                .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, createSecurityContext()))
                .andExpect(status().isOk()).andReturn();

        WorkplanForm form = (WorkplanForm) result.getModelAndView().getModel().get("form");
        List<TestResultItem> tests = form.getWorkplanTests();

        assertTrue("Should have analyses", tests.size() > 0);
        for (int i = 1; i < tests.size(); i++) {
            TestResultItem current = tests.get(i);
            TestResultItem previous = tests.get(i - 1);
            if (current.getAccessionNumber().equals(previous.getAccessionNumber())) {
                assertEquals("Same accession should have same grouping number", previous.getSampleGroupingNumber(),
                        current.getSampleGroupingNumber());
            } else {
                assertTrue("Different accessions should have different grouping numbers",
                        current.getSampleGroupingNumber() != previous.getSampleGroupingNumber());
            }
        }
    }

    /**
     * TEST: Workplan items have required non-null fields.
     */
    @Test
    public void testShowWorkPlanByTest_itemsHaveRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/WorkPlanByTest").param("selectedSearchID", VALID_TEST_ID)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createSessionData())
                .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, createSecurityContext()))
                .andExpect(status().isOk()).andReturn();

        WorkplanForm form = (WorkplanForm) result.getModelAndView().getModel().get("form");
        List<TestResultItem> tests = form.getWorkplanTests();

        assertFalse("Should have test items", tests.isEmpty());
        for (TestResultItem item : tests) {
            assertNotNull("Accession number must exist", item.getAccessionNumber());
            assertNotNull("Test ID must exist", item.getTestId());
            assertNotNull("Received date must exist", item.getReceivedDate());
        }
    }

    /**
     * TEST: Form type is set to "test" for WorkPlanByTest.
     */
    @Test
    public void testShowWorkPlanByTest_formTypeIsTest() throws Exception {
        MvcResult result = mockMvc.perform(get("/WorkPlanByTest").param("selectedSearchID", VALID_TEST_ID)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createSessionData())
                .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, createSecurityContext()))
                .andExpect(status().isOk()).andReturn();

        WorkplanForm form = (WorkplanForm) result.getModelAndView().getModel().get("form");
        assertEquals("Form type should be 'test'", "test", form.getType());
    }

    private UserSessionData createSessionData() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        return sessionData;
    }

    private SecurityContext createSecurityContext() {
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", "password", Collections.emptyList()));
        return securityContext;
    }
}