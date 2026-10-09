package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;

/**
 * OGC-1398 (IG Known Issues 2 and 15): every order status gives the order Task
 * a status (R4 requires one; unmapped statuses used to be sent as none), and a
 * finished order's outputs carry the same coded type as referral Tasks.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class OrderTaskStatusAndOutputTest {

    private static final String OE_SYSTEM = "http://openelis-global.org";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private SampleService sampleService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private IStatusService statusService;
    @Mock
    private FhirCommonTransformService common;

    @InjectMocks
    private TaskTransformServiceImpl transformService;

    private Sample sample;

    @Before
    public void setUp() {
        sample = new Sample();
        sample.setId("9002");
        sample.setAccessionNumber("DEV01260000000001398");
        sample.setReferringId(null);
        sample.setFhirUuid(UUID.fromString("5b0e7c1a-1398-4d2f-8a6b-9c3e1f0a2d58"));

        Analysis analysis = new Analysis();
        analysis.setId("52");
        analysis.setFhirUuid(UUID.fromString("7a2d4e6f-1398-4b1c-9e0a-3f5d7c9b1a24"));
        when(sampleService.getAnalysis(sample)).thenReturn(List.of(analysis));
        when(electronicOrderService.getElectronicOrdersByExternalId(any())).thenReturn(List.of());
        when(fhirConfig.getOeFhirSystem()).thenReturn(OE_SYSTEM);
        when(common.createReferenceFor(any(ResourceType.class), anyString()))
                .thenAnswer(call -> new Reference(call.getArgument(0) + "/" + call.getArgument(1)));

        when(statusService.getStatusID(OrderStatus.Entered)).thenReturn("1");
        when(statusService.getStatusID(OrderStatus.Started)).thenReturn("2");
        when(statusService.getStatusID(OrderStatus.Finished)).thenReturn("3");
        when(statusService.getStatusID(OrderStatus.NonConforming_depricated)).thenReturn("12");
        when(statusService.getStatusID(SampleStatus.Canceled)).thenReturn("19");
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("20");
        when(statusService.getStatusID(SampleStatus.SampleRejected)).thenReturn("27");
        when(statusService.getStatusID(SampleStatus.Disposed)).thenReturn("28");
    }

    private TaskStatus statusFor(String statusId) {
        sample.setStatusId(statusId);
        return transformService.transformToTask(sample).getStatus();
    }

    @Test
    public void anOrderEnteredBeforeItsTestsAreAddedIsReady() {
        assertEquals(TaskStatus.READY, statusFor("20"));
    }

    @Test
    public void aCancelledOrderIsCancelled() {
        assertEquals(TaskStatus.CANCELLED, statusFor("19"));
    }

    @Test
    public void aRejectedSampleIsRejected() {
        assertEquals(TaskStatus.REJECTED, statusFor("27"));
    }

    @Test
    public void anUnmappedStatusFallsBackToInProgress() {
        assertEquals(TaskStatus.INPROGRESS, statusFor("28"));
        assertEquals(TaskStatus.INPROGRESS, statusFor("999"));
    }

    @Test
    public void anOrderWithoutAStatusStillGetsOne() {
        assertEquals(TaskStatus.INPROGRESS, statusFor(null));
    }

    @Test
    public void theStatusesMappedBeforeAreUnchanged() {
        assertEquals(TaskStatus.READY, statusFor("1"));
        assertEquals(TaskStatus.INPROGRESS, statusFor("2"));
        assertEquals(TaskStatus.REJECTED, statusFor("12"));
        assertEquals(TaskStatus.COMPLETED, statusFor("3"));
    }

    @Test
    public void aFinishedOrdersOutputsUseTheReferralTaskOutputType() {
        sample.setStatusId("3");

        Task task = transformService.transformToTask(sample);

        assertEquals(1, task.getOutput().size());
        Coding type = task.getOutputFirstRep().getType().getCodingFirstRep();
        assertEquals(OE_SYSTEM + "/task_output", type.getSystem());
        assertEquals("DiagnosticReport", type.getCode());
        assertTrue(task.getOutputFirstRep().getValue() instanceof Reference);
        assertEquals("DiagnosticReport/7a2d4e6f-1398-4b1c-9e0a-3f5d7c9b1a24",
                ((Reference) task.getOutputFirstRep().getValue()).getReference());
    }
}
