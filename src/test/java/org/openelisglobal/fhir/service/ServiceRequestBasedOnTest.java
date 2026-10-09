package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisAnchorService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1396 (IG Known Issue 22): an order imported from an EMR keeps the EMR
 * ServiceRequest's order number (its first identifier) as the sample's
 * referring id, not the request's id. basedOn was written as
 * {@code ServiceRequest/<order number>}, which never resolves. It now points at
 * the EMR request by id, found through its local copy, and falls back to a
 * logical reference by the order number.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ServiceRequestBasedOnTest {

    private static final String ORDER_NUMBER = "ORD-1396";
    private static final String EMR_REQUEST_ID = "6f1c9d2e-1396-4e0b-9a51-2f8e7c0d1b34";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private FhirPersistanceService fhirPersistanceService;
    @Mock
    private AnalysisAnchorService analysisAnchorService;
    @Mock
    private IStatusService statusService;
    @Mock
    private FhirCommonTransformService common;
    @Mock
    private org.openelisglobal.samplehuman.service.SampleHumanService sampleHumanService;
    @Mock
    private org.openelisglobal.sample.service.SampleService sampleService;
    @Mock
    private org.openelisglobal.observationhistory.service.ObservationHistoryService observationHistoryService;
    @Mock
    private org.openelisglobal.note.service.NoteService noteService;
    @Mock
    private TerminologyTransformService terminologyTransformService;

    @InjectMocks
    private ServiceRequestTransformServiceImpl transformService;

    private Analysis analysis;
    private TableIdService tableIdsBefore;

    @Before
    public void setUp() {
        tableIdsBefore = TableIdService.getInstance();
        if (tableIdsBefore == null) {
            ReflectionTestUtils.setField(TableIdService.class, "INSTANCE", new TableIdService());
        }
        Sample sample = new Sample();
        sample.setId("9001");
        sample.setAccessionNumber("DEV01260000000001396");
        sample.setReferringId(ORDER_NUMBER);

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("7");
        analysis = new Analysis();
        analysis.setId("51");
        analysis.setFhirUuid(UUID.fromString("0d6c2a41-1396-4a7e-8f0b-3c5d9e1a2b47"));
        analysis.setStatusId("4");
        analysis.setTest(test);

        when(analysisAnchorService.resolveSample(analysis)).thenReturn(sample);
        when(statusService.getStatusID(any(AnalysisStatus.class))).thenReturn("-1");
        when(common.createReferenceFor(any(ResourceType.class), anyString()))
                .thenAnswer(call -> new Reference(call.getArgument(0) + "/" + call.getArgument(1)));
    }

    @After
    public void restoreTableIds() {
        ReflectionTestUtils.setField(TableIdService.class, "INSTANCE", tableIdsBefore);
    }

    private void importedFrom(ElectronicOrderType type) {
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId(ORDER_NUMBER);
        eOrder.setType(type);
        when(electronicOrderService.getElectronicOrdersByExternalId(ORDER_NUMBER)).thenReturn(List.of(eOrder));
    }

    @Test
    public void basedOnPointsAtTheEmrServiceRequestById() {
        importedFrom(ElectronicOrderType.FHIR);
        ServiceRequest emrRequest = new ServiceRequest();
        emrRequest.setId(EMR_REQUEST_ID);
        when(fhirPersistanceService.getServiceRequestByReferingId(ORDER_NUMBER)).thenReturn(Optional.of(emrRequest));

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals(1, serviceRequest.getBasedOn().size());
        assertEquals("ServiceRequest/" + EMR_REQUEST_ID, serviceRequest.getBasedOnFirstRep().getReference());
    }

    @Test
    public void basedOnFallsBackToTheOrderNumberWhenTheEmrRequestIsNotStored() {
        importedFrom(ElectronicOrderType.FHIR);
        when(fhirPersistanceService.getServiceRequestByReferingId(ORDER_NUMBER)).thenReturn(Optional.empty());

        Reference basedOn = transformService.transformToServiceRequest(analysis).getBasedOnFirstRep();

        assertNull(basedOn.getReference());
        assertEquals("ServiceRequest", basedOn.getType());
        assertEquals(ORDER_NUMBER, basedOn.getIdentifier().getValue());
    }

    @Test
    public void basedOnFallsBackToTheOrderNumberWhenTheFhirStoreIsUnreachable() {
        importedFrom(ElectronicOrderType.FHIR);
        when(fhirPersistanceService.getServiceRequestByReferingId(ORDER_NUMBER))
                .thenThrow(new FhirClientConnectionException("store down"));

        Reference basedOn = transformService.transformToServiceRequest(analysis).getBasedOnFirstRep();

        assertNull(basedOn.getReference());
        assertEquals(ORDER_NUMBER, basedOn.getIdentifier().getValue());
    }

    @Test
    public void anHl7OrderHasNoBasedOn() {
        importedFrom(ElectronicOrderType.HL7_V2);

        assertFalse(transformService.transformToServiceRequest(analysis).hasBasedOn());
    }
}
