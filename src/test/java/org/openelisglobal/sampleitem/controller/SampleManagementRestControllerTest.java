package org.openelisglobal.sampleitem.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sampleitem.dto.AddTestsResponse;
import org.openelisglobal.sampleitem.dto.CancelTestResponse;
import org.openelisglobal.sampleitem.dto.CreateAliquotResponse;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.openelisglobal.sampleitem.form.AddTestsForm;
import org.openelisglobal.sampleitem.form.CancelTestForm;
import org.openelisglobal.sampleitem.form.CreateAliquotForm;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

@WithMockUser(username = "admin", roles = { "ADMIN", "RESULTS" })
public class SampleManagementRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String BASE_PATH = "/rest/sample-management";
    private static final String ACCESSION = "SM-TEST-001";
    private static final String DEFAULT_PASSWORD = "adminADMIN!";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private IStatusService statusService;

    private ObjectMapper objectMapper;
    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        executeDataSetWithStateManagement("testdata/sample-management-controller.xml");
        session = buildAuthenticatedSession();
    }

    @Test
    public void searchByAccessionNumber_shouldReturnSampleItems() throws Exception {
        MvcResult result = mockMvc
                .perform(get(BASE_PATH + "/search").param("accessionNumber", ACCESSION).session(session))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        SearchSamplesResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                SearchSamplesResponse.class);

        assertEquals(ACCESSION, response.getAccessionNumber());
        assertEquals(2, response.getTotalCount());
        assertEquals(2, response.getSampleItems().size());
        assertEquals("SM-TEST-001-1", response.getSampleItems().get(0).getExternalId());
        assertEquals("SM-TEST-001-2", response.getSampleItems().get(1).getExternalId());
    }

    @Test
    public void searchByAccessionNumber_unknownAccession_returnsEmptyList() throws Exception {
        MvcResult result = mockMvc
                .perform(get(BASE_PATH + "/search").param("accessionNumber", "UNKNOWN-999").session(session))
                .andExpect(status().isOk()).andReturn();

        SearchSamplesResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                SearchSamplesResponse.class);

        assertEquals("UNKNOWN-999", response.getAccessionNumber());
        assertEquals(Collections.emptyList(), response.getSampleItems());
        assertEquals(0, response.getTotalCount());
    }

    @Test
    public void searchByAccessionNumber_blankAccessionNumber_returnsEmptyList() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_PATH + "/search").param("accessionNumber", "").session(session))
                .andExpect(status().isOk()).andReturn();

        SearchSamplesResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                SearchSamplesResponse.class);

        assertEquals("", response.getAccessionNumber());
        assertEquals(Collections.emptyList(), response.getSampleItems());
        assertEquals(0, response.getTotalCount());
    }

    @Test
    public void createAliquot_shouldPersistChildSampleItemLinkedToParent() throws Exception {
        SampleItem initialParent = sampleItemService.getData("10001");
        assertEquals("10001", initialParent.getId());
        assertEquals(10.0, initialParent.getRemainingQuantity().doubleValue(), 0.001);

        CreateAliquotForm form = new CreateAliquotForm();
        form.setParentSampleItemId("10001");
        form.setQuantityToTransfer(new BigDecimal("2.5"));
        form.setNumberOfAliquots(1);
        form.setNotes("Test Aliquot");

        MvcResult result = mockMvc
                .perform(post(BASE_PATH + "/aliquot").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)).session(session))
                .andExpect(status().isCreated()).andReturn();

        CreateAliquotResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                CreateAliquotResponse.class);

        assertEquals(1, response.getAliquotCount());
        assertTrue(response.getAliquot().getExternalId().matches("SM-TEST-001-1\\.\\d+"));
        assertEquals(7.5, response.getParentUpdatedRemainingQuantity().doubleValue(), 0.001);
        assertEquals(2.5, response.getQuantityPerAliquot().doubleValue(), 0.001);

        SampleItem updatedParent = sampleItemService.getData("10001");
        assertEquals("10001", updatedParent.getId());
        assertEquals(7.5, updatedParent.getRemainingQuantity().doubleValue(), 0.001);

        String childId = response.getAliquot().getId();
        SampleItem child = sampleItemService.getData(childId);
        assertEquals(response.getAliquot().getExternalId(), child.getExternalId());
        assertEquals("10001", child.getParentSampleItem().getId());
        assertEquals(2.5, child.getQuantity(), 0.001);
        assertEquals(2.5, child.getRemainingQuantity().doubleValue(), 0.001);
        assertEquals(initialParent.getSample().getId(), child.getSample().getId());
    }

    @Test
    public void createAliquot_zeroQuantity_returnsInvalidRequestError() throws Exception {
        CreateAliquotForm form = new CreateAliquotForm();
        form.setParentSampleItemId("10001");
        form.setQuantityToTransfer(new BigDecimal("0"));

        mockMvc.perform(post(BASE_PATH + "/aliquot").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)).session(session)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Quantity to transfer must be greater than 0"));
    }

    @Test
    public void createAliquot_invalidParentId_returnsInvalidRequestError() throws Exception {
        CreateAliquotForm form = new CreateAliquotForm();
        form.setParentSampleItemId("99999");
        form.setQuantityToTransfer(new BigDecimal("1.0"));
        form.setNumberOfAliquots(1);

        mockMvc.perform(post(BASE_PATH + "/aliquot").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)).session(session)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Parent sample item not found: 99999"));
    }

    @Test
    public void addTestsToSamples_shouldLinkTestToSampleItemAndReportSuccess() throws Exception {
        SampleItem sampleItem = sampleItemService.getData("10001");
        assertEquals("10001", sampleItem.getId());
        List<Analysis> initialAnalyses = analysisService.getAnalysesBySampleItem(sampleItem);
        assertEquals(1, initialAnalyses.size());
        assertEquals("1", initialAnalyses.get(0).getTest().getId());

        AddTestsForm form = new AddTestsForm();
        form.setSampleItemIds(Arrays.asList("10001"));
        form.setTestIds(Arrays.asList("2"));

        MvcResult result = mockMvc
                .perform(post(BASE_PATH + "/add-tests").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)).session(session))
                .andExpect(status().isOk()).andReturn();

        AddTestsResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                AddTestsResponse.class);

        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getResults().size());
        assertEquals(true, response.getResults().get(0).isSuccess());
        assertEquals("10001", response.getResults().get(0).getSampleItemId());
        assertEquals(Arrays.asList("2"), response.getResults().get(0).getAddedTestIds());
        assertEquals(Collections.emptyList(), response.getResults().get(0).getSkippedTestIds());

        List<Analysis> postAnalyses = analysisService.getAnalysesBySampleItem(sampleItem);
        assertEquals(2, postAnalyses.size());
        boolean test2Persisted = postAnalyses.stream()
                .anyMatch(a -> a.getTest() != null && "2".equals(a.getTest().getId()));
        assertTrue(test2Persisted);
    }

    @Test
    public void addTestsToSamples_duplicateTest_isSkippedAndReportedCorrectly() throws Exception {
        AddTestsForm form = new AddTestsForm();
        form.setSampleItemIds(Arrays.asList("10002"));
        form.setTestIds(Arrays.asList("1"));

        MvcResult result = mockMvc
                .perform(post(BASE_PATH + "/add-tests").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)).session(session))
                .andExpect(status().isOk()).andReturn();

        AddTestsResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                AddTestsResponse.class);

        assertEquals(0, response.getSuccessCount());
        assertEquals(1, response.getResults().size());
        assertEquals(true, response.getResults().get(0).isSuccess());
        assertEquals("10002", response.getResults().get(0).getSampleItemId());
        assertEquals(Collections.emptyList(), response.getResults().get(0).getAddedTestIds());
        assertEquals(Arrays.asList("1"), response.getResults().get(0).getSkippedTestIds());
    }

    @Test
    public void addTestsToSamples_invalidSampleItemId_returnsInvalidRequestError() throws Exception {
        AddTestsForm form = new AddTestsForm();
        form.setSampleItemIds(Arrays.asList("99999"));
        form.setTestIds(Arrays.asList("1"));

        mockMvc.perform(post(BASE_PATH + "/add-tests").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)).session(session)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Sample item not found: 99999"));
    }

    @Test
    public void cancelTest_shouldTransitionAnalysisStatusToCancelled() throws Exception {
        Analysis initialAnalysis = analysisService.getAnalysisById("10001");
        String notStartedStatusId = statusService.getStatusID(StatusService.AnalysisStatus.NotStarted);
        assertEquals("10001", initialAnalysis.getId());
        assertEquals(notStartedStatusId, initialAnalysis.getStatusId());

        CancelTestForm form = new CancelTestForm();
        form.setAnalysisId("10001");
        form.setSampleItemId("10001");

        MvcResult result = mockMvc
                .perform(post(BASE_PATH + "/cancel-test").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)).session(session))
                .andExpect(status().isOk()).andReturn();

        CancelTestResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                CancelTestResponse.class);

        assertEquals(true, response.isSuccess());
        assertEquals("10001", response.getAnalysisId());

        Analysis analysis = analysisService.getAnalysisById("10001");
        String canceledStatusId = statusService.getStatusID(StatusService.AnalysisStatus.Canceled);
        assertEquals(canceledStatusId, analysis.getStatusId());
    }

    @Test
    public void cancelTest_finalizedAnalysis_returnsInvalidStateError() throws Exception {
        Analysis initialAnalysis = analysisService.getAnalysisById("10002");
        String finalizedStatusId = statusService.getStatusID(StatusService.AnalysisStatus.Finalized);
        assertEquals("10002", initialAnalysis.getId());
        assertEquals(finalizedStatusId, initialAnalysis.getStatusId());

        CancelTestForm form = new CancelTestForm();
        form.setAnalysisId("10002");
        form.setSampleItemId("10002");

        mockMvc.perform(post(BASE_PATH + "/cancel-test").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)).session(session)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid State"))
                .andExpect(jsonPath("$.message").value("Cannot cancel test: analysis is already Finalized"));

        Analysis postAttemptAnalysis = analysisService.getAnalysisById("10002");
        assertEquals(finalizedStatusId, postAttemptAnalysis.getStatusId());
    }

    private MockHttpSession buildAuthenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password(DEFAULT_PASSWORD)
                .authorities("ROLE_ADMIN", "ROLE_RESULTS").build();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, DEFAULT_PASSWORD, userDetails.getAuthorities()));

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);

        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return httpSession;
    }
}
