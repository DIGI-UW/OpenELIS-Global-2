package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.validator.SampleEditFormValidator;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;

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

    /**
     * OGC-1376: a Modify Order screen opened before someone else saved the order
     * used to save everything it showed, silently undoing that change.
     */
    @Test
    public void sampleEdit_loadCarriesTheSampleVersion() throws Exception {
        SampleEditForm form = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);

        assertEquals(
                StaleSaveGuard.token(sampleService.getSampleByAccessionNumber(PATIENTLESS_ACCESSION).getLastupdated()),
                form.getSampleLastupdated());
    }

    @Test
    public void sampleEdit_aSaveFromAStaleScreenIsRefusedAndChangesNothing() throws Exception {
        SampleEditForm form = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);
        OrderPriority before = sampleService.getSampleByAccessionNumber(PATIENTLESS_ACCESSION).getPriority();
        form.getSampleOrderItems()
                .setPriority(OrderPriority.STAT.equals(before) ? OrderPriority.ROUTINE : OrderPriority.STAT);
        form.setSampleLastupdated(String.valueOf(Long.parseLong(form.getSampleLastupdated()) - 60_000));

        ResponseEntity<?> response = controller.saveSampleEdit(request, form,
                new BeanPropertyBindingResult(form, "form"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("error.order.staleSave", ((Map<?, ?>) response.getBody()).get("messageKey"));
        assertEquals(before, sampleService.getSampleByAccessionNumber(PATIENTLESS_ACCESSION).getPriority());
    }

    @Test
    public void sampleEdit_aSaveFromTheCurrentVersionIsNotRefused() throws Exception {
        ReflectionTestUtils.setField(controller, "formValidator", Mockito.mock(SampleEditFormValidator.class));
        SampleEditForm form = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);

        ResponseEntity<?> response = controller.saveSampleEdit(request, form,
                new BeanPropertyBindingResult(form, "form"));

        assertEquals(200, response.getStatusCode().value());
    }

    /**
     * Review of #4478: the notebook saves one sample at a time through SampleEdit.
     * Reloading the form after each save, as it now does, carries the version the
     * previous save moved to, so the next save is not refused.
     */
    @Test
    public void sampleEdit_aScreenThatReloadsAfterEachSaveIsNeverRefused() throws Exception {
        ReflectionTestUtils.setField(controller, "formValidator", Mockito.mock(SampleEditFormValidator.class));
        SampleEditForm first = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);
        assertEquals(200, controller.saveSampleEdit(request, first, new BeanPropertyBindingResult(first, "form"))
                .getStatusCode().value());

        SampleEditForm reloaded = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);

        assertEquals(200, controller.saveSampleEdit(request, reloaded, new BeanPropertyBindingResult(reloaded, "form"))
                .getStatusCode().value());
    }

    /**
     * A form the validator refuses, such as one naming a requester that does not
     * exist, used to be saved anyway and then failed with a bare HTTP 500, or
     * answered 200 having ignored the bad value (OGC-1543 walk).
     */
    @Test
    public void sampleEdit_aRefusedFormAnswers400AndChangesNothing() throws Exception {
        SampleEditFormValidator refusing = Mockito.mock(SampleEditFormValidator.class);
        Mockito.doAnswer(call -> {
            ((BeanPropertyBindingResult) call.getArgument(1)).rejectValue("sampleOrderItems.providerPersonId",
                    "error.provider.notFound", new Object[] { "9000099" },
                    "The requester with id 9000099 does not exist");
            return null;
        }).when(refusing).validate(Mockito.any(), Mockito.any());
        ReflectionTestUtils.setField(controller, "formValidator", refusing);
        SampleEditForm form = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);
        OrderPriority before = sampleService.getSampleByAccessionNumber(PATIENTLESS_ACCESSION).getPriority();
        form.getSampleOrderItems()
                .setPriority(OrderPriority.STAT.equals(before) ? OrderPriority.ROUTINE : OrderPriority.STAT);

        ResponseEntity<?> response = controller.saveSampleEdit(request, form,
                new BeanPropertyBindingResult(form, "form"));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(String.valueOf(((Map<?, ?>) response.getBody()).get("error")).contains("9000099"));
        assertEquals(before, sampleService.getSampleByAccessionNumber(PATIENTLESS_ACCESSION).getPriority());
    }

    /**
     * The refusal names the requester in the user's language (review of #4822): a
     * French session reads the French message, not the English default.
     */
    @Test
    public void sampleEdit_aRefusalReadsInTheUsersLanguage() throws Exception {
        SampleEditFormValidator refusing = Mockito.mock(SampleEditFormValidator.class);
        Mockito.doAnswer(call -> {
            ((BeanPropertyBindingResult) call.getArgument(1)).rejectValue("sampleOrderItems.providerPersonId",
                    "error.provider.notFound", new Object[] { "9000099" },
                    "The requester with id 9000099 does not exist");
            return null;
        }).when(refusing).validate(Mockito.any(), Mockito.any());
        ReflectionTestUtils.setField(controller, "formValidator", refusing);
        SampleEditForm form = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);
        java.util.Locale before = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        org.springframework.context.i18n.LocaleContextHolder.setLocale(java.util.Locale.FRENCH);
        try {
            ResponseEntity<?> response = controller.saveSampleEdit(request, form,
                    new BeanPropertyBindingResult(form, "form"));

            String error = String.valueOf(((Map<?, ?>) response.getBody()).get("error"));
            assertTrue(error, error.contains("Le demandeur avec l'identifiant 9000099 n'existe pas"));
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.setLocale(before);
        }
    }

    /**
     * Normal Modify Order saves have always raised other validator errors (an empty
     * sampleXML, a patient name with a comma), which the save ignores; only an
     * unknown site, department or requester stops it.
     */
    @Test
    public void sampleEdit_otherValidatorErrorsStillSave() throws Exception {
        SampleEditFormValidator noisy = Mockito.mock(SampleEditFormValidator.class);
        Mockito.doAnswer(call -> {
            ((BeanPropertyBindingResult) call.getArgument(1)).reject("batchentry.error.sampleXML.invalid");
            return null;
        }).when(noisy).validate(Mockito.any(), Mockito.any());
        ReflectionTestUtils.setField(controller, "formValidator", noisy);
        SampleEditForm form = controller.showSampleEdit(request, PATIENTLESS_ACCESSION, null);

        ResponseEntity<?> response = controller.saveSampleEdit(request, form,
                new BeanPropertyBindingResult(form, "form"));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    public void sampleEdit_stillReportsAMissingSample() throws Exception {
        SampleEditForm form = controller.showSampleEdit(request, "DASH-9999", null);

        assertTrue(form.getNoSampleFound());
    }
}
