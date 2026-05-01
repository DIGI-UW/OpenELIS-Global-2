package org.openelisglobal.biorepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleEventDTO;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleResponseDTO;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.ChainOfCustodyService;
import org.openelisglobal.biorepository.service.SampleLifecycleServiceImpl;
import org.openelisglobal.biorepository.service.SampleRetrievalService;
import org.openelisglobal.biorepository.service.SampleTransferService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.openelisglobal.systemuser.service.SystemUserService;

@RunWith(MockitoJUnitRunner.class)
public class SampleLifecycleServiceImplTest {

    @Mock
    private ChainOfCustodyService chainOfCustodyService;
    @Mock
    private BioSampleService bioSampleService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private SampleService sampleService;
    @Mock
    private SampleStorageService sampleStorageService;
    @Mock
    private SampleStorageMovementDAO sampleStorageMovementDAO;
    @Mock
    private SampleTransferService sampleTransferService;
    @Mock
    private SampleRetrievalService sampleRetrievalService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private StorageLocationService storageLocationService;

    @InjectMocks
    private SampleLifecycleServiceImpl lifecycleService;

    private SampleItem sampleItem;
    private BioSample bioSample;

    @Before
    public void setUp() {
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-1001");

        sampleItem = new SampleItem();
        sampleItem.setId("1001");
        sampleItem.setExternalId("SAMPLE-1001");
        sampleItem.setSample(sample);

        bioSample = new BioSample();
        bioSample.setId(501);
        bioSample.setSampleItem(sampleItem);
        bioSample.setWorkflowStatus(WorkflowStatus.STORED);

        when(sampleItemService.get("1001")).thenReturn(sampleItem);
        when(sampleItemService.getSampleItemsByExternalID("SAMPLE-1001")).thenReturn(List.of(sampleItem));
        when(sampleService.getSampleByAccessionNumber("SAMPLE-1001")).thenReturn(null);
        when(bioSampleService.getBySampleItemId(1001)).thenReturn(bioSample);
        when(sampleStorageService.getSampleItemLocation("1001"))
                .thenReturn(Map.of("hierarchicalPath", "Room-1 > Freezer-1 > Shelf-A > Rack-1 > Box-A > A1"));
        when(sampleTransferService.getBySampleItemId(1001)).thenReturn(List.of());
        when(sampleRetrievalService.getByBioSampleId(501)).thenReturn(List.of());
    }

    @Test
    public void search_sortsByTimestampThenSourceTypeThenSourceId() {
        Timestamp sameTs = Timestamp.valueOf("2026-04-30 09:00:00");

        ChainOfCustodyLog eventA = new ChainOfCustodyLog();
        eventA.setId(1);
        eventA.setSampleItem(sampleItem);
        eventA.setCustodyAction(CustodyAction.CHECKOUT_REQUESTED);
        eventA.setActionTimestamp(sameTs);
        eventA.setSourceRecordType("BType");
        eventA.setSourceRecordId(20);

        ChainOfCustodyLog eventB = new ChainOfCustodyLog();
        eventB.setId(2);
        eventB.setSampleItem(sampleItem);
        eventB.setCustodyAction(CustodyAction.CHECKOUT_APPROVED);
        eventB.setActionTimestamp(sameTs);
        eventB.setSourceRecordType("AType");
        eventB.setSourceRecordId(10);

        ChainOfCustodyLog latestEvent = new ChainOfCustodyLog();
        latestEvent.setId(3);
        latestEvent.setSampleItem(sampleItem);
        latestEvent.setCustodyAction(CustodyAction.CHECKOUT_RETRIEVED);
        latestEvent.setActionTimestamp(Timestamp.valueOf("2026-04-30 10:00:00"));
        latestEvent.setSourceRecordType("ZType");
        latestEvent.setSourceRecordId(1);

        when(chainOfCustodyService.getBySampleItemId(1001)).thenReturn(List.of(eventA, eventB, latestEvent));
        when(sampleStorageMovementDAO.findBySampleItemId("1001")).thenReturn(List.of());

        Map<String, Object> searchResponse = lifecycleService.search("SAMPLE-1001", null, null, null, 0, 10);

        @SuppressWarnings("unchecked")
        List<SampleLifecycleEventDTO> data = (List<SampleLifecycleEventDTO>) searchResponse.get("data");
        assertEquals(3, data.size());

        assertEquals(CustodyAction.CHECKOUT_RETRIEVED.name(), data.get(0).getEventType());
        assertEquals("AType", data.get(1).getSourceRecordType());
        assertEquals(Integer.valueOf(10), data.get(1).getSourceRecordId());
        assertEquals("BType", data.get(2).getSourceRecordType());
        assertEquals(Integer.valueOf(20), data.get(2).getSourceRecordId());
    }

    @Test
    public void getBySampleItemId_includesStorageMovementFallbackWithoutCustody() {
        when(chainOfCustodyService.getBySampleItemId(1001)).thenReturn(List.of());

        SampleStorageMovement movement = new SampleStorageMovement();
        movement.setId(77);
        movement.setSampleItemId(1001);
        movement.setMovementDate(Timestamp.valueOf("2026-04-30 08:00:00"));
        movement.setNewLocationType("room");
        movement.setNewLocationId(1);
        movement.setNewPositionCoordinate("A2");
        movement.setReason("Initial placement");
        when(sampleStorageMovementDAO.findBySampleItemId("1001")).thenReturn(List.of(movement));

        SampleLifecycleResponseDTO response = lifecycleService.getBySampleItemId(1001);

        assertNotNull(response);
        assertNotNull(response.getEvents());
        assertFalse(response.getEvents().isEmpty());
        assertTrue(response.getEvents().stream()
                .anyMatch(event -> CustodyAction.STORAGE_ASSIGNED.name().equals(event.getEventType())));
    }
}
