package org.openelisglobal.notebook.controller.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for PathologyWorkflowController metrics endpoint.
 *
 * Tests verify actual behavior with real data: - GET
 * /rest/notebook/pathology/metrics returns correct calculated metrics - Report
 * ID format validation - Specimen types breakdown reflects actual sample types
 * - QC metrics calculated from page sample data - Error handling for
 * invalid/missing parameters
 */
public class PathologyMetricsControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private IStatusService statusService;

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;
    private SystemUser testUser;
    private NoteBook testNotebook;
    private NotebookEntry testEntry;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_pathology_metrics_user");
            testUser.setFirstName("Test");
            testUser.setLastName("Pathology Metrics User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }

        // Set up mock session with user data for authentication
        mockSession = new MockHttpSession();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(Integer.parseInt(testUser.getId()));
        userSessionData.setLoginName(testUser.getLoginName());
        userSessionData.setAdmin(true);
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);

        // Setup test notebook (as a template to allow entry creation)
        testNotebook = createTestNotebook("Pathology Test Notebook " + System.currentTimeMillis());

        // Setup test entry
        testEntry = createTestNotebookEntry(testNotebook, "Pathology Test Entry " + System.currentTimeMillis());
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    public void testGetMetrics_InvalidEntryId_ReturnsNotFound() throws Exception {
        Integer nonExistentEntryId = 999999;

        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", nonExistentEntryId.toString()).contentType(MediaType.APPLICATION_JSON)).andReturn();

        assertEquals("Status should be 404 for non-existent entry", 404, result.getResponse().getStatus());
    }

    @Test
    public void testGetMetrics_MissingEntryId_ReturnsBadRequest() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/notebook/pathology/metrics").contentType(MediaType.APPLICATION_JSON)).andReturn();

        assertEquals("Status should be 400 when entryId is missing", 400, result.getResponse().getStatus());
    }

    // ========== REPORT ID TESTS ==========

    @Test
    public void testGetMetrics_ReportIdFormat_ContainsEntryIdAndTimestamp() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue("Response must have 'reportId' field", responseJson.has("reportId"));
        String reportId = responseJson.get("reportId").asText();

        // Verify Report ID format: RPT-{entryId}-{timestamp}
        assertTrue("Report ID must start with 'RPT-'", reportId.startsWith("RPT-"));
        assertTrue("Report ID must contain the entry ID", reportId.contains(testEntry.getId().toString()));

        // Parse and verify format
        String[] parts = reportId.split("-");
        assertEquals("Report ID should have 3 parts (RPT, entryId, timestamp)", 3, parts.length);
        assertEquals("First part should be 'RPT'", "RPT", parts[0]);
        assertEquals("Second part should be entry ID", testEntry.getId().toString(), parts[1]);

        // Verify timestamp is a valid number
        try {
            long timestamp = Long.parseLong(parts[2]);
            assertTrue("Timestamp should be recent (within last minute)",
                    System.currentTimeMillis() - timestamp < 60000);
        } catch (NumberFormatException e) {
            fail("Third part of Report ID should be a valid timestamp number");
        }
    }

    @Test
    public void testGetMetrics_EntryMetadata_MatchesCreatedEntry() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());

        // Verify entry ID matches exactly
        assertTrue("Response must have 'entryId' field", responseJson.has("entryId"));
        assertEquals("Entry ID must match the requested entry", testEntry.getId().intValue(),
                responseJson.get("entryId").asInt());

        // Verify entry title is not empty
        assertTrue("Response must have 'entryTitle' field", responseJson.has("entryTitle"));
        String entryTitle = responseJson.get("entryTitle").asText();
        assertFalse("Entry title must not be empty", entryTitle.isEmpty());
    }

    // ========== SPECIMEN VOLUME WITH DATA TESTS ==========

    @Test
    public void testGetMetrics_SpecimenVolume_ReflectsActualSamples() throws Exception {
        // Create samples with specific types
        TypeOfSample tissueType = getOrCreateTypeOfSample("Tissue", "TIS");
        TypeOfSample bloodType = getOrCreateTypeOfSample("Blood", "BLD");

        // Create 2 Tissue samples and 1 Blood sample
        addSampleToEntry(testEntry, tissueType, "TST001");
        addSampleToEntry(testEntry, tissueType, "TST002");
        addSampleToEntry(testEntry, bloodType, "TST003");

        // Refresh entry to get updated samples
        testEntry = notebookEntryService.getWithRelationships(testEntry.getId());

        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());

        // Verify total sample count
        JsonNode specimenVolume = responseJson.get("monthlySpecimenVolume");
        assertNotNull("Response must have monthlySpecimenVolume", specimenVolume);

        int total = specimenVolume.get("total").asInt();
        assertEquals("Total specimens must be 3", 3, total);

        // Verify breakdown by type
        JsonNode byType = specimenVolume.get("byType");
        assertTrue("byType must be an array", byType.isArray());
        assertEquals("Should have 2 specimen types", 2, byType.size());

        // Find and verify each type's count
        int tissueCount = 0;
        int bloodCount = 0;
        for (JsonNode typeEntry : byType) {
            String type = typeEntry.get("type").asText();
            int count = typeEntry.get("count").asInt();
            if ("Tissue".equals(type)) {
                tissueCount = count;
            } else if ("Blood".equals(type)) {
                bloodCount = count;
            }
        }

        assertEquals("Tissue count must be 2", 2, tissueCount);
        assertEquals("Blood count must be 1", 1, bloodCount);
    }

    @Test
    public void testGetMetrics_SpecimenVolume_PercentagesAreCorrect() throws Exception {
        // Create 3 Tissue and 1 Blood sample (75% Tissue, 25% Blood)
        TypeOfSample tissueType = getOrCreateTypeOfSample("Tissue", "TIS");
        TypeOfSample bloodType = getOrCreateTypeOfSample("Blood", "BLD");

        addSampleToEntry(testEntry, tissueType, "PCT001");
        addSampleToEntry(testEntry, tissueType, "PCT002");
        addSampleToEntry(testEntry, tissueType, "PCT003");
        addSampleToEntry(testEntry, bloodType, "PCT004");

        testEntry = notebookEntryService.getWithRelationships(testEntry.getId());

        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode byType = responseJson.get("monthlySpecimenVolume").get("byType");

        for (JsonNode typeEntry : byType) {
            String type = typeEntry.get("type").asText();
            double percentage = typeEntry.get("percentage").asDouble();

            if ("Tissue".equals(type)) {
                assertEquals("Tissue percentage should be 75%", 75.0, percentage, 0.1);
            } else if ("Blood".equals(type)) {
                assertEquals("Blood percentage should be 25%", 25.0, percentage, 0.1);
            }
        }
    }

    // ========== QC METRICS WITH DATA TESTS ==========

    @Test
    public void testGetMetrics_QCMetrics_ReflectPageSampleData() throws Exception {
        // Create samples and page with QC data
        TypeOfSample tissueType = getOrCreateTypeOfSample("Tissue", "TIS");
        SampleItem sample1 = addSampleToEntry(testEntry, tissueType, "QC001");
        SampleItem sample2 = addSampleToEntry(testEntry, tissueType, "QC002");
        SampleItem sample3 = addSampleToEntry(testEntry, tissueType, "QC003");

        // Create a page for QC data
        NoteBookPage page = createTestPage(testNotebook, "QC Test Page");

        // Create page samples: 2 PASS, 1 FAIL
        createPageSampleWithQCStatus(page, sample1, "PASS");
        createPageSampleWithQCStatus(page, sample2, "PASS");
        createPageSampleWithQCStatus(page, sample3, "FAIL");

        testEntry = notebookEntryService.getWithRelationships(testEntry.getId());

        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());

        // Verify QC counts
        int qcPassCount = responseJson.get("qcPassCount").asInt();
        int qcFailCount = responseJson.get("qcFailCount").asInt();
        int qcIncidents = responseJson.get("qcIncidents").asInt();

        assertEquals("QC pass count must be 2", 2, qcPassCount);
        assertEquals("QC fail count must be 1", 1, qcFailCount);
        assertEquals("QC incidents should equal QC fail count", qcFailCount, qcIncidents);

        // Verify rejection rate calculation: 1 fail / 3 total = 33.33%
        double rejectionRate = responseJson.get("specimenRejectionRate").asDouble();
        assertEquals("Rejection rate should be ~33.33%", 33.33, rejectionRate, 0.5);
    }

    @Test
    public void testGetMetrics_EmptyEntry_ReturnsZeroMetrics() throws Exception {
        // Test with the empty entry (no samples)
        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());

        // Verify zero values for empty entry
        assertEquals("Total samples should be 0 for empty entry", 0,
                responseJson.get("monthlySpecimenVolume").get("total").asInt());
        assertEquals("Total test orders should be 0", 0, responseJson.get("totalTestOrders").asInt());
        assertEquals("QC pass count should be 0", 0, responseJson.get("qcPassCount").asInt());
        assertEquals("QC fail count should be 0", 0, responseJson.get("qcFailCount").asInt());

        // Verify byType is empty array
        assertTrue("byType should be empty array for empty entry",
                responseJson.get("monthlySpecimenVolume").get("byType").isEmpty());
    }

    // ========== LINKED TEST ORDERS TESTS ==========

    @Test
    public void testGetMetrics_LinkedTestOrders_ReflectActualSamples() throws Exception {
        // Create samples with unique accession numbers
        TypeOfSample tissueType = getOrCreateTypeOfSample("Tissue", "TIS");
        addSampleToEntry(testEntry, tissueType, "LTO001");
        addSampleToEntry(testEntry, tissueType, "LTO002");

        testEntry = notebookEntryService.getWithRelationships(testEntry.getId());

        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());

        JsonNode linkedTestOrders = responseJson.get("linkedTestOrders");
        assertTrue("linkedTestOrders must be an array", linkedTestOrders.isArray());
        assertEquals("Should have 2 linked test orders", 2, linkedTestOrders.size());

        int totalTestOrders = responseJson.get("totalTestOrders").asInt();
        assertEquals("totalTestOrders must match array size", linkedTestOrders.size(), totalTestOrders);

        // Verify structure of test order entries
        for (JsonNode order : linkedTestOrders) {
            assertTrue("Each order must have accessionNumber", order.has("accessionNumber"));
            assertTrue("Each order must have sampleId", order.has("sampleId"));
            assertTrue("Each order must have specimenType", order.has("specimenType"));
        }
    }

    // ========== DATE RANGE PARAMETER TESTS ==========

    @Test
    public void testGetMetrics_AcceptsOptionalDateParameters() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/notebook/pathology/metrics")
                .param("entryId", testEntry.getId().toString()).param("startDate", "2026-01-01")
                .param("endDate", "2026-12-31").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());

        // Verify response is valid even with date parameters
        assertTrue("Response must have reportId", responseJson.has("reportId"));
        assertTrue("Response must have monthlySpecimenVolume", responseJson.has("monthlySpecimenVolume"));
        assertFalse("Response must not have error field", responseJson.has("error"));
    }

    // ========== HELPER METHODS ==========

    private NoteBook createTestNotebook(String title) {
        NoteBook notebook = new NoteBook();
        notebook.setTitle(title);
        notebook.setIsTemplate(true);
        notebook.setStatus(NoteBookStatus.ACTIVE);
        notebook.setDateCreated(new Date());
        notebook.setSysUserId(testUser.getId().toString());
        return noteBookService.save(notebook);
    }

    private NotebookEntry createTestNotebookEntry(NoteBook notebook, String title) {
        return notebookEntryService.createEntry(notebook.getId(), title, testUser.getId().toString());
    }

    private NoteBookPage createTestPage(NoteBook notebook, String title) {
        NoteBookPage page = new NoteBookPage();
        page.setNotebook(notebook);
        page.setTitle(title);
        page.setSysUserId(testUser.getId().toString());
        return noteBookPageService.save(page);
    }

    private TypeOfSample getOrCreateTypeOfSample(String description, String abbrev) {
        // First try to find by description (which is what the duplicate check uses)
        TypeOfSample searchTos = new TypeOfSample();
        searchTos.setDescription(description);
        searchTos.setDomain("H");
        TypeOfSample existing = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchTos, true);
        if (existing != null) {
            return existing;
        }

        // Not found, create new with unique abbreviation
        String uniqueAbbrev = abbrev + System.currentTimeMillis() % 10000;
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setDescription(description);
        typeOfSample.setLocalAbbreviation(uniqueAbbrev);
        typeOfSample.setDomain("H");
        typeOfSample.setIsActive(true);
        typeOfSample.setSysUserId(testUser.getId().toString());
        return typeOfSampleService.save(typeOfSample);
    }

    private SampleItem addSampleToEntry(NotebookEntry entry, TypeOfSample typeOfSample, String accessionPrefix) {
        // Create sample with unique short accession number (max 20 chars)
        String accession = accessionPrefix.substring(0, Math.min(accessionPrefix.length(), 10))
                + (System.currentTimeMillis() % 100000000);
        if (accession.length() > 20) {
            accession = accession.substring(0, 20);
        }

        Sample sample = new Sample();
        sample.setAccessionNumber(accession);
        sample.setSysUserId(testUser.getId().toString());
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setReceivedDate(new java.sql.Date(System.currentTimeMillis()));
        sample = sampleService.save(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(typeOfSample);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
        sampleItem.setSysUserId(testUser.getId().toString());
        sampleItem = sampleItemService.save(sampleItem);

        // Use the service method to add samples - it handles the transaction properly
        List<SampleItem> samples = new ArrayList<>();
        samples.add(sampleItem);
        notebookEntryService.addSamples(entry.getId(), samples, testUser.getId().toString());

        return sampleItem;
    }

    private void createPageSampleWithQCStatus(NoteBookPage page, SampleItem sampleItem, String qcStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("qcStatus", qcStatus);

        NotebookPageSample pageSample = new NotebookPageSample();
        pageSample.setNotebookPage(page);
        pageSample.setSampleItemId(sampleItem.getId().toString());
        pageSample.setData(data);
        pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
        pageSample.setSysUserId(testUser.getId().toString());
        notebookPageSampleService.save(pageSample);
    }
}
