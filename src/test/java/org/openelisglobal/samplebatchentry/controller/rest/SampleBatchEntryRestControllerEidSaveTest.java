package org.openelisglobal.samplebatchentry.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.springserialization.QuestionnaireResponseDeserializer;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.validator.SamplePatientEntryFormValidator;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntrySaveForm;
import org.openelisglobal.samplebatchentry.validator.SampleBatchEntryFormValidator;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/**
 * Batch Order Entry save for the EID study form: the order must carry the
 * specimens and the DNA PCR test the technician ticked, and a save that fails
 * validation must leave nothing behind. The controller is called directly
 * because the test context does not scan controllers.
 */
public class SampleBatchEntryRestControllerEidSaveTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private AccessionNumberValidatorFactory accessionNumberValidatorFactory;

    private static final int LAB_NUMBER_LENGTH = 20;
    private static final ObjectMapper CLIENT_JSON = new ObjectMapper().registerModule(
            new SimpleModule().addDeserializer(QuestionnaireResponse.class, new QuestionnaireResponseDeserializer()));

    private SampleBatchEntryRestController controller;
    private MockHttpServletRequest request;

    @Before
    public void setUpController() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-batch-entry-eid.xml");
        org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap.reset();

        request = new MockHttpServletRequest();
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(1);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, session);

        // The test context mocks the accession factory: a lab number is valid when
        // it has the full length, so a partial number is refused as by production.
        IAccessionNumberValidator validator = mock(IAccessionNumberValidator.class);
        when(validator.checkAccessionNumberValidity(any(), any(), any(), any())).thenAnswer(
                call -> ((String) call.getArgument(0)).length() == LAB_NUMBER_LENGTH ? ValidationResults.SUCCESS
                        : ValidationResults.LENGTH_FAIL);
        when(validator.getInvalidMessage(any())).thenReturn("invalid lab number");
        when(accessionNumberValidatorFactory.getValidator(AccessionFormat.GENERAL)).thenReturn(validator);

        controller = new SampleBatchEntryRestController();
        ReflectionTestUtils.setField(controller, "request", request);
        SamplePatientEntryFormValidator entryFormValidator = new SamplePatientEntryFormValidator();
        ReflectionTestUtils.setField(entryFormValidator, "organizationService",
                webApplicationContext.getBean(OrganizationService.class));
        ReflectionTestUtils.setField(controller, "formValidator", new SampleBatchEntryFormValidator());
        ReflectionTestUtils.setField(controller, "entryFormValidator", entryFormValidator);
        ReflectionTestUtils.setField(controller, "testService", webApplicationContext.getBean(TestService.class));
        ReflectionTestUtils.setField(controller, "typeOfSampleService",
                webApplicationContext.getBean(TypeOfSampleService.class));
        ReflectionTestUtils.setField(controller, "organizationService",
                webApplicationContext.getBean(OrganizationService.class));
        ReflectionTestUtils.setField(controller, "samplePatientEntryService",
                webApplicationContext.getBean(SamplePatientEntryService.class));
        ReflectionTestUtils.setField(controller, "fhirTransformService", mock(FhirTransformService.class));
    }

    @After
    public void resetAccessionFactory() {
        reset(accessionNumberValidatorFactory);
    }

    /** The JSON the batch screen posts, with the EID form's flags. */
    private SampleBatchEntrySaveForm eidForm(String labNo, String dryTube, String dbs, String dnaPcr) throws Exception {
        String today = DateUtil.getCurrentDateAsText();
        String json = "{\"currentDate\":\"" + today + "\",\"currentTime\":\"10:00\",\"method\":\"Pre-Printed\","
                + "\"sampleXML\":\"\",\"tests\":[{\"value\":\"DNA PCR\",\"id\":\"eid_dnaPCR\"}],"
                + "\"_ProjectDataEID\":{\"dryTubeTaken\":" + dryTube + ",\"dbsTaken\":" + dbs + ",\"dnaPCR\":" + dnaPcr
                + "},\"patientProperties\":{\"patientPK\":\"1\",\"patientUpdateStatus\":\"NO_ACTION\"},"
                + "\"sampleOrderItems\":{\"labNo\":\"" + labNo + "\",\"receivedDateForDisplay\":\"" + today
                + "\",\"receivedTime\":\"10:00\"}}";
        return CLIENT_JSON.readValue(json, SampleBatchEntrySaveForm.class);
    }

    private ResponseEntity<?> save(SampleBatchEntrySaveForm form) throws Exception {
        BindingResult result = new BeanPropertyBindingResult(form, "form");
        return controller.showSamplePatientEntrySave(request, form, result, new RedirectAttributesModelMap());
    }

    private List<String> specimenTypes(Sample sample) {
        return sampleItemService.getSampleItemsBySampleId(sample.getId()).stream()
                .map(item -> item.getTypeOfSample().getDescription()).sorted().collect(Collectors.toList());
    }

    private List<String> analysedTests(Sample sample) {
        return analysisService.getAnalysesBySampleId(sample.getId()).stream()
                .map(analysis -> analysis.getTest().getDescription()).collect(Collectors.toList());
    }

    @Test
    public void aTickedSpecimenAndDnaPcrAreSavedOnTheOrder() throws Exception {
        String labNo = "DEV01260000000000171";

        ResponseEntity<?> response = save(eidForm(labNo, "true", "\"\"", "true"));

        assertEquals(200, response.getStatusCode().value());
        Sample saved = sampleService.getSampleByAccessionNumber(labNo);
        assertNotNull("the order is saved under the lab number", saved);
        assertEquals(List.of("Dry Tube"), specimenTypes(saved));
        assertEquals(List.of("DNA PCR"), analysedTests(saved));
    }

    @Test
    public void everyTickedSpecimenCarriesTheDnaPcrTest() throws Exception {
        String labNo = "DEV01260000000000172";

        ResponseEntity<?> response = save(eidForm(labNo, "true", "true", "true"));

        assertEquals(200, response.getStatusCode().value());
        Sample saved = sampleService.getSampleByAccessionNumber(labNo);
        assertNotNull(saved);
        assertEquals(List.of("DBS", "Dry Tube"), specimenTypes(saved));
        assertEquals(List.of("DNA PCR", "DNA PCR"), analysedTests(saved));
    }

    @Test
    public void aTickedSpecimenWithoutTheTestIsRefusedAndNothingIsSaved() throws Exception {
        String labNo = "DEV01260000000000173";

        ResponseEntity<?> response = save(eidForm(labNo, "true", "\"\"", "false"));

        assertEquals(400, response.getStatusCode().value());
        assertNull(sampleService.getSampleByAccessionNumber(labNo));
    }

    @Test
    public void aLabNumberThatIsNotAValidAccessionNumberIsRefusedAndNothingIsSaved() throws Exception {
        String partial = "DEV01";

        ResponseEntity<?> response = save(eidForm(partial, "true", "\"\"", "true"));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(String.valueOf(response.getBody()).contains("labNo"));
        assertNull(sampleService.getSampleByAccessionNumber(partial));
    }
}
