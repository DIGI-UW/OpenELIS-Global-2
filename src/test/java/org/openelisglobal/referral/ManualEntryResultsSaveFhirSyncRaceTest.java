package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.referral.fhir.service.FhirReferralServiceImpl;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration coverage for the OGC-799 manual-entry FHIR-sync race:
 *
 * <p>
 * When the user saves a result via Result Entry for an Outstanding referral,
 * the controller runs two FHIR pushes back-to-back —
 *
 * <ol>
 * <li>{@code logbookPersistService.persistDataSet(...)} fires the OGC-799 hook
 * ({@code advanceReferralsForManualEntry → publishManualEntryCompletion}) which
 * PUTs the ServiceRequest to {@code status=completed}.</li>
 * <li>{@code fhirTransformService.transformPersistResultsEntryFhirObjects(actionDataSet)}
 * (LogbookResultsController:445) then rebuilds the ServiceRequest from the
 * local {@code Analysis.status_id} and pushes it again. Because manual Result
 * Entry doesn't advance the Analysis past {@code NotStarted},
 * {@code transformToServiceRequest} maps to {@code SR.status=active} and
 * overwrites the {@code completed} written in step 1.</li>
 * </ol>
 *
 * <p>
 * Verified in production via the FHIR store's _history endpoint:
 * 
 * <pre>
 *   v3  06:31:20.380  status=completed   ← OGC-799 hook
 *   v4  06:31:20.667  status=active      ← transformPersistResultsEntryFhirObjects (287ms later)
 * </pre>
 *
 * <p>
 * This test reproduces that race using a Map-backed simulated FHIR store. It
 * fails today because the final SR.status is {@code active}; it passes after
 * the fix (advancing the Analysis to {@code Finalized} inside the manual-entry
 * hook so both pushes agree on {@code completed}).
 */
public class ManualEntryResultsSaveFhirSyncRaceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralService referralService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    // System under test: instantiated manually because AppTestConfig replaces
    // the FhirReferralService Spring bean with a Mockito mock. We want the REAL
    // publishManualEntryCompletion code path against captured FHIR writes.
    private FhirReferralServiceImpl fhirReferralServiceUnderTest;

    private FhirPersistanceService capturingFhirStore;
    private Map<String, Resource> simulatedFhirStore;
    private Object originalFhirPersistanceServiceOnTransform;
    private Object originalFhirReferralServiceOnReferralService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");

        // The fixture only seeds Sample-domain status_of_sample rows; this test needs
        // ANALYSIS-domain rows so IStatusService can map AnalysisStatus.Finalized for
        // the OGC-799 hook's Analysis advancement. Codes/names mirror the production
        // Liquibase seed.
        seedAnalysisStatusIfMissing(4, 4, "Not Tested");
        seedAnalysisStatusIfMissing(6, 6, "Finalized");
        statusService.refreshCache();
        // Hard precondition: the test depends on AnalysisStatus.Finalized resolving to
        // status_of_sample.id=6. Bail loudly if it doesn't.
        String resolvedFinalizedId = statusService.getStatusID(AnalysisStatus.Finalized);
        assertEquals("AnalysisStatus.Finalized must resolve to the seeded status_of_sample.id=6", "6",
                resolvedFinalizedId);

        // Map-backed simulated FHIR store. Both push APIs land entries here so the
        // test can inspect the *final* state after both writers have run.
        simulatedFhirStore = new ConcurrentHashMap<>();
        capturingFhirStore = mock(FhirPersistanceService.class);

        // updateFhirResourcesInFhirStore(Map<String, Resource>) — used by
        // publishManualEntryCompletion
        when(capturingFhirStore.updateFhirResourcesInFhirStore(any())).thenAnswer(inv -> {
            Map<String, Resource> updates = inv.getArgument(0);
            for (Resource r : updates.values()) {
                simulatedFhirStore.put(typedKey(r), r);
            }
            return new Bundle();
        });

        // createUpdateFhirResourcesInFhirStore(FhirOperations) — used by
        // transformPersistResultsEntryFhirObjects.
        // FhirOperations.{create,update}Resources are package-private fields so we read
        // them via reflection.
        when(capturingFhirStore.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class))).thenAnswer(inv -> {
            FhirOperations ops = inv.getArgument(0);
            applyFhirOperationsToSimulatedStore(ops);
            return new Bundle();
        });

        // Reads needed by publishManualEntryCompletion when it fetches the existing SR
        when(capturingFhirStore.getServiceRequestByAnalysisUuid(anyString())).thenAnswer(inv -> {
            Resource r = simulatedFhirStore.get("ServiceRequest/" + inv.getArgument(0));
            return r == null ? Optional.empty() : Optional.of((ServiceRequest) r);
        });
        org.hl7.fhir.r4.model.Organization stubOrg = new org.hl7.fhir.r4.model.Organization();
        stubOrg.setId("stub-fhir-org");
        when(capturingFhirStore.getFhirOrganizationByName(anyString())).thenReturn(Optional.of(stubOrg));
        when(capturingFhirStore.getPatientByUuid(anyString())).thenReturn(Optional.empty());

        // Swap the capturing mock onto the live Spring FhirTransformService bean so its
        // post-persistDataSet push (createUpdateFhirResourcesInFhirStore) hits our
        // store.
        originalFhirPersistanceServiceOnTransform = ReflectionTestUtils.getField(fhirTransformService,
                "fhirPersistanceService");
        ReflectionTestUtils.setField(fhirTransformService, "fhirPersistanceService", capturingFhirStore);

        // Build a real FhirReferralServiceImpl with Spring-managed collaborators, then
        // override its FhirPersistanceService with our capturing mock.
        fhirReferralServiceUnderTest = new FhirReferralServiceImpl();
        beanFactory.autowireBean(fhirReferralServiceUnderTest);
        ReflectionTestUtils.setField(fhirReferralServiceUnderTest, "fhirPersistanceService", capturingFhirStore);

        // Swap the FhirReferralService dependency on the Spring ReferralService bean
        // with our test instance so that
        // referralService.markReferralCompletedFromManualEntry
        // routes its FHIR push through our capturing pipeline.
        originalFhirReferralServiceOnReferralService = ReflectionTestUtils.getField(referralService,
                "fhirReferralService");
        ReflectionTestUtils.setField(referralService, "fhirReferralService", fhirReferralServiceUnderTest);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(fhirTransformService, "fhirPersistanceService",
                originalFhirPersistanceServiceOnTransform);
        ReflectionTestUtils.setField(referralService, "fhirReferralService",
                originalFhirReferralServiceOnReferralService);
    }

    @Test
    public void manualEntry_finalServiceRequestStatusInFhirStore_isCompletedAfterBothWriters() throws Exception {
        // GIVEN a Referral that's been dispatched and is awaiting acceptance.
        // The Analysis is whatever the fixture left it at — crucially NOT Finalized,
        // which is the only AnalysisStatus that maps to SR.status=completed in
        // transformToServiceRequest. Anything else (NotStarted in production,
        // unmapped in this fixture) means the second push will downgrade the SR.
        // Ensure analysis 1 has a fhir_uuid (the fixture doesn't set one, but
        // publishManualEntryCompletion early-returns on a null analysis fhir_uuid
        // since there's no SR to address).
        Analysis seedAnalysis = analysisService.get("1");
        if (seedAnalysis.getFhirUuid() == null) {
            seedAnalysis.setFhirUuid(UUID.randomUUID());
            seedAnalysis.setSysUserId("1");
            analysisService.update(seedAnalysis);
        }

        referralService.dispatchReferral("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", "dispatched");
        referralService.markReferralReceived("1", "42", "peer received");

        // Capture the fixture's pre-hook Analysis state so we can guard against false
        // positives (Analysis already Finalized → both pushes would agree on completed
        // regardless of the bug).
        TransactionTemplate tx = new TransactionTemplate(txManager);
        String analysisFhirUuid = tx.execute(status -> {
            Referral referral = referralService.getReferralById("1");
            Analysis analysis = referral.getAnalysis();
            String finalizedId = statusService.getStatusID(AnalysisStatus.Finalized);
            if (finalizedId != null && finalizedId.equals(analysis.getStatusId())) {
                fail("Test precondition violated: fixture Analysis is already at Finalized, "
                        + "which would make both FHIR pushes agree on SR.status=completed and hide the bug.");
            }
            String fhirUuid = analysis.getFhirUuidAsString();
            assertNotNull("analysis must have a fhir_uuid for the SR to be addressable", fhirUuid);
            return fhirUuid;
        });

        // STEP 1 — Drive the full OGC-799 manual-entry hook. This is what
        // LogbookPersistService.advanceReferralsForManualEntry calls when the user
        // saves a result via Result Entry. The fix lives here: the method must
        // advance the local Analysis to Finalized so that the post-persistDataSet
        // FHIR sync in step 2 produces a consistent SR.status=completed.
        // markReferralCompletedFromManualEntry is REQUIRES_NEW; it commits on its
        // own and routes the FHIR push through our test instance of
        // FhirReferralServiceImpl (swapped onto referralService in @Before).
        referralService.markReferralCompletedFromManualEntry("1", "55");

        // Intermediate sanity-check: confirm step 1 actually wrote SR=completed
        // before we assert the final state. If this fails, the test setup is broken
        // (not the bug).
        ServiceRequest afterStep1 = (ServiceRequest) simulatedFhirStore.get("ServiceRequest/" + analysisFhirUuid);
        assertNotNull("Step 1 (OGC-799 hook) must push the ServiceRequest to FHIR", afterStep1);
        assertEquals("Step 1 (OGC-799 hook) must set SR.status=completed", ServiceRequestStatus.COMPLETED,
                afterStep1.getStatus());
        Referral referralRecheck = referralService.getReferralById("1");
        Task taskAfterStep1 = (Task) simulatedFhirStore.get("Task/" + referralRecheck.getFhirUuidAsString());
        assertNotNull("Step 1 must also push the Task", taskAfterStep1);
        assertEquals("Task.status must be completed from step 1", TaskStatus.COMPLETED, taskAfterStep1.getStatus());

        // STEP 2 — LogbookResultsController:445 runs the existing FHIR sync that
        // rebuilds the SR from Analysis.status (NotStarted → SR.status=active) and
        // PUTs it, overwriting step 1's completed.
        tx.executeWithoutResult(status -> {
            Analysis analysis = referralService.getReferralById("1").getAnalysis();
            ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet("55");
            actionDataSet.getModifiedAnalysis().add(analysis);
            try {
                fhirTransformService.transformPersistResultsEntryFhirObjects(actionDataSet);
            } catch (FhirTransformationException | FhirPersistanceException e) {
                fail("transformPersistResultsEntryFhirObjects threw unexpectedly: " + e.getMessage());
            }
        });

        // THEN the FHIR store's view of the ServiceRequest must still be completed.
        // BUG: today the second push overwrites step 1 with status=active because
        // transformToServiceRequest derives status from Analysis.status (NotStarted).
        ServiceRequest finalSr = (ServiceRequest) simulatedFhirStore.get("ServiceRequest/" + analysisFhirUuid);
        assertNotNull("ServiceRequest must still be in the FHIR store after both pushes", finalSr);
        assertEquals("After manual Result Entry, the ServiceRequest in FHIR must remain status=completed. "
                + "BUG: transformPersistResultsEntryFhirObjects rebuilds SR from Analysis.status (NotStarted) and "
                + "downgrades SR.status to active, clobbering the OGC-799 manual-entry hook's completed write. "
                + "Fix candidates: (a) advance Analysis to Finalized inside the manual-entry hook so both pushes "
                + "agree, or (b) make transformToServiceRequest aware that completed SRs must not be downgraded.",
                ServiceRequestStatus.COMPLETED, finalSr.getStatus());

        // Sentinel: Task must NOT be downgraded by step 2 (the bug is SR-specific —
        // transformPersistResultsEntryFhirObjects doesn't touch Task). If this ever
        // starts failing, the bug surface has expanded and the diagnosis must be
        // re-checked before re-running the fix.
        Task finalTask = (Task) simulatedFhirStore.get("Task/" + referralRecheck.getFhirUuidAsString());
        assertNotNull(finalTask);
        assertEquals("Task.status must survive step 2 unchanged (it isn't part of the existing sync's resource set)",
                TaskStatus.COMPLETED, finalTask.getStatus());
    }

    private String typedKey(Resource r) {
        return r.getResourceType().name() + "/" + r.getIdElement().getIdPart();
    }

    private void seedAnalysisStatusIfMissing(int id, int code, String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.status_of_sample WHERE id = ?",
                Integer.class, id);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO clinlims.status_of_sample "
                    + "(id, code, status_type, name, description, lastupdated) "
                    + "VALUES (?, ?, 'ANALYSIS', ?, ?, now())", id, code, name, name);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyFhirOperationsToSimulatedStore(FhirOperations ops) {
        // FhirOperations.{create,update}Resources are package-private; reach them via
        // reflection.
        Map<String, Resource> updateResources = (Map<String, Resource>) ReflectionTestUtils.getField(ops,
                "updateResources");
        Map<String, Resource> createResources = (Map<String, Resource>) ReflectionTestUtils.getField(ops,
                "createResources");
        if (updateResources != null) {
            for (Resource r : updateResources.values()) {
                simulatedFhirStore.put(typedKey(r), r);
            }
        }
        if (createResources != null) {
            for (Resource r : createResources.values()) {
                // createResources get a fresh UUID in production; mirror that here so the
                // simulated store reflects how the resource ends up addressable.
                if (r.getIdElement().isEmpty()) {
                    r.setId(UUID.randomUUID().toString());
                }
                simulatedFhirStore.put(typedKey(r), r);
            }
        }
    }
}
