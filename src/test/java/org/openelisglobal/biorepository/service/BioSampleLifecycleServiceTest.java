package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleLifecycleEventDTO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@RunWith(MockitoJUnitRunner.class)
public class BioSampleLifecycleServiceTest {

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private SampleTransferService sampleTransferService;

    @Mock
    private SampleRetrievalService sampleRetrievalService;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private ChainOfCustodyService chainOfCustodyService;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private BioSampleLifecycleServiceImpl lifecycleService;

    private BioSample bioSample;
    private SampleItem sampleItem;

    @Before
    public void setUp() {
        sampleItem = new SampleItem();
        sampleItem.setId("10");

        bioSample = new BioSample();
        bioSample.setId(100);
        bioSample.setSampleItem(sampleItem);
    }

    @Test
    public void buildLifecycleEvents_unknownBioSample_returnsEmpty() {
        when(bioSampleService.get(999)).thenReturn(null);
        assertEquals(Collections.emptyList(), lifecycleService.buildLifecycleEvents(999));
    }

    @Test
    public void buildLifecycleEvents_stored_readsMovementAndResolveUser() {
        when(bioSampleService.get(100)).thenReturn(bioSample);
        when(sampleTransferService.getBySampleItemId(10)).thenReturn(Collections.emptyList());
        when(sampleRetrievalService.getByBioSampleId(100)).thenReturn(Collections.emptyList());
        when(chainOfCustodyService.getBySampleItemId(10)).thenReturn(Collections.emptyList());

        SampleStorageMovement move = new SampleStorageMovement();
        move.setNewLocationId(5);
        move.setNewLocationType("rack");
        move.setNewPositionCoordinate("A1");
        move.setMovementDate(new Timestamp(3_000L));
        move.setMovedByUserId(7);
        when(sampleStorageService.getSampleStorageMovementsBySampleItem(sampleItem))
                .thenReturn(Collections.singletonList(move));

        SystemUser mover = new SystemUser();
        mover.setId("7");
        mover.setLoginName("mover");
        mover.setFirstName("Mo");
        mover.setLastName("Ver");
        when(systemUserService.get("7")).thenReturn(mover);

        List<BioSampleLifecycleEventDTO> events = lifecycleService.buildLifecycleEvents(100);
        assertEquals(1, events.size());
        assertEquals(BioSampleLifecycleServiceImpl.TYPE_STORED, events.get(0).getEventType());
        assertEquals("rack #5 · A1", events.get(0).getDestinationLocation());
        verify(sampleStorageService).getSampleStorageMovementsBySampleItem(sampleItem);
    }

    @Test
    public void buildLifecycleEvents_includesTransferredWhenTransferAccepted() {
        when(bioSampleService.get(100)).thenReturn(bioSample);
        when(sampleStorageService.getSampleStorageMovementsBySampleItem(sampleItem))
                .thenReturn(Collections.emptyList());
        when(sampleRetrievalService.getByBioSampleId(100)).thenReturn(Collections.emptyList());
        when(chainOfCustodyService.getBySampleItemId(10)).thenReturn(Collections.emptyList());

        SystemUser processor = new SystemUser();
        processor.setId("2");
        processor.setFirstName("P");
        processor.setLastName("Roc");

        SampleTransferItem ti = new SampleTransferItem();
        ti.setSampleItem(sampleItem);
        ti.setBioSample(bioSample);
        ti.setStatus(ItemStatus.ACCEPTED);

        SampleTransferRequest tr = new SampleTransferRequest();
        tr.addItem(ti);
        tr.setSourceLab("Lab A");
        tr.setDestinationLab("BIOREPOSITORY");
        tr.setProcessedTimestamp(new Timestamp(5_000L));
        tr.setRequestedTimestamp(new Timestamp(1_000L));
        tr.setProcessedBy(processor);

        when(sampleTransferService.getBySampleItemId(10)).thenReturn(Collections.singletonList(tr));

        List<BioSampleLifecycleEventDTO> events = lifecycleService.buildLifecycleEvents(100);
        assertEquals(1, events.size());
        assertEquals(BioSampleLifecycleServiceImpl.TYPE_TRANSFERRED, events.get(0).getEventType());
        assertEquals("Lab A", events.get(0).getSourceLocation());
        assertEquals("BIOREPOSITORY", events.get(0).getDestinationLocation());
    }

    @Test
    public void buildLifecycleEvents_sortsOldestFirstAcrossMovements() {
        when(bioSampleService.get(100)).thenReturn(bioSample);
        when(sampleTransferService.getBySampleItemId(10)).thenReturn(Collections.emptyList());
        when(sampleRetrievalService.getByBioSampleId(100)).thenReturn(Collections.emptyList());
        when(chainOfCustodyService.getBySampleItemId(10)).thenReturn(Collections.emptyList());
        when(systemUserService.get(anyString())).thenReturn(null);

        SampleStorageMovement newer = buildMovement(3000L);
        SampleStorageMovement older = buildMovement(1000L);

        when(sampleStorageService.getSampleStorageMovementsBySampleItem(sampleItem))
                .thenReturn(Arrays.asList(newer, older));

        List<BioSampleLifecycleEventDTO> events = lifecycleService.buildLifecycleEvents(100);
        assertEquals(2, events.size());
        assertEquals(epochIso(1000), events.get(0).getOccurredAt());
        assertEquals(epochIso(3000), events.get(1).getOccurredAt());
    }

    private static String epochIso(long epochMs) {
        return java.time.Instant.ofEpochMilli(epochMs).toString();
    }

    private SampleStorageMovement buildMovement(long epochMs) {
        SampleStorageMovement m = new SampleStorageMovement();
        m.setNewLocationId(1);
        m.setNewLocationType("device");
        m.setMovementDate(new Timestamp(epochMs));
        m.setMovedByUserId(1);
        return m;
    }

    @Test
    public void buildLifecycleEvents_retrievalAndReturn_createSeparateEvents() {
        when(bioSampleService.get(100)).thenReturn(bioSample);
        when(sampleTransferService.getBySampleItemId(10)).thenReturn(Collections.emptyList());
        when(sampleStorageService.getSampleStorageMovementsBySampleItem(sampleItem))
                .thenReturn(Collections.emptyList());
        when(chainOfCustodyService.getBySampleItemId(10)).thenReturn(Collections.emptyList());

        BioSample bsRef = bioSample;

        SampleRetrievalItem ri = new SampleRetrievalItem();
        ri.setBioSample(bsRef);
        ri.setRetrievedTimestamp(new Timestamp(600L));
        ri.setReturnedTimestamp(new Timestamp(900L));
        ri.setStatus(SampleRetrievalItem.ItemStatus.RETURNED);

        SampleRetrievalRequest rr = new SampleRetrievalRequest();
        rr.setRequestNumber("RQ-1");
        rr.setDestinationDetails("Bench 2");
        rr.addItem(ri);

        when(sampleRetrievalService.getByBioSampleId(100)).thenReturn(Collections.singletonList(rr));

        List<BioSampleLifecycleEventDTO> events = lifecycleService.buildLifecycleEvents(100);
        assertEquals(2, events.size());
        assertEquals(BioSampleLifecycleServiceImpl.TYPE_RETRIEVED, events.get(0).getEventType());
        assertEquals(BioSampleLifecycleServiceImpl.TYPE_RETURNED, events.get(1).getEventType());
    }

    @Test
    public void buildLifecycleEvents_includesExistingChainOfCustodyLogs() {
        when(bioSampleService.get(100)).thenReturn(bioSample);
        when(sampleTransferService.getBySampleItemId(10)).thenReturn(Collections.emptyList());
        when(sampleRetrievalService.getByBioSampleId(100)).thenReturn(Collections.emptyList());
        when(sampleStorageService.getSampleStorageMovementsBySampleItem(sampleItem))
                .thenReturn(Collections.emptyList());

        SystemUser custodian = new SystemUser();
        custodian.setId("9");
        custodian.setFirstName("Chain");
        custodian.setLastName("Keeper");

        ChainOfCustodyLog log = new ChainOfCustodyLog();
        log.setSampleItem(sampleItem);
        log.setCustodyAction(CustodyAction.TRANSFER_RECEIVED);
        log.setActionTimestamp(new Timestamp(1200L));
        log.setFromLocation("Lab A");
        log.setToLocation("Biorepository");
        log.setStorageCoordinates("Rack 1 / A1");
        log.setToCustodian(custodian);
        log.setNotes("accepted");

        when(chainOfCustodyService.getBySampleItemId(10)).thenReturn(Collections.singletonList(log));

        List<BioSampleLifecycleEventDTO> events = lifecycleService.buildLifecycleEvents(100);
        assertEquals(1, events.size());
        assertEquals(BioSampleLifecycleServiceImpl.TYPE_TRANSFERRED, events.get(0).getEventType());
        assertEquals("Lab A", events.get(0).getSourceLocation());
        assertEquals("Biorepository · Rack 1 / A1", events.get(0).getDestinationLocation());
        assertEquals("Chain Keeper", events.get(0).getActor());
    }
}
