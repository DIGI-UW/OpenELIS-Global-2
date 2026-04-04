package org.openelisglobal.workplan.controller.rest;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Integration tests for WorkplanByPanelRestController.
 *
 * TEST DATA: testdata/workplan-by-panel-controller.xml ID ranges: 30001-30999
 * (domain), 40001-40999 (tests), 50001-50999 (samples)
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. statusList reset: WorkplanRestController.statusList is static, initialized
 * once at PostConstruct via IStatusService. If the test DB had no
 * status_of_sample rows at startup, getStatusID returns null and parseInt
 * throws, leaving statusList=null permanently. We reset it to null before each
 * test so initialize() re-runs after executeDataSetWithStateManagement +
 * refreshCache() have loaded our fixture rows, guaranteeing statusList gets
 * [1,2,3,4] matching our analysis.status_id=1.
 *
 * 2. filterResultsByLabUnitRoles is mocked to return results unchanged. The
 * real implementation requires Spring Security session auth, DisplayListService
 * cache (not refreshed by fixture), and system_role table contents. Mocking
 * isolates the workplan-building logic under test.
 */
public class WorkplanByPanelRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String TEST_DATA_FILE = "testdata/workplan-by-panel-controller.xml";
    private static final String VALID_PANEL_ID = "30001";
    private static final String EMPTY_PANEL_ID = "30002";
    private static final String INVALID_PANEL_ID = "-1";

    @Autowired
    private UserService realUserService;

    private WorkplanByPanelRestController controller;
    private HttpServletRequest mockRequest;
    private UserService mockUserService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        controller = webApplicationContext.getBean(WorkplanByPanelRestController.class);

        // Load fixture + refreshCache() so status_of_sample ids 1-4 are in DB
        // and IStatusService maps AnalysisStatus.NotStarted -> "1" etc.
        executeDataSetWithStateManagement(TEST_DATA_FILE);

        // Reset static statusList so WorkplanRestController.initialize() re-runs
        // now that the correct status_of_sample rows are loaded.
        // Without this, statusList may be null (if PostConstruct ran against an
        // empty DB) or stale, causing getAllAnalysisByTestAndStatus to return [].
        resetStatusList();

        // Trigger re-initialization by calling initialize() via the controller bean
        // initialize() is @PostConstruct private — invoke it via reflection
        java.lang.reflect.Method initMethod = WorkplanRestController.class.getDeclaredMethod("initialize");
        initMethod.setAccessible(true);
        initMethod.invoke(controller);

        // Mock filterResultsByLabUnitRoles to return results unchanged
        mockUserService = mock(UserService.class);
        doAnswer(invocation -> invocation.getArgument(1)).when(mockUserService).filterResultsByLabUnitRoles(anyString(),
                anyList(), anyString());
        injectField(controller, WorkplanByPanelRestController.class, "userService", mockUserService);

        mockRequest = createMockRequest("1");
    }

    @After
    public void tearDown() throws Exception {
        // Restore real userService and reset statusList so other tests are unaffected
        injectField(controller, WorkplanByPanelRestController.class, "userService", realUserService);
        resetStatusList();
    }

    /**
     * Resets WorkplanRestController.statusList to null so initialize() will
     * re-populate it from the current state of IStatusService on next call.
     */
    private void resetStatusList() throws Exception {
        Field statusListField = WorkplanRestController.class.getDeclaredField("statusList");
        statusListField.setAccessible(true);
        statusListField.set(null, null);
    }

    private void injectField(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * TEST: Valid panel returns workplan with analyses. EXPECTED: 2 analyses sorted
     * by accession number.
     */
    @Test
    public void testShowWorkPlanByPanelWithValidPanelReturnsAnalyses() throws Exception {
        WorkplanForm form = controller.showWorkPlanByPanel(mockRequest, VALID_PANEL_ID);

        assertNotNull("Workplan form should not be null", form);
        assertNotNull("Test list should exist", form.getWorkplanTests());
        assertFalse("Test list should not be empty for Chemistry Panel", form.getWorkplanTests().isEmpty());

        List<TestResultItem> tests = form.getWorkplanTests();
        assertEquals("Should have 2 analyses for Chemistry Panel", 2, tests.size());
        assertEquals("First test should have accession 22000000001", "22000000001", tests.get(0).getAccessionNumber());
        assertEquals("Second test should have accession 22000000002", "22000000002", tests.get(1).getAccessionNumber());
    }

    /**
     * TEST: Empty panel returns empty workplan without error.
     */
    @Test
    public void testShowWorkPlanByEmptyPanelReturnsEmptyList() throws Exception {
        WorkplanForm form = controller.showWorkPlanByPanel(mockRequest, EMPTY_PANEL_ID);

        assertNotNull("Workplan form should not be null", form);
        assertNotNull("Test list should exist", form.getWorkplanTests());
        assertTrue("Test list should be empty for Empty Panel", form.getWorkplanTests().isEmpty());
    }

    /**
     * TEST: Items grouped correctly by accession number.
     */
    @Test
    public void testWorkplanItemsGroupedByAccessionNumber() throws Exception {
        WorkplanForm form = controller.showWorkPlanByPanel(mockRequest, VALID_PANEL_ID);
        List<TestResultItem> tests = form.getWorkplanTests();

        assertTrue("Should have analyses", tests.size() > 0);

        for (int i = 1; i < tests.size(); i++) {
            TestResultItem current = tests.get(i);
            TestResultItem previous = tests.get(i - 1);
            if (current.getAccessionNumber().equals(previous.getAccessionNumber())) {
                assertEquals("Items with same accession should have same grouping", previous.getSampleGroupingNumber(),
                        current.getSampleGroupingNumber());
            } else {
                assertTrue("Different accessions should have different grouping numbers",
                        current.getSampleGroupingNumber() != previous.getSampleGroupingNumber());
            }
        }
    }

    /**
     * TEST: Invalid panel ID returns empty list without error.
     */
    @Test
    public void testShowWorkPlanByPanelWithInvalidPanelIdReturnsEmpty() throws Exception {
        WorkplanForm form = controller.showWorkPlanByPanel(mockRequest, INVALID_PANEL_ID);
        assertNotNull("Form should not be null for invalid ID", form);
        assertTrue("Should return empty list for invalid panel ID",
                form.getWorkplanTests() == null || form.getWorkplanTests().isEmpty());
    }

    /**
     * TEST: All returned items have required non-null fields.
     */
    @Test
    public void testWorkplanItemsHaveCompleteData() throws Exception {
        WorkplanForm form = controller.showWorkPlanByPanel(mockRequest, VALID_PANEL_ID);
        List<TestResultItem> tests = form.getWorkplanTests();

        assertFalse("Should have test items", tests.isEmpty());

        for (TestResultItem item : tests) {
            assertNotNull("Accession number must exist", item.getAccessionNumber());
            assertNotNull("Test ID must exist", item.getTestId());
            assertNotNull("Test name must be localized", item.getTestName());
            assertNotNull("Patient info must exist", item.getPatientInfo());
            assertNotNull("Received date must exist", item.getReceivedDate());
        }
    }

    /**
     * TEST: QA status field is accessible on all returned items.
     */
    @Test
    public void testWorkplanItemsHaveQaStatus() throws Exception {
        WorkplanForm form = controller.showWorkPlanByPanel(mockRequest, VALID_PANEL_ID);
        List<TestResultItem> tests = form.getWorkplanTests();

        assertFalse("Should have test items", tests.isEmpty());

        boolean foundConforming = false;
        boolean foundNonConforming = false;
        for (TestResultItem item : tests) {
            if (item.isNonconforming()) {
                foundNonConforming = true;
            } else {
                foundConforming = true;
            }
        }
        assertTrue("Should have at least conforming or non-conforming items", foundConforming || foundNonConforming);
    }

    private HttpServletRequest createMockRequest(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", userId);

        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute("userSessionData", sessionData);

        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", "password", Collections.emptyList()));
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);

        return request;
    }
}