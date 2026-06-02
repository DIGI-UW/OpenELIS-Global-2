package org.openelisglobal.biorepository.controller.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.service.RetrievalFulfillmentSuggestionService;
import org.openelisglobal.biorepository.service.SampleRetrievalService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.PriorityLevel;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.service.StorageLocationService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class SampleRetrievalFulfillmentsMappingTest {

    @InjectMocks
    private SampleRetrievalRestController controller;

    @Mock
    private SampleRetrievalService retrievalService;

    @Mock
    private RetrievalFulfillmentSuggestionService fulfillmentSuggestionService;

    @Mock
    private NotebookEntryService notebookEntryService;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private StorageLocationService storageLocationService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getRequest_mapsNotebookEntryIdWithoutRequiringLazyNotebook() throws Exception {
        SampleRetrievalRequest request = new SampleRetrievalRequest();
        request.setId(42);
        request.setStatus(RequestStatus.APPROVED);
        request.setRequestNumber("WO-NB-ENTRY");
        request.setDestinationType(DestinationType.INTERNAL_LAB);
        request.setPriorityLevel(PriorityLevel.NORMAL);
        request.setRequestedTimestamp(new Timestamp(System.currentTimeMillis()));

        NotebookEntry entry = new NotebookEntry();
        entry.setId(6);
        request.setNotebookEntry(entry);

        when(retrievalService.get(42)).thenReturn(request);

        mockMvc.perform(get("/rest/biorepository/retrieval/requests/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notebookEntryId").value(6))
                .andExpect(jsonPath("$.notebookId").doesNotExist());
    }

    @Test
    public void getRequest_includesFulfillmentsWhenLazyParentUnloaded() throws Exception {
        SampleRetrievalRequest request = new SampleRetrievalRequest();
        request.setId(42);
        request.setStatus(RequestStatus.APPROVED);
        request.setRequestNumber("WO-TEST");
        request.setDestinationType(DestinationType.INTERNAL_LAB);
        request.setPriorityLevel(PriorityLevel.NORMAL);
        request.setRequestedTimestamp(new Timestamp(System.currentTimeMillis()));

        SampleRetrievalItem reference = new SampleRetrievalItem();
        reference.setId(1);
        reference.setStatus(SampleRetrievalItem.ItemStatus.AWAITING_FULFILLMENT);

        BioSample bioSample = new BioSample();
        bioSample.setId(77);
        bioSample.setWorkflowStatus(WorkflowStatus.STORED);
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("501");
        bioSample.setSampleItem(sampleItem);

        SampleRetrievalItem fulfillment = new SampleRetrievalItem();
        fulfillment.setId(2);
        fulfillment.setBioSample(bioSample);
        fulfillment.setStatus(SampleRetrievalItem.ItemStatus.PENDING);
        setFulfillsItemIdColumn(fulfillment, 1);

        request.getItems().add(reference);
        request.getItems().add(fulfillment);

        when(retrievalService.get(42)).thenReturn(request);

        mockMvc.perform(get("/rest/biorepository/retrieval/requests/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].fulfillments[0].id").value(2))
                .andExpect(jsonPath("$.items[0].fulfillments[0].fulfillsItemId").value(1));
    }

    private static void setFulfillsItemIdColumn(SampleRetrievalItem item, int referenceId) throws Exception {
        Field field = SampleRetrievalItem.class.getDeclaredField("fulfillsItemIdColumn");
        field.setAccessible(true);
        field.set(item, referenceId);
    }
}
