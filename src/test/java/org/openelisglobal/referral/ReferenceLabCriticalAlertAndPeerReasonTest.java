package org.openelisglobal.referral;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkflowService;
import org.openelisglobal.referral.dto.ReferenceLabReferralDTO;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.referral.service.ReferenceLabResultsService;
import org.openelisglobal.referral.service.ReferenceLabResultsService.DashboardView;
import org.openelisglobal.referral.service.ReferenceLabResultsServiceImpl;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers the reference-range rendering and peer-reason plumbing behind the
 * OGC-803 critical-result alert and the OGC-804 reject reason pre-fill.
 *
 * <p>
 * Referral 1 in {@code testdata/referral.xml} is DRAFT with a subcontract row
 * and a FHIR uuid; the tests drive it to COMPLETED so it lands in the Returned
 * bucket, which is the only view that reads the peer's Task.
 */
public class ReferenceLabCriticalAlertAndPeerReasonTest extends BaseWebContextSensitiveTest {

    private static final String REFERRAL_ID = "1";
    private static final String ACTOR_USER_ID = "1";
    private static final String ORIGINAL_SERVICE_REQUEST_ID = "9f1f2d64-0d2a-4a35-9b0e-2b0a3f5f0a11";

    @Autowired
    private ReferenceLabResultsService referenceLabResultsService;

    @Autowired
    private ReferralService referralService;

    private FhirApiWorkflowService workflowServiceMock;
    private Object originalWorkflowService;
    private Object originalFhirReferralService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        resyncSequence("clinlims.referral_status_history_seq", "clinlims.referral_status_history");

        // Keep the best-effort FHIR publishes off a real store during the lifecycle
        // transitions below.
        originalFhirReferralService = ReflectionTestUtils.getField(referralService, "fhirReferralService");
        ReflectionTestUtils.setField(referralService, "fhirReferralService", Mockito.mock(FhirReferralService.class));

        referralService.dispatchReferral(REFERRAL_ID, java.sql.Timestamp.valueOf("2026-05-15 10:30:00"), ACTOR_USER_ID,
                null);
        referralService.markReferralCompleted(REFERRAL_ID, ACTOR_USER_ID, "Result returned");

        // The Returned view live-reads the peer store. Stand in for it so the test
        // controls exactly what the peer sent, and so the real enrichReturnedResults
        // path runs against it.
        workflowServiceMock = Mockito.mock(FhirApiWorkflowService.class);
        originalWorkflowService = ReflectionTestUtils.getField(referenceLabResultsService, "fhirApiWorkflowService");
        ReflectionTestUtils.setField(referenceLabResultsService, "fhirApiWorkflowService", workflowServiceMock);
    }

    @After
    public void tearDown() {
        if (originalWorkflowService != null) {
            ReflectionTestUtils.setField(referenceLabResultsService, "fhirApiWorkflowService", originalWorkflowService);
        }
        if (originalFhirReferralService != null) {
            ReflectionTestUtils.setField(referralService, "fhirReferralService", originalFhirReferralService);
        }
    }

    // ---------------------------------------------------------------- range

    @Test
    public void referenceRange_rendersStructuredLowAndHighWhenThePeerSendsNoText() {
        Observation.ObservationReferenceRangeComponent rr = new Observation.ObservationReferenceRangeComponent();
        rr.setLow(quantity(3.9));
        rr.setHigh(quantity(5.6));

        Assert.assertEquals("3.9 – 5.6", ReferenceLabResultsServiceImpl.referenceRangeText(rr));
    }

    @Test
    public void referenceRange_prefersThePeersOwnTextWhenPresent() {
        Observation.ObservationReferenceRangeComponent rr = new Observation.ObservationReferenceRangeComponent();
        rr.setText("3.9 to 5.6 mmol/L (fasting)");
        rr.setLow(quantity(3.9));
        rr.setHigh(quantity(5.6));

        Assert.assertEquals("3.9 to 5.6 mmol/L (fasting)", ReferenceLabResultsServiceImpl.referenceRangeText(rr));
    }

    @Test
    public void referenceRange_rendersOneSidedBoundsAndNullWhenThereIsNothingToRender() {
        Observation.ObservationReferenceRangeComponent highOnly = new Observation.ObservationReferenceRangeComponent();
        highOnly.setHigh(quantity(5.6));
        Assert.assertEquals("≤ 5.6", ReferenceLabResultsServiceImpl.referenceRangeText(highOnly));

        Observation.ObservationReferenceRangeComponent lowOnly = new Observation.ObservationReferenceRangeComponent();
        lowOnly.setLow(quantity(3.9));
        Assert.assertEquals("≥ 3.9", ReferenceLabResultsServiceImpl.referenceRangeText(lowOnly));

        Assert.assertNull("an empty range must render nothing rather than an empty string",
                ReferenceLabResultsServiceImpl
                        .referenceRangeText(new Observation.ObservationReferenceRangeComponent()));
    }

    @Test
    public void referenceRange_stripsTrailingZerosSoAWholeNumberDoesNotRenderAsDecimal() {
        Observation.ObservationReferenceRangeComponent rr = new Observation.ObservationReferenceRangeComponent();
        rr.setLow(quantity(4.00));
        rr.setHigh(quantity(10.0));

        Assert.assertEquals("4 – 10", ReferenceLabResultsServiceImpl.referenceRangeText(rr));
    }

    // ----------------------------------------------------------- peer reason

    @Test
    public void returnedView_carriesThePeersStatusReasonText() {
        Mockito.when(workflowServiceMock.fetchReturnedResults(Mockito.any(UUID.class)))
                .thenReturn(Collections.singletonList(importsWithTaskReason(reasonWithText("Hemolyzed"))));

        Assert.assertEquals("Hemolyzed", peerReasonOfReferralOne());
    }

    @Test
    public void returnedView_fallsBackToTheCodingDisplayWhenTheReasonHasNoText() {
        CodeableConcept coded = new CodeableConcept();
        coded.addCoding(new Coding().setCode("clotted").setDisplay("Clotted"));

        Mockito.when(workflowServiceMock.fetchReturnedResults(Mockito.any(UUID.class)))
                .thenReturn(Collections.singletonList(importsWithTaskReason(coded)));

        Assert.assertEquals("Clotted", peerReasonOfReferralOne());
    }

    @Test
    public void returnedView_leavesPeerReasonNullWhenThePeerSentNone() {
        Mockito.when(workflowServiceMock.fetchReturnedResults(Mockito.any(UUID.class)))
                .thenReturn(Collections.singletonList(importsWithTaskReason(null)));

        Assert.assertNull("no statusReason from the peer must leave the modal on its own default",
                peerReasonOfReferralOne());
    }

    @Test
    public void returnedView_leavesPeerReasonNullWhenThePeerReturnedNoTaskAtAll() {
        ReferralResultsImportObjects imports = newImports();
        imports.originalReferralObjects.serviceRequests = Collections.singletonList(originalServiceRequest());
        imports.originalReferralObjects.task = null;

        Mockito.when(workflowServiceMock.fetchReturnedResults(Mockito.any(UUID.class)))
                .thenReturn(Collections.singletonList(imports));

        Assert.assertNull(peerReasonOfReferralOne());
    }

    // ------------------------------------------------------------- helpers

    private String peerReasonOfReferralOne() {
        Assert.assertEquals("precondition: referral 1 must be COMPLETED to appear in Returned",
                ReferralStatus.COMPLETED, referralService.getReferralById(REFERRAL_ID).getStatus());

        List<ReferenceLabReferralDTO> returned = referenceLabResultsService
                .getDashboardReferrals(DashboardView.RETURNED);
        ReferenceLabReferralDTO dto = returned.stream().filter(r -> REFERRAL_ID.equals(r.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("referral " + REFERRAL_ID + " missing from the Returned view"));
        return dto.getPeerReason();
    }

    private static CodeableConcept reasonWithText(String text) {
        return new CodeableConcept().setText(text);
    }

    private static ReferralResultsImportObjects importsWithTaskReason(CodeableConcept reason) {
        ReferralResultsImportObjects imports = newImports();
        imports.originalReferralObjects.serviceRequests = Collections.singletonList(originalServiceRequest());
        Task task = new Task();
        task.setId(UUID.randomUUID().toString());
        if (reason != null) {
            task.setStatusReason(reason);
        }
        imports.originalReferralObjects.task = task;
        return imports;
    }

    /**
     * ReferralResultsImportObjects is a non-static inner of
     * FhirApiWorkFlowServiceImpl, so its synthetic constructor takes the outer
     * instance as the first argument.
     */
    private static ReferralResultsImportObjects newImports() {
        try {
            Constructor<ReferralResultsImportObjects> ctor = ReferralResultsImportObjects.class
                    .getDeclaredConstructor(FhirApiWorkFlowServiceImpl.class);
            ctor.setAccessible(true);
            return ctor.newInstance(new FhirApiWorkFlowServiceImpl());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("could not construct ReferralResultsImportObjects", e);
        }
    }

    private static ServiceRequest originalServiceRequest() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(ORIGINAL_SERVICE_REQUEST_ID);
        return serviceRequest;
    }

    private static Quantity quantity(double value) {
        return new Quantity().setValue(value);
    }
}
