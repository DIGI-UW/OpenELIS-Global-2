package org.openelisglobal.biorepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
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

/**
 * Integration tests for BiorepositoryQCInspectionRestController input validation.
 */
public class BiorepositoryQCInspectionRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SystemUserService systemUserService;

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;
    private SystemUser testUser;
    private TypeOfSample testSampleType;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_qc_rest_user");
            testUser.setFirstName("Test");
            testUser.setLastName("QC REST User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }

        mockSession = new MockHttpSession();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(Integer.parseInt(testUser.getId()));
        userSessionData.setLoginName(testUser.getLoginName());
        userSessionData.setAdmin(true);
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);

        List<TypeOfSample> allTypes = typeOfSampleService.getAll();
        if (allTypes.isEmpty()) {
            throw new IllegalStateException("No sample types found in database");
        }
        testSampleType = allTypes.get(0);
    }

    @Test
    public void testGenerateRound_WithoutSession_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/rest/biorepository/qc-inspection/generate-round").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("User session not found. Please log in again."));
    }

    @Test
    public void testGenerateRound_InvalidSamplesPerBox_ReturnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("boxesPerRound", 10);
        payload.put("samplesPerBox", 0);

        mockMvc.perform(post("/rest/biorepository/qc-inspection/generate-round").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("samplesPerBox must be between 1 and 200"));
    }

    @Test
    public void testBulkApply_DuplicateBioSampleIds_ReturnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bioSampleIds", List.of(123, 123));
        payload.put("inspectorName", "Inspector A");

        mockMvc.perform(post("/rest/biorepository/qc-inspection/bulk-apply").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Duplicate biosample IDs are not allowed in bulk QC request"));
    }

    @Test
    public void testBulkApply_FailedQCWithoutRemarks_ReturnsBadRequest() throws Exception {
        BioSample bioSample = createStoredBioSample("QC-REST-" + System.currentTimeMillis());

        Map<String, Object> payload = new HashMap<>();
        payload.put("bioSampleIds", List.of(bioSample.getId()));
        payload.put("inspectorName", "Inspector B");
        payload.put("samplePresent", false);
        payload.put("labelIntegrity", true);
        payload.put("containerIntegrity", true);
        payload.put("volumeAppearanceAcceptable", true);
        payload.put("correctPosition", true);
        payload.put("discrepancyType", "MISSING_SAMPLE");
        payload.put("correctiveAction", "Investigate missing sample");
        payload.put("remarks", "   ");

        mockMvc.perform(post("/rest/biorepository/qc-inspection/bulk-apply").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Comment/remarks are required when QC fails"));
    }

    private BioSample createStoredBioSample(String externalId) {
        Sample sample = new Sample();
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000000);
        sample.setAccessionNumber("QCR" + timestamp);
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(testUser.getId());
        sample = sampleService.save(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(testSampleType);
        sampleItem.setStatusId("1");
        sampleItem.setSortOrder("1");
        sampleItem.setExternalId(externalId);
        sampleItem.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        sampleItem.setSysUserId(testUser.getId());
        sampleItem = sampleItemService.save(sampleItem);

        BioSample bioSample = new BioSample();
        bioSample.setSampleItem(sampleItem);
        bioSample.setBiosafetyLevel(BioSample.BiosafetyLevel.BSL_1);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setPreservationMedium("EDTA");
        bioSample.setSysUserId(testUser.getId());

        return bioSampleService.save(bioSample);
    }
}
