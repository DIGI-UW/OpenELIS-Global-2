package org.openelisglobal.biorepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleResponseDTO;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleStateDTO;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.ChainOfCustodyService;
import org.openelisglobal.biorepository.service.SampleLifecycleService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for lifecycle timeline/state assembly.
 */
public class SampleLifecycleServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleLifecycleService lifecycleService;

    @Autowired
    private ChainOfCustodyService custodyService;

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

    private SystemUser testUser;
    private TypeOfSample testSampleType;
    private Sample testSample;

    @Before
    public void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        testUser = new SystemUser();
        testUser.setLoginName("lcs_" + suffix);
        testUser.setFirstName("Lifecycle" + suffix);
        testUser.setLastName("Tester");
        testUser.setIsActive("Y");
        testUser.setIsEmployee("Y");
        testUser.setSysUserId("1");
        testUser = systemUserService.save(testUser);

        List<TypeOfSample> sampleTypes = typeOfSampleService.getAll();
        testSampleType = sampleTypes.isEmpty() ? createSampleType("Serum") : sampleTypes.get(0);
        testSample = createSample("LCS" + (System.currentTimeMillis() % 100000000));
    }

    @Test
    public void getBySampleItemId_coversEightLifecycleScenarios() {
        BioSample bioSample = createBioSampleWithStatus("LCS-8-" + System.currentTimeMillis(), WorkflowStatus.STORED);
        Integer sampleItemId = Integer.valueOf(bioSample.getSampleItem().getId());

        List<CustodyAction> expectedActions = Arrays.asList(
                CustodyAction.TRANSFER_INITIATED,
                CustodyAction.TRANSFER_RECEIVED,
                CustodyAction.STORAGE_ASSIGNED,
                CustodyAction.CHECKOUT_REQUESTED,
                CustodyAction.CHECKOUT_APPROVED,
                CustodyAction.CHECKOUT_RETRIEVED,
                CustodyAction.RETURN_RECEIVED,
                CustodyAction.RETURN_STORED);

        int sourceRecordId = 100;
        for (CustodyAction action : expectedActions) {
            custodyService.logCustodyAction(
                    bioSample.getSampleItem(),
                    action,
                    null,
                    null,
                    "Freezer-A > Shelf-1 > Rack-A > Box-1 > A1",
                    testUser,
                    "From-" + action.name(),
                    "To-" + action.name(),
                    null,
                    "scenario=" + action.name(),
                    testUser.getId(),
                    "LifecycleScenario",
                    sourceRecordId++,
                    "IN_USE",
                    "PENDING_STORAGE");
        }

        SampleLifecycleResponseDTO response = lifecycleService.getBySampleItemId(sampleItemId);

        assertNotNull(response);
        assertNotNull(response.getEvents());
        List<String> returnedActions = response.getEvents().stream()
                .map(event -> event.getCustodyAction())
                .collect(Collectors.toList());

        for (CustodyAction expectedAction : expectedActions) {
            assertTrue("Missing lifecycle action " + expectedAction.name(),
                    returnedActions.contains(expectedAction.name()));
        }
    }

    @Test
    public void getBySampleItemId_setsAwaitingRestorageAfterReturnReceived() {
        BioSample bioSample = createBioSampleWithStatus("LCS-RET-" + System.currentTimeMillis(),
                WorkflowStatus.PENDING_STORAGE);
        Integer sampleItemId = Integer.valueOf(bioSample.getSampleItem().getId());

        custodyService.logCustodyAction(
                bioSample.getSampleItem(),
                CustodyAction.RETURN_RECEIVED,
                null,
                null,
                "Biorepository Intake",
                testUser,
                "Research Unit",
                "Biorepository Custody",
                null,
                "Returned and awaiting physical re-storage",
                testUser.getId(),
                "SampleRetrievalItem",
                200,
                WorkflowStatus.IN_USE.name(),
                WorkflowStatus.PENDING_STORAGE.name());

        SampleLifecycleResponseDTO response = lifecycleService.getBySampleItemId(sampleItemId);
        SampleLifecycleStateDTO currentState = response.getCurrentState();

        assertNotNull(currentState);
        assertEquals("PENDING_STORAGE", currentState.getWorkflowStatus());
        assertTrue(currentState.isAwaitingRestorage());
    }

    @Test
    public void getBySampleItemId_clearsAwaitingRestorageAfterReturnStored() {
        BioSample bioSample = createBioSampleWithStatus("LCS-STORED-" + System.currentTimeMillis(),
                WorkflowStatus.STORED);
        Integer sampleItemId = Integer.valueOf(bioSample.getSampleItem().getId());

        custodyService.logCustodyAction(
                bioSample.getSampleItem(),
                CustodyAction.RETURN_STORED,
                null,
                null,
                "Freezer-B > Shelf-2 > Rack-C > Box-4 > B2",
                testUser,
                "Biorepository Custody",
                "Freezer-B > Shelf-2 > Rack-C > Box-4 > B2",
                null,
                "Physically re-stored",
                testUser.getId(),
                "SampleStorageMovement",
                300,
                WorkflowStatus.PENDING_STORAGE.name(),
                WorkflowStatus.STORED.name());

        SampleLifecycleResponseDTO response = lifecycleService.getBySampleItemId(sampleItemId);
        SampleLifecycleStateDTO currentState = response.getCurrentState();

        assertNotNull(currentState);
        assertEquals("STORED", currentState.getWorkflowStatus());
        assertFalse(currentState.isAwaitingRestorage());
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
        sample.setSysUserId(testUser.getId());
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
        item.setSysUserId(testUser.getId());
        return sampleItemService.save(item);
    }

    private BioSample createBioSampleWithStatus(String externalId, WorkflowStatus status) {
        SampleItem sampleItem = createSampleItem(externalId);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setWorkflowStatus(status);
        bioSample.setSysUserId(testUser.getId());
        return bioSampleService.createForSampleItem(sampleItem, bioSample);
    }
}
