package org.openelisglobal.result.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.result.form.LogbookResultsForm;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * Direct-invocation integration tests for
 * {@link LogbookResultsRestController#showRestLogbookResults}.
 *
 * <p>
 * These tests call the controller method directly (not via MockMvc) because the
 * controller is excluded from the test component-scan
 * ({@code org.openelisglobal.result.controller.*} in {@code AppTestConfig}).
 * The constructor is {@code private}, and using {@code @Import} or
 * {@code @TestConfiguration} changes the context fingerprint, which triggers an
 * unrelated {@code NoClassDefFoundError} for {@code AdminOptionMenuForm}.
 * Reflection is therefore the most practical instantiation strategy.
 *
 * <p>
 * Real Spring beans from the shared test context are injected for all
 * collaborators except {@link UserService} (stubbed — its
 * {@code filterResultsByLabUnitRoles} requires a Spring Security principal that
 * the test context does not provide) and {@code NotificationDAO} (mocked —
 * {@code org.openelisglobal.notifications} is not component-scanned).
 */
@Rollback
public class LogbookResultsRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private org.openelisglobal.test.service.TestSectionService testSectionService;
    @Autowired
    private org.openelisglobal.result.service.LogbookResultsPersistService logbookPersistService;
    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;
    @Autowired
    private org.openelisglobal.sample.service.SampleService sampleService;
    @Autowired
    private org.openelisglobal.patient.service.PatientService patientService;
    @Autowired
    private org.openelisglobal.search.service.SearchResultsService searchResultsService;
    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItemService;
    @Autowired
    private org.openelisglobal.userrole.service.UserRoleService userRoleService;
    @Autowired
    private org.openelisglobal.samplehuman.service.SampleHumanService sampleHumanService;
    @Autowired
    private org.openelisglobal.systemuser.service.SystemUserService systemUserService;
    @Autowired
    private org.openelisglobal.referral.service.ReferralTypeService referralTypeService;
    @Autowired
    private UserModuleService userModuleService;
    @Autowired
    private PageBuilderService pageBuilderService;

    private org.openelisglobal.notifications.dao.NotificationDAO notificationDAO;
    private LogbookResultsRestController controller;
    private UserService userServiceMock;
    private MockHttpServletRequest mockRequest;
    private UserSessionData userSessionData;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/logbook-db.xml");

        Constructor<LogbookResultsRestController> ctor = LogbookResultsRestController.class
                .getDeclaredConstructor(org.openelisglobal.referral.service.ReferralTypeService.class);
        ctor.setAccessible(true);
        controller = ctor.newInstance(referralTypeService);

        ReflectionTestUtils.setField(controller, "testSectionService", testSectionService);
        ReflectionTestUtils.setField(controller, "logbookPersistService", logbookPersistService);
        ReflectionTestUtils.setField(controller, "analysisService", analysisService);
        ReflectionTestUtils.setField(controller, "sampleService", sampleService);
        ReflectionTestUtils.setField(controller, "patientService", patientService);
        ReflectionTestUtils.setField(controller, "searchService", searchResultsService);
        ReflectionTestUtils.setField(controller, "sampleItemService", sampleItemService);
        ReflectionTestUtils.setField(controller, "userRoleService", userRoleService);
        ReflectionTestUtils.setField(controller, "sampleHumanService", sampleHumanService);
        ReflectionTestUtils.setField(controller, "systemUserService", systemUserService);

        notificationDAO = Mockito.mock(org.openelisglobal.notifications.dao.NotificationDAO.class);
        ReflectionTestUtils.setField(controller, "notificationDAO", notificationDAO);

        org.openelisglobal.dataexchange.fhir.service.FhirTransformService fhirTransformService = Mockito
                .mock(org.openelisglobal.dataexchange.fhir.service.FhirTransformService.class);
        ReflectionTestUtils.setField(controller, "fhirTransformService", fhirTransformService);

        userServiceMock = Mockito.mock(UserService.class);
        when(userServiceMock.filterResultsByLabUnitRoles(anyString(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        ReflectionTestUtils.setField(controller, "userService", userServiceMock);

        ReflectionTestUtils.setField(controller, "userModuleService", userModuleService);
        ReflectionTestUtils.setField(controller, "pageBuilderService", pageBuilderService);

        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);

        mockRequest = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        mockRequest.setSession(session);
        ReflectionTestUtils.setField(controller, "request", mockRequest);
    }

    @Test
    public void showRestLogbookResults_withLabNumber_setsAccessionAndSearchFinished() throws Exception {
        LogbookResultsForm form = new LogbookResultsForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        LogbookResultsForm result = controller.showRestLogbookResults("S-TEST-001", null, null, null, null, null, null,
                null, false, false, form, bindingResult);

        assertFalse("No binding errors expected for valid input", bindingResult.hasErrors());
        assertNotNull("Response form should not be null", result);
        assertEquals("accessionNumber should match labNumber param", "S-TEST-001", result.getAccessionNumber());
        assertTrue("searchFinished should be true after accession lookup", result.isSearchFinished());
        assertNotNull("currentDate should be populated", result.getCurrentDate());
    }

    @Test
    public void showRestLogbookResults_withUnknownAccession_returnsEmptyTestResult() throws Exception {
        LogbookResultsForm form = new LogbookResultsForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        LogbookResultsForm result = controller.showRestLogbookResults("NO-SUCH-ACCESSION-99999", null, null, null, null,
                null, null, null, false, false, form, bindingResult);

        assertFalse("No binding errors expected for valid input", bindingResult.hasErrors());
        assertNotNull("Response form should not be null", result);
        assertEquals("accessionNumber should reflect the searched value", "NO-SUCH-ACCESSION-99999",
                result.getAccessionNumber());
        assertTrue("testResult should be null or empty for non-existent accession",
                result.getTestResult() == null || result.getTestResult().isEmpty());
    }

    @Test
    public void showRestLogbookResults_withDateFilters_returnsFormWithDisplayDefaults() throws Exception {
        LogbookResultsForm form = new LogbookResultsForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        LogbookResultsForm result = controller.showRestLogbookResults(null, null, "01/01/2025", "01/01/2025", null,
                null, null, null, false, false, form, bindingResult);

        assertFalse("No binding errors expected for valid date input", bindingResult.hasErrors());
        assertNotNull("Response form should not be null", result);
        assertTrue("displayTestSections should be true", result.getDisplayTestSections());
        assertFalse("searchByRange should be false when doRange is not set", result.getSearchByRange());
    }

    @Test
    public void showRestLogbookResults_withNoSearchParams_returnsFormWithDefaults() throws Exception {
        LogbookResultsForm form = new LogbookResultsForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        LogbookResultsForm result = controller.showRestLogbookResults(null, null, null, null, null, null, null, null,
                false, false, form, bindingResult);

        assertFalse("No binding errors expected for empty search", bindingResult.hasErrors());
        assertNotNull("Response form should not be null", result);
        assertTrue("displayTestSections should be true", result.getDisplayTestSections());
        assertFalse("searchByRange should be false by default", result.getSearchByRange());
        assertNotNull("hivKits list should be initialized", result.getHivKits());
        assertNotNull("syphilisKits list should be initialized", result.getSyphilisKits());
    }

    @Test
    public void showRestLogbookResults_withDoRange_setsAccessionAndSearchFinished() throws Exception {
        LogbookResultsForm form = new LogbookResultsForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        LogbookResultsForm result = controller.showRestLogbookResults("S-TEST-001", null, null, null, null, null, null,
                "S-TEST-002", true, false, form, bindingResult);

        assertFalse("No binding errors expected for valid range input", bindingResult.hasErrors());
        assertNotNull("Response form should not be null", result);
        assertEquals("accessionNumber should be set to lower range bound", "S-TEST-001", result.getAccessionNumber());
        assertTrue("searchFinished should be true after range search", result.isSearchFinished());
    }

    @Test
    public void showRestLogbookResults_withFinishedFlag_setsAccessionAndSearchFinished() throws Exception {
        LogbookResultsForm form = new LogbookResultsForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        LogbookResultsForm result = controller.showRestLogbookResults("S-TEST-001", null, null, null, null, null, null,
                "S-TEST-002", true, true, form, bindingResult);

        assertFalse("No binding errors expected for valid finished input", bindingResult.hasErrors());
        assertNotNull("Response form should not be null", result);
        assertEquals("accessionNumber should be set to lower range bound", "S-TEST-001", result.getAccessionNumber());
        assertTrue("searchFinished should be true after finished search", result.isSearchFinished());
    }
}
