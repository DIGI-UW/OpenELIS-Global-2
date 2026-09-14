package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1192 §2 — a sample that exists but has no patient (an environmental
 * order) must load in SampleEdit like any other sample, with the patient block
 * simply empty, instead of failing with a NullPointerException (HTTP 500).
 *
 * The controller is exercised directly: the test context does not
 * component-scan {@code org.openelisglobal.sample.controller}, and the form's
 * JSON view needs the accession-number generator that only a running
 * application configures.
 */
public class SampleEditPatientlessTest extends BaseWebContextSensitiveTest {

    private static final String PATIENTLESS_ACCESSION = "DASH-0025";

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    private SampleEditRestController controller;
    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/order-dashboard-patientless.xml");
        authenticateAs("testUser");
        PatientlessOrderObservations.create(sampleService.getSampleByAccessionNumber(PATIENTLESS_ACCESSION),
                observationHistoryService, observationHistoryTypeService);
        statusService.refreshCache();
        controller = webApplicationContext.getAutowireCapableBeanFactory().createBean(SampleEditRestController.class);
        pinStatusIdsToThisFixture();
        request = new MockHttpServletRequest();
        request.setSession(buildSession());
    }

    /**
     * The controller caches the "entered" sample status and the "canceled" analysis
     * status in static sets when its class loads, i.e. against whichever fixture an
     * earlier test class had loaded. Point them at this fixture's ids so the test
     * does not depend on class order.
     */
    @SuppressWarnings("unchecked")
    private void pinStatusIdsToThisFixture() {
        Set<String> entered = (Set<String>) ReflectionTestUtils.getField(SampleEditRestController.class,
                "ENTERED_STATUS_SAMPLE_LIST");
        entered.clear();
        entered.add(statusService.getStatusID(SampleStatus.Entered));
        Set<String> excluded = (Set<String>) ReflectionTestUtils.getField(SampleEditRestController.class,
                "excludedAnalysisStatusList");
        excluded.clear();
        excluded.add(statusService.getStatusID(AnalysisStatus.Canceled));
    }

    private MockHttpSession buildSession() {
        UserDetails userDetails = User.withUsername("testUser").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return httpSession;
    }

    @Test
    public void sampleEdit_loadsAPatientlessSample_withAnEmptyPatientBlock() throws Exception {
        SampleEditForm form = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);

        assertEquals(PATIENTLESS_ACCESSION, form.getAccessionNumber());
        assertFalse("an existing sample is not reported as missing", Boolean.TRUE.equals(form.getNoSampleFound()));
        assertEquals(1, form.getExistingTests().size());
        assertEquals("Lead (Pb)", form.getExistingTests().get(0).getTestName());
        assertTrue("no patient name for a patientless sample", GenericValidator.isBlankOrNull(form.getPatientName()));
        assertTrue("no patient id for a patientless sample", GenericValidator.isBlankOrNull(form.getPatientId()));
    }

    @Test
    public void sampleEdit_stillReportsAMissingSample() throws Exception {
        SampleEditForm form = controller.showSampleEdit(request, "DASH-9999", null);

        assertTrue(form.getNoSampleFound());
    }
}
