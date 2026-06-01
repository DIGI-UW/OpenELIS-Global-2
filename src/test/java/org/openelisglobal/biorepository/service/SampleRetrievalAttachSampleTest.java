package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.dao.SampleRetrievalRequestDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;

@RunWith(MockitoJUnitRunner.class)
public class SampleRetrievalAttachSampleTest {

    @Spy
    @InjectMocks
    private SampleRetrievalServiceImpl retrievalService;

    @Mock
    private SampleRetrievalRequestDAO baseObjectDAO;

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private SampleStorageService sampleStorageService;

    private SampleRetrievalRequest request;
    private SampleRetrievalItem referenceItem;

    @Before
    public void setUp() {
        when(sampleStorageService.getSampleItemLocation(any())).thenReturn(java.util.Map.of());

        request = new SampleRetrievalRequest();
        request.setId(10);
        request.setStatus(RequestStatus.APPROVED);

        referenceItem = new SampleRetrievalItem();
        referenceItem.setId(20);
        referenceItem.setRetrievalRequest(request);
        referenceItem.setStatus(SampleRetrievalItem.ItemStatus.AWAITING_FULFILLMENT);
        referenceItem.setRequestedSampleType("Plasma");
        referenceItem.setQuantityRequested(BigDecimal.valueOf(2));
        referenceItem.setReturnExpected(true);
        request.getItems().add(referenceItem);

        doReturn(referenceItem).when(retrievalService).getRetrievalItem(20);
        when(baseObjectDAO.update(any(SampleRetrievalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void attachSampleToReferenceItem_createsFulfillmentChild() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("501");
        sampleItem.setExternalId("BIO-ATTACH-1");
        sampleItem.setQuantity(5.0);
        sampleItem.setRemainingQuantity(BigDecimal.valueOf(5));
        sampleItem.setUnitOfMeasureName("mL");

        BioSample bioSample = new BioSample();
        bioSample.setId(99);
        bioSample.setWorkflowStatus(WorkflowStatus.STORED);
        bioSample.setSampleItem(sampleItem);

        when(bioSampleService.get(99)).thenReturn(bioSample);
        when(baseObjectDAO.hasActiveRetrievalForBioSample(99)).thenReturn(false);

        SampleRetrievalItem fulfillment = retrievalService.attachSampleToReferenceItem(
                20, 99, BigDecimal.valueOf(2), "user-1");

        assertNotNull(fulfillment);
        assertEquals(SampleRetrievalItem.ItemStatus.PENDING, fulfillment.getStatus());
        assertEquals(referenceItem, fulfillment.getFulfillsItem());
        assertEquals(bioSample, fulfillment.getBioSample());
        assertEquals(2, request.getItems().size());
    }
}
