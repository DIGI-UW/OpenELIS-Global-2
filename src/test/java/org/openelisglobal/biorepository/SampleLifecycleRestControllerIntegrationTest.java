package org.openelisglobal.biorepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.RetrievalItemCreate;
import org.openelisglobal.biorepository.service.SampleRetrievalService;
import org.openelisglobal.biorepository.service.SampleTransferService;
import org.openelisglobal.biorepository.service.TransferItemMetadata;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.PriorityLevel;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.BaseStorageTest;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class SampleLifecycleRestControllerIntegrationTest extends BaseStorageTest {

    private static final String BOX_LOCATION_TYPE = "box";
    private static final String BOX_A_ID = "100";
    private static final String BOX_B_ID = "101";
    private static final String BOX_C_ID = "102";

    @Autowired
    private SampleTransferService transferService;

    @Autowired
    private SampleRetrievalService retrievalService;

    @Autowired
    private SampleStorageService sampleStorageService;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SystemUser requester;
    private SystemUser approver;
    private TypeOfSample testSampleType;
    private Sample testSample;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);

        requester = new SystemUser();
        requester.setLoginName("lcr_" + suffix);
        requester.setFirstName("Life" + suffix);
        requester.setLastName("Req" + suffix);
        requester.setIsActive("Y");
        requester.setIsEmployee("Y");
        requester.setSysUserId("1");
        requester = systemUserService.save(requester);

        approver = new SystemUser();
        approver.setLoginName("lca_" + suffix);
        approver.setFirstName("Life" + suffix);
        approver.setLastName("Apr" + suffix);
        approver.setIsActive("Y");
        approver.setIsEmployee("Y");
        approver.setSysUserId("1");
        approver = systemUserService.save(approver);

        List<TypeOfSample> sampleTypes = typeOfSampleService.getAll();
        testSampleType = sampleTypes.isEmpty() ? createSampleType("Serum") : sampleTypes.get(0);
        testSample = createSample("TEST-LIFECYCLE-" + System.currentTimeMillis());
    }

    @Test
    public void getBySampleItem_showsPendingStorageAfterTransferAcceptance() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-PENDING-" + System.currentTimeMillis());

        JsonNode response = getLifecycleBySampleItem(fixture.sampleItemId);

        assertEquals("PENDING_STORAGE", response.at("/currentState/workflowStatus").asText());
        assertFalse(response.at("/currentState/isPhysicallyInStorage").asBoolean());
        assertFalse(response.at("/currentState/awaitingRestorage").asBoolean());
        assertTrue(hasEvent(response, "TRANSFER_INITIATED"));
        assertTrue(hasEvent(response, "TRANSFER_RECEIVED"));
    }

    @Test
    public void getByBioSample_showsStoredStateAfterInitialAssignment() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-STORED-" + System.currentTimeMillis());
        assignToStorage(fixture.sampleItemId, BOX_A_ID, "A1", "Initial storage");

        JsonNode response = getLifecycleByBioSample(fixture.bioSampleId);

        assertEquals("STORED", response.at("/currentState/workflowStatus").asText());
        assertTrue(response.at("/currentState/isPhysicallyInStorage").asBoolean());
        assertTrue(response.at("/currentState/lastKnownStorageLocation").asText().contains("Plate A"));
        assertTrue(response.at("/currentState/lastKnownStorageLocation").asText().contains("A1"));
        assertTrue(hasEvent(response, "STORAGE_ASSIGNED"));
    }

    @Test
    public void getBySampleItem_showsStorageMovedEventAfterRelocation() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-MOVE-" + System.currentTimeMillis());
        assignToStorage(fixture.sampleItemId, BOX_A_ID, "A1", "Initial storage");
        moveInStorage(fixture.sampleItemId, BOX_B_ID, "B2", "Relocation");

        JsonNode response = getLifecycleBySampleItem(fixture.sampleItemId);

        assertEquals("STORED", response.at("/currentState/workflowStatus").asText());
        assertTrue(response.at("/currentState/lastKnownStorageLocation").asText().contains("Plate B"));
        assertTrue(response.at("/currentState/lastKnownStorageLocation").asText().contains("B2"));
        assertTrue(hasEvent(response, "STORAGE_MOVED"));
    }

    @Test
    public void getBySampleItem_showsCheckedOutStateDuringRetrieval() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-CHECKOUT-" + System.currentTimeMillis());
        assignToStorage(fixture.sampleItemId, BOX_A_ID, "A1", "Initial storage");

        SampleRetrievalRequest request = createApprovedRetrieval(fixture.bioSampleId, "Research analysis");
        SampleRetrievalItem item = retrievalService.retrieveItem(request.getItems().get(0).getId(), "Good", "Released for analysis", null, null, approver.getId().toString());

        JsonNode response = getLifecycleBySampleItem(fixture.sampleItemId);

        assertEquals("IN_USE", response.at("/currentState/workflowStatus").asText());
        assertFalse(response.at("/currentState/awaitingRestorage").asBoolean());
        assertEquals(request.getId(), Integer.valueOf(response.at("/currentState/activeRetrievalRequestId").asInt()));
        assertEquals(Integer.valueOf(item.getId()),
                Integer.valueOf(latestEvent(response, "CHECKOUT_RETRIEVED").at("/sourceRecordId").asInt()));
        assertTrue(hasEvent(response, "CHECKOUT_REQUESTED"));
        assertTrue(hasEvent(response, "CHECKOUT_APPROVED"));
        assertTrue(hasEvent(response, "CHECKOUT_RETRIEVED"));
    }

    @Test
    public void getBySampleItem_showsAwaitingRestorageAfterReturn() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-RETURN-" + System.currentTimeMillis());
        assignToStorage(fixture.sampleItemId, BOX_A_ID, "A1", "Initial storage");

        SampleRetrievalRequest request = createApprovedRetrieval(fixture.bioSampleId, "Temporary analysis");
        SampleRetrievalItem item = retrievalService.retrieveItem(request.getItems().get(0).getId(), "Good", "Released for analysis", null, null, approver.getId().toString());
        retrievalService.returnItem(item.getId(), "Good condition", "Returned to custody", false,
                approver.getId().toString());

        JsonNode response = getLifecycleBySampleItem(fixture.sampleItemId);

        assertEquals("PENDING_STORAGE", response.at("/currentState/workflowStatus").asText());
        assertTrue(response.at("/currentState/awaitingRestorage").asBoolean());
        assertFalse(response.at("/currentState/isPhysicallyInStorage").asBoolean());
        assertTrue(hasEvent(response, "RETURN_RECEIVED"));
        assertFalse(hasEvent(response, "RETURN_STORED"));
    }

    @Test
    public void getBySampleItem_showsReturnStoredAfterPhysicalRestorage() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-RESTORE-" + System.currentTimeMillis());
        assignToStorage(fixture.sampleItemId, BOX_A_ID, "A1", "Initial storage");

        SampleRetrievalRequest request = createApprovedRetrieval(fixture.bioSampleId, "Temporary analysis");
        SampleRetrievalItem item = retrievalService.retrieveItem(request.getItems().get(0).getId(), "Good", "Released for analysis", null, null, approver.getId().toString());
        retrievalService.returnItem(item.getId(), "Good condition", "Returned to custody", false,
                approver.getId().toString());
        moveInStorage(fixture.sampleItemId, BOX_C_ID, "C3", "Re-storage after return");

        JsonNode response = getLifecycleBySampleItem(fixture.sampleItemId);

        assertEquals("STORED", response.at("/currentState/workflowStatus").asText());
        assertFalse(response.at("/currentState/awaitingRestorage").asBoolean());
        assertTrue(response.at("/currentState/isPhysicallyInStorage").asBoolean());
        assertTrue(response.at("/currentState/lastKnownStorageLocation").asText().contains("Plate C"));
        assertTrue(hasEvent(response, "RETURN_STORED"));
    }

    @Test
    public void getBySampleItem_showsDisposedStateAfterConsumedReturn() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-DISPOSE-" + System.currentTimeMillis());
        assignToStorage(fixture.sampleItemId, BOX_A_ID, "A1", "Initial storage");

        SampleRetrievalRequest request = createApprovedRetrieval(fixture.bioSampleId, "Consumptive analysis");
        SampleRetrievalItem item = retrievalService.retrieveItem(request.getItems().get(0).getId(), "Good", "Released for analysis", null, null, approver.getId().toString());
        retrievalService.returnItem(item.getId(), "Consumed", "Sample exhausted", true, approver.getId().toString());

        JsonNode response = getLifecycleBySampleItem(fixture.sampleItemId);

        assertEquals("DISPOSED", response.at("/currentState/workflowStatus").asText());
        assertFalse(response.at("/currentState/isPhysicallyInStorage").asBoolean());
        assertFalse(response.at("/currentState/awaitingRestorage").asBoolean());
        JsonNode disposedEvent = latestEvent(response, "DISPOSED");
        assertNotNull(disposedEvent);
        assertEquals("DISPOSAL", disposedEvent.at("/stage").asText());
    }

    @Test
    public void search_filtersLifecycleEventsBySampleAndAction() throws Exception {
        LifecycleFixture fixture = createAcceptedTransferFixture("LC-SEARCH-" + System.currentTimeMillis());
        assignToStorage(fixture.sampleItemId, BOX_A_ID, "A1", "Initial storage");

        SampleRetrievalRequest request = createApprovedRetrieval(fixture.bioSampleId, "Search verification");
        SampleRetrievalItem item = retrievalService.retrieveItem(request.getItems().get(0).getId(), "Good", "Released for analysis", null, null, approver.getId().toString());
        retrievalService.returnItem(item.getId(), "Good condition", "Returned to custody", false,
                approver.getId().toString());

        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/lifecycle/search")
                        .param("sampleExternalId", fixture.sampleExternalId)
                        .param("action", "RETURN_RECEIVED")
                        .param("startDate", LocalDate.now().minusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(1).toString())
                        .param("page", "0")
                        .param("pageSize", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(response.at("/totalCount").asInt() >= 1);
        assertEquals(0, response.at("/page").asInt());
        assertEquals(5, response.at("/pageSize").asInt());
        assertEquals("RETURN_RECEIVED", response.at("/data/0/eventType").asText());
        assertEquals(fixture.sampleExternalId, response.at("/data/0/sampleExternalId").asText());
    }

    private JsonNode getLifecycleBySampleItem(Integer sampleItemId) throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/lifecycle/sample-item/" + sampleItemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getLifecycleByBioSample(Integer bioSampleId) throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/biorepository/lifecycle/bio-sample/" + bioSampleId)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private boolean hasEvent(JsonNode response, String eventType) {
        for (JsonNode event : response.withArray("events")) {
            if (eventType.equals(event.path("eventType").asText())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode latestEvent(JsonNode response, String eventType) {
        JsonNode latest = null;
        for (JsonNode event : response.withArray("events")) {
            if (eventType.equals(event.path("eventType").asText())) {
                latest = event;
            }
        }
        return latest;
    }

    private LifecycleFixture createAcceptedTransferFixture(String externalId) {
        SampleItem sampleItem = createSampleItem(externalId);
        SampleTransferRequest request = transferService.createTransferRequest("Lifecycle Source Lab",
                Arrays.asList(Integer.valueOf(sampleItem.getId())), "Test Project", "Lifecycle transfer",
                List.of(new TransferItemMetadata(Integer.valueOf(sampleItem.getId()), "Good", "RNAlater")),
                requester.getId().toString());

        BioSample bioSampleInput = new BioSample();
        bioSampleInput.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSampleInput.setEthicsApprovalRef("ETH-LC-001");
        BioSample createdBioSample = transferService.acceptItem(request.getItems().get(0).getId(), bioSampleInput,
                approver.getId().toString()).getBioSample();

        return new LifecycleFixture(Integer.valueOf(sampleItem.getId()), createdBioSample.getId(), externalId);
    }

    private void assignToStorage(Integer sampleItemId, String locationId, String position, String notes) {
        sampleStorageService.assignSampleItemWithLocation(String.valueOf(sampleItemId), locationId, BOX_LOCATION_TYPE,
                position, notes, approver.getId().toString());

        BioSample updated = bioSampleService.getBySampleItemId(sampleItemId);
        assertEquals(WorkflowStatus.STORED, updated.getWorkflowStatus());
    }

    private void moveInStorage(Integer sampleItemId, String locationId, String position, String notes) {
        sampleStorageService.moveSampleItemWithLocation(String.valueOf(sampleItemId), locationId, BOX_LOCATION_TYPE,
                position, "Lifecycle move", notes, approver.getId().toString());
    }

    private SampleRetrievalRequest createApprovedRetrieval(Integer bioSampleId, String purpose) {
        SampleRetrievalRequest request = retrievalService.createRequest(purpose, itemsFor(bioSampleId), null, null,
                DestinationType.ANALYSIS_RETURN, "Research Unit", PriorityLevel.NORMAL, LocalDate.now().plusDays(7),
                requester.getId().toString());
        request = retrievalService.submitForApproval(request.getId(), requester.getId().toString());
        return retrievalService.approveRequest(request.getId(), "Approved for lifecycle validation",
                approver.getId().toString());
    }

    private TypeOfSample createSampleType(String description) {
        TypeOfSample type = new TypeOfSample();
        type.setDescription(description);
        type.setDomain("H");
        return typeOfSampleService.save(type);
    }

    private Sample createSample(String accession) {
        Sample sample = new Sample();
        sample.setAccessionNumber(accession);
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(requester.getId().toString());
        return sampleService.save(sample);
    }

    private SampleItem createSampleItem(String externalId) {
        SampleItem item = new SampleItem();
        item.setSample(testSample);
        item.setExternalId(externalId);
        item.setTypeOfSample(testSampleType);
        item.setSortOrder("1");
        item.setQuantity(10.0);
        item.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        item.setStatusId("1");
        item.setSysUserId(requester.getId().toString());
        return sampleItemService.save(item);
    }

    private static class LifecycleFixture {
        private final Integer sampleItemId;
        private final Integer bioSampleId;
        private final String sampleExternalId;

        private LifecycleFixture(Integer sampleItemId, Integer bioSampleId, String sampleExternalId) {
            this.sampleItemId = sampleItemId;
            this.bioSampleId = bioSampleId;
            this.sampleExternalId = sampleExternalId;
        }
    }

    private List<RetrievalItemCreate> itemsFor(Integer... bioSampleIds) {
        List<RetrievalItemCreate> items = new java.util.ArrayList<>();
        for (Integer bioSampleId : bioSampleIds) {
            items.add(new RetrievalItemCreate(bioSampleId, null, null));
        }
        return items;
    }

}
