package org.openelisglobal.sample.controller.rest;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

public class SamplePatientEntryRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ENDPOINT = "/rest/SamplePatientEntry";

    private static final IdValuePair IMMUNOLOGY = new IdValuePair("1", "Immunology");
    private static final IdValuePair BROKEN_CONTAINER = new IdValuePair("7", "Broken container");
    private static final IdValuePair TEST_ORG = new IdValuePair("1", "Test Org");
    private static final IdValuePair WRONG_TEST = new IdValuePair("3", "Wrong test ordered");

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private org.openelisglobal.common.services.IStatusService statusService;

    private static final AtomicInteger PATIENT_SEQUENCE = new AtomicInteger();

    private JdbcTemplate jdbc;
    private MockHttpSession session;
    private String today;
    private AlphanumAccessionValidator accessionNumbers;
    private AccessionNumberValidatorFactory accessionFactory;
    private DisplayListService displayLists;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        accessionNumbers = new AlphanumAccessionValidator();

        accessionFactory = webApplicationContext.getBean(AccessionNumberValidatorFactory.class);

        when(accessionFactory.getValidator(any(AccessionFormat.class))).thenReturn(accessionNumbers);

        when(accessionFactory.getGenerator(any(AccessionFormat.class))).thenReturn(accessionNumbers);

        displayLists = webApplicationContext.getBean(DisplayListService.class);

        when(displayLists.getList(ListType.TEST_SECTION_ACTIVE)).thenReturn(List.of(IMMUNOLOGY));

        when(displayLists.getList(ListType.REJECTION_REASONS)).thenReturn(List.of(BROKEN_CONTAINER));

        when(displayLists.getList(ListType.REFERRAL_ORGANIZATIONS)).thenReturn(List.of(TEST_ORG));

        when(displayLists.getList(ListType.REFERRAL_REASONS)).thenReturn(List.of(WRONG_TEST));

        executeDataSetWithStateManagement("testdata/samplepatiententry-rest.xml");

        jdbc = new JdbcTemplate(dataSource);

        ensureOrderStatuses();
        cleanRowsInCurrentConnection(new String[] { "sample" });

        today = DateUtil.getCurrentDateAsText();
        session = authenticatedSession();
    }

    @After
    public void tearDown() {
        reset(accessionFactory, displayLists);
    }

    @Test
    public void get_returnsAnEmptyOrderStampedWithTodaysDates() throws Exception {
        MvcResult result = mockMvc.perform(get(ENDPOINT).session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentDate").value(today))
                .andExpect(jsonPath("$.sampleOrderItems.receivedDateForDisplay").value(today))
                .andExpect(jsonPath("$.sampleOrderItems.requestDate").value(today)).andReturn();

        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());

        assertTrue("GET response must contain sampleOrderItems", body.has("sampleOrderItems"));

        assertTrue("GET response must contain patientProperties", body.has("patientProperties"));

        assertEquals("A new order must not already have a laboratory accession number", 0,
                body.path("sampleOrderItems").path("labNo").asText("").length());

        assertEquals("A new order must not already contain a patient name", 0,
                body.path("patientProperties").path("lastName").asText("").length());
    }

    @Test
    public void get_returnsTheConfiguredPickLists() throws Exception {
        MvcResult result = mockMvc.perform(get(ENDPOINT).session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.testSectionList.length()").value(1))
                .andExpect(jsonPath("$.testSectionList[0].id").value(IMMUNOLOGY.getId()))
                .andExpect(jsonPath("$.testSectionList[0].value").value(IMMUNOLOGY.getValue()))
                .andExpect(jsonPath("$.rejectReasonList.length()").value(1))
                .andExpect(jsonPath("$.rejectReasonList[0].value").value(BROKEN_CONTAINER.getValue()))
                .andExpect(jsonPath("$.referralOrganizations.length()").value(1))
                .andExpect(jsonPath("$.referralOrganizations[0].value").value(TEST_ORG.getValue()))
                .andExpect(jsonPath("$.referralReasons.length()").value(1))
                .andExpect(jsonPath("$.referralReasons[0].value").value(WRONG_TEST.getValue())).andReturn();

        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());

        assertEquals("The configured test section must be returned exactly once", IMMUNOLOGY.getId(),
                body.path("testSectionList").get(0).path("id").asText());

        assertEquals("The configured referral organization must be returned exactly once", TEST_ORG.getValue(),
                body.path("referralOrganizations").get(0).path("value").asText());
    }

    @Test
    public void get_returnsOnlyTheSampleTypesTheReceptionUserMayOrder() throws Exception {
        MvcResult result = mockMvc.perform(get(ENDPOINT).session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sampleTypes.length()").value(1))
                .andExpect(jsonPath("$.sampleTypes[0].id").value("1"))
                .andExpect(jsonPath("$.sampleTypes[0].value").value("Serum")).andReturn();

        JsonNode sampleTypes = JSON.readTree(result.getResponse().getContentAsString()).path("sampleTypes");

        assertEquals("Reception user must receive exactly one orderable sample type", 1, sampleTypes.size());

        assertEquals("The only configured sample type must be Serum", "Serum",
                sampleTypes.get(0).path("value").asText());
    }

    @Test
    public void get_disablesSaveOnTheSessionUntilTheFormIsSubmitted() throws Exception {
        mockMvc.perform(get(ENDPOINT).session(session)).andExpect(status().isOk());

        assertEquals("GET must park the form in save-disabled state on the session", IActionConstants.TRUE,
                session.getAttribute(IActionConstants.SAVE_DISABLED));
    }

    @Test
    public void get_withExternalOrderNumber_echoesItOntoTheOrder() throws Exception {
        String externalOrderNumber = "eOrder-1234";

        MvcResult result = mockMvc
                .perform(get(ENDPOINT).session(session).param(IActionConstants.ID, externalOrderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleOrderItems.externalOrderNumber").value(externalOrderNumber)).andReturn();

        assertEquals("The requested external order number must survive request processing", externalOrderNumber,
                JSON.readTree(result.getResponse().getContentAsString()).path("sampleOrderItems")
                        .path("externalOrderNumber").asText());
    }

    @Test
    public void get_withoutExternalOrderNumber_leavesItUnset() throws Exception {
        MvcResult result = mockMvc.perform(get(ENDPOINT).session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode order = JSON.readTree(result.getResponse().getContentAsString()).path("sampleOrderItems");

        assertEquals("A new order must not be assigned an external order number", "",
                order.path("externalOrderNumber").asText(""));
    }

    @Test
    public void get_reappliesTheFlashScopedProviderAndReferringSite() throws Exception {
        MvcResult result = mockMvc
                .perform(get(ENDPOINT).session(session).flashAttr("sampleOrderItems.providerPersonId", "9005")
                        .flashAttr("sampleOrderItems.providerEmail", "doc@example.com")
                        .flashAttr("sampleOrderItems.providerfax", "555-0100")
                        .flashAttr("sampleOrderItems.providerFirstName", "John")
                        .flashAttr("sampleOrderItems.providerLastName", "Doe")
                        .flashAttr("sampleOrderItems.providerWorkPhone", "555-0101")
                        .flashAttr("sampleOrderItems.referringSiteId", "1")
                        .flashAttr("sampleOrderItems.referringSiteName", "Test Org")
                        .flashAttr("sampleOrderItems.referringSiteCode", "TO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sampleOrderItems.providerPersonId").value("9005"))
                .andExpect(jsonPath("$.sampleOrderItems.providerEmail").value("doc@example.com"))
                .andExpect(jsonPath("$.sampleOrderItems.providerFax").value("555-0100"))
                .andExpect(jsonPath("$.sampleOrderItems.providerFirstName").value("John"))
                .andExpect(jsonPath("$.sampleOrderItems.providerLastName").value("Doe"))
                .andExpect(jsonPath("$.sampleOrderItems.providerWorkPhone").value("555-0101"))
                .andExpect(jsonPath("$.sampleOrderItems.referringSiteId").value("1"))
                .andExpect(jsonPath("$.sampleOrderItems.referringSiteName").value("Test Org"))
                .andExpect(jsonPath("$.sampleOrderItems.referringSiteCode").value("TO")).andReturn();

        JsonNode items = JSON.readTree(result.getResponse().getContentAsString()).path("sampleOrderItems");

        assertEquals("9005", items.path("providerPersonId").asText());
        assertEquals("John", items.path("providerFirstName").asText());
        assertEquals("Doe", items.path("providerLastName").asText());
        assertEquals("Test Org", items.path("referringSiteName").asText());
    }

    @Test
    public void post_clinicalOrder_persistsSampleItemAndAnalysis() throws Exception {
        String labNo = nextAccessionNumber();

        MvcResult result = save(clinicalOrder(labNo), 200).andExpect(jsonPath("$.sampleOrderItems.labNo").value(labNo))
                .andReturn();

        String sampleId = sampleIdFor(labNo);

        assertEquals("Exactly one sample must be created for the submitted order", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample WHERE accession_number = ?", Integer.class,
                        labNo));

        assertEquals("Exactly one collected sample item must belong to the saved sample", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample_item WHERE samp_id = ?::numeric",
                        Integer.class, sampleId));

        assertEquals("The sample item must use the requested Serum sample type", "1",
                jdbc.queryForObject("SELECT typeosamp_id::text FROM clinlims.sample_item WHERE samp_id = ?::numeric",
                        String.class, sampleId));

        assertEquals("The requested test must produce exactly one analysis", "1",
                jdbc.queryForObject("SELECT a.test_id::text " + "FROM clinlims.analysis a "
                        + "JOIN clinlims.sample_item si ON a.sampitem_id = si.id " + "WHERE si.samp_id = ?::numeric",
                        String.class, sampleId));

        assertEquals("The response must return the persisted accession number", labNo, JSON
                .readTree(result.getResponse().getContentAsString()).path("sampleOrderItems").path("labNo").asText());
    }

    @Test
    public void post_clinicalOrder_linksTheNewPatientToTheSample() throws Exception {
        long patientsBefore = registeredPatientCount();
        String labNo = nextAccessionNumber();

        ObjectNode order = clinicalOrder(labNo);
        String nationalId = order.get("patientProperties").get("nationalId").asText();

        save(order, 200);

        assertEquals("A clinical order for an unknown patient must register exactly one patient", patientsBefore + 1,
                registeredPatientCount());

        assertEquals("The persisted patient must retain the submitted national ID", nationalId,
                jdbc.queryForObject("SELECT p.national_id " + "FROM clinlims.patient p "
                        + "JOIN clinlims.sample_human sh ON sh.patient_id = p.id " + "WHERE sh.samp_id = ?::numeric",
                        String.class, sampleIdFor(labNo)));

        assertEquals("The sample must be linked to exactly one patient", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample_human " + "WHERE samp_id = ?::numeric",
                        Integer.class, sampleIdFor(labNo)));
    }

    @Test
    public void post_clinicalOrder_forAnExistingPatient_reusesThatPatientRow() throws Exception {
        long patientsBefore = registeredPatientCount();
        String labNo = nextAccessionNumber();

        ObjectNode order = clinicalOrder(labNo);
        ObjectNode patient = (ObjectNode) order.get("patientProperties");

        patient.put("patientPK", "9001");
        patient.put("nationalId", "NID0001");

        MvcResult result = save(order, 200).andReturn();

        assertEquals("The save must reuse the supplied patient key", "9001",
                JSON.readTree(result.getResponse().getContentAsString()).at("/patientProperties/patientPK").asText());

        assertEquals("An existing patient must not result in a duplicate patient row", patientsBefore,
                registeredPatientCount());

        assertEquals("The sample must be linked to the pre-existing patient", "9001",
                jdbc.queryForObject(
                        "SELECT patient_id::text " + "FROM clinlims.sample_human " + "WHERE samp_id = ?::numeric",
                        String.class, sampleIdFor(labNo)));

        assertEquals("The existing patient must retain its national ID", "NID0001", jdbc.queryForObject(
                "SELECT p.national_id " + "FROM clinlims.patient p " + "WHERE p.id = 9001", String.class));
    }

    @Test
    public void post_orderEntryOnly_savesTheOrderWithNoSampleItems() throws Exception {
        String labNo = nextAccessionNumber();

        ObjectNode order = clinicalOrder(labNo);
        order.put("orderEntryOnly", true);
        order.put("sampleXML", emptySampleManifest());

        save(order, 200);

        String sampleId = sampleIdFor(labNo);

        assertEquals("Order-entry-only must create exactly one order", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample " + "WHERE accession_number = ?", Integer.class, labNo));

        assertEquals("Order-entry-only must not create a sample item", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample_item " + "WHERE samp_id = ?::numeric", Integer.class, sampleId));

        assertEquals("Order-entry-only must not create an analysis", Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) " + "FROM clinlims.analysis a "
                        + "JOIN clinlims.sample_item si ON a.sampitem_id = si.id " + "WHERE si.samp_id = ?::numeric",
                        Integer.class, sampleId));
    }

    @Test
    public void post_emptyBody_isRejectedAsBadRequest_notAsAServerError() throws Exception {
        MvcResult result = save("{}", 400)
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'sampleOrderItems')].defaultMessage")
                        .value(hasItem("Sample order is required")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'patientProperties')].defaultMessage")
                        .value(hasItem("Patient properties are required")))
                .andReturn();

        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());

        assertEquals("Validation must produce HTTP 400 rather than a server error", 400,
                result.getResponse().getStatus());

        assertTrue("The response must contain field validation errors", body.has("fieldErrors"));
    }

    @Test
    public void post_blankLabNumber_isRejectedAgainstTheLabNoField() throws Exception {
        ObjectNode order = clinicalOrder("");

        save(order, 400).andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("sampleOrderItems.labNo")));

        assertEquals("A rejected blank accession number must not create a sample", Integer.valueOf(0), sampleCount());
    }

    @Test
    public void post_malformedLabNumber_isRejectedAgainstTheLabNoField() throws Exception {
        ObjectNode order = clinicalOrder("BAD#001");

        save(order, 400).andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("sampleOrderItems.labNo")));

        assertEquals("A malformed accession number must not create a sample", Integer.valueOf(0), sampleCount());
    }

    @Test
    public void post_reusedLabNumber_isRejectedInsteadOfCreatingASecondOrder() throws Exception {
        String labNo = nextAccessionNumber();

        save(clinicalOrder(labNo), 200);

        save(clinicalOrder(labNo), 400)
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("sampleOrderItems.labNo")));

        assertEquals("Reusing an accession number must leave exactly one sample", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample " + "WHERE accession_number = ?",
                        Integer.class, labNo));

        assertEquals("The rejected duplicate order must not create another sample item", Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) " + "FROM clinlims.sample_item si "
                                + "JOIN clinlims.sample s ON s.id = si.samp_id " + "WHERE s.accession_number = ?",
                        Integer.class, labNo));
    }

    @Test
    public void post_clinicalOrderWithoutGender_isRejectedAgainstThePatientForm() throws Exception {
        ObjectNode order = clinicalOrder(nextAccessionNumber());

        ((ObjectNode) order.get("patientProperties")).put("gender", "");

        save(order, 400).andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("patientProperties.gender")));

        assertEquals("A clinical order rejected for missing gender must not create a sample", Integer.valueOf(0),
                sampleCount());
    }

    @Test
    public void post_receivedDateInTheFuture_isRejected() throws Exception {
        ObjectNode order = clinicalOrder(nextAccessionNumber());

        ((ObjectNode) order.get("sampleOrderItems")).put("receivedDateForDisplay", tomorrow());

        save(order, 400).andExpect(
                jsonPath("$.fieldErrors[*].field").value(hasItem("sampleOrderItems.receivedDateForDisplay")));

        assertEquals("An order with a future received date must not create a sample", Integer.valueOf(0),
                sampleCount());
    }

    @Test
    public void post_clinicalOrderWithNoSampleItems_isRejected() throws Exception {
        ObjectNode order = clinicalOrder(nextAccessionNumber());
        order.put("sampleXML", emptySampleManifest());

        save(order, 400).andExpect(jsonPath("$.fieldErrors[?(@.field == 'sampleOrderItems')].defaultMessage")
                .value(hasItem("errors.no.sample")));

        assertEquals("An order with no sample items must not create a sample", Integer.valueOf(0), sampleCount());
    }

    @Test
    public void post_unparseableSampleManifest_isReportedAsAGlobalError() throws Exception {
        ObjectNode order = clinicalOrder(nextAccessionNumber());
        order.put("sampleXML", "this is not xml");

        save(order, 400).andExpect(jsonPath("$.globalErrors").value(hasItem("batchentry.error.sampleXML.invalid")));

        assertEquals("An invalid sample manifest must not create a sample", Integer.valueOf(0), sampleCount());
    }

    @Test
    public void post_environmentalOrderWithNoRequester_reportsTheRequesterError_notPatientErrors() throws Exception {

        ObjectNode order = environmentalOrder(nextAccessionNumber());

        ((ObjectNode) order.get("sampleOrderItems")).remove("referringSiteId");

        MvcResult result = save(order, 400)
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'sampleOrderItems')].defaultMessage")
                        .value(hasItem("errors.requester.org.or.requestor.required")))
                .andReturn();

        String body = result.getResponse().getContentAsString();

        assertTrue("The requester validation error must be returned",
                body.contains("errors.requester.org.or.requestor.required"));

        assertFalse("An environmental order must not produce patient validation errors",
                body.contains("patientProperties"));

        assertEquals("A rejected environmental order must not create a sample", Integer.valueOf(0), sampleCount());
    }

    @Test
    public void post_withoutRememberSiteAndRequester_stillSaves() throws Exception {
        String labNo = nextAccessionNumber();

        ObjectNode order = clinicalOrder(labNo);

        ((ObjectNode) order.get("sampleOrderItems")).put("providerPersonId", "9005");

        MvcResult result = save(order, 200).andReturn();

        assertEquals("Omitting the optional remember flag must not prevent persistence", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample " + "WHERE accession_number = ?",
                        Integer.class, labNo));

        assertEquals("The successful response must return the persisted accession number", labNo, JSON
                .readTree(result.getResponse().getContentAsString()).path("sampleOrderItems").path("labNo").asText());
    }

    @Test
    public void post_orderCarryingUnknownProperties_stillSaves() throws Exception {
        String labNo = nextAccessionNumber();

        ObjectNode order = clinicalOrder(labNo);
        order.put("someFrontendOnlyFlag", "whatever");

        MvcResult result = save(order, 200).andReturn();

        JsonNode response = JSON.readTree(result.getResponse().getContentAsString());

        assertFalse("Unknown frontend-only data must not be exposed by the API response",
                response.fieldNames().hasNext() && response.has("someFrontendOnlyFlag"));

        assertEquals("The order must still be persisted", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample " + "WHERE accession_number = ?", Integer.class, labNo));

        assertEquals("The persisted order must retain the submitted accession number", labNo, jdbc.queryForObject(
                "SELECT accession_number FROM clinlims.sample " + "WHERE accession_number = ?", String.class, labNo));
    }

    private ResultActions save(ObjectNode body, int expectedStatus) throws Exception {
        return save(JSON.writeValueAsString(body), expectedStatus);
    }

    private ResultActions save(String body, int expectedStatus) throws Exception {
        ResultActions actions = mockMvc.perform(post(ENDPOINT).session(session).contentType(MediaType.APPLICATION_JSON)
                .content(body).accept(MediaType.APPLICATION_JSON));

        MockHttpServletResponse response = actions.andReturn().getResponse();

        assertEquals("POST " + ENDPOINT + " answered " + response.getStatus() + "; body was: "
                + response.getContentAsString(), expectedStatus, response.getStatus());

        return actions;
    }

    private ObjectNode clinicalOrder(String labNo) {
        ObjectNode form = JSON.createObjectNode();

        form.put("currentDate", today);
        form.put("warning", false);
        form.put("orderEntryOnly", false);
        form.put("sampleXML", sampleManifest());

        ObjectNode items = form.putObject("sampleOrderItems");

        items.put("labNo", labNo);
        items.put("receivedDateForDisplay", today);
        items.put("receivedTime", "10:00");
        items.put("requestDate", today);
        items.put("referringSiteId", "1");

        ObjectNode patient = form.putObject("patientProperties");

        patient.put("currentDate", today);
        patient.put("patientUpdateStatus", "ADD");
        patient.put("lastName", "Tester");
        patient.put("firstName", "Ann");
        patient.put("gender", "F");
        patient.put("nationalId", "NID" + PATIENT_SEQUENCE.incrementAndGet());

        return form;
    }

    /**
     * The same order tagged as an environmental collection, with the patient block
     * left blank the way the environmental form posts it. The block itself must
     * still be present — the save path reads {@code patientProperties} before it
     * ever looks at the workflow type — but every patient field it would normally
     * require (gender, names) is empty, which is exactly the validation noise the
     * environmental branch has to ignore.
     */
    private ObjectNode environmentalOrder(String labNo) {
        ObjectNode form = clinicalOrder(labNo);

        ObjectNode patient = (ObjectNode) form.get("patientProperties");

        patient.remove("lastName");
        patient.remove("firstName");
        patient.remove("nationalId");
        patient.put("gender", "");

        ObjectNode items = (ObjectNode) form.get("sampleOrderItems");

        items.putObject("environmentalFields").put("workflowType", "environmental");

        return form;
    }

    private String sampleManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><samples requiredBy=''>"
                + "<sample sampleID='1' typeId='1' sampleItemId='' date='" + today + "' time='09:00'"
                + " collector='' collectionConditions='' quantity='' uom='' receivedDate='' receivedTime=''"
                + " tests='1' testSectionMap='' testSampleTypeMap='' panels='' rejected='false'"
                + " rejectReasonId='' initialConditionIds='' />" + "</samples>";
    }

    private String emptySampleManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><samples requiredBy=''></samples>";
    }

    private String nextAccessionNumber() {
        return accessionNumbers.getNextAvailableAccessionNumber(null, true);
    }

    private String tomorrow() {
        return DateUtil.convertTimestampToStringDate(
                new java.sql.Timestamp(System.currentTimeMillis() + 24L * 60 * 60 * 1000));
    }

    private String sampleIdFor(String accessionNumber) {
        return jdbc.queryForObject("SELECT id::text FROM clinlims.sample " + "WHERE accession_number = ?", String.class,
                accessionNumber);
    }

    private Integer sampleCount() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.sample", Integer.class);
    }

    private void ensureOrderStatuses() {
        String[][] statuses = { { "Test Entered", "ORDER" }, { "SampleEntered", "SAMPLE" },
                { "Sample Rejected", "SAMPLE" }, { "Not Tested", "ANALYSIS" }, { "Sample Rejected", "ANALYSIS" } };

        for (String[] status : statuses) {
            jdbc.update(
                    "INSERT INTO clinlims.status_of_sample " + "(id, name, status_type, is_active, display_key, "
                            + "description, lastupdated) "
                            + "SELECT nextval('clinlims.status_of_sample_seq'), ?, ?, 'Y', ?, ?, " + "now() "
                            + "WHERE NOT EXISTS " + "(SELECT 1 FROM clinlims.status_of_sample "
                            + "WHERE name = ? AND status_type = ?)",
                    status[0], status[1], "status." + status[0].replace(' ', '.'), status[0], status[0], status[1]);
        }

        statusService.refreshCache();
    }

    private long registeredPatientCount() {
        return jdbc.queryForObject("SELECT count(*) " + "FROM clinlims.patient p "
                + "JOIN clinlims.person pe ON p.person_id = pe.id " + "WHERE pe.last_name <> 'UNKNOWN_'", Long.class);
    }

    private MockHttpSession authenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
                .build();

        SecurityContext securityContext = new SecurityContextImpl();

        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));

        UserSessionData userSessionData = new UserSessionData();

        userSessionData.setSytemUserId(Integer.parseInt(TEST_SYS_USER_ID));

        MockHttpSession httpSession = new MockHttpSession();

        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);

        return httpSession;
    }
}