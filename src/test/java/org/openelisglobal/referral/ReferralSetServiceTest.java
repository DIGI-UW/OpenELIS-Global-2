package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.referral.service.ReferralItemService;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.service.ReferralSetService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;

@WithMockUser(username = "admin", roles = "GLOBAL_ADMIN")
public class ReferralSetServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private ReferralSetService referralSetService;
    @Autowired
    private ReferralItemService referralItemService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ReferralResultService referralResultService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private ReferralService referralService;
    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    // Swap the live FhirReferralService for a mock so we can verify *whether* the
    // FHIR push fires (legacy create path) or doesn't (env/vector draft path)
    // without actually contacting a FHIR store. Captured original is restored in
    // @After. Matches the ReflectionTestUtils pattern already used in
    // ReferralSubcontractDispatchIntegrationTest.
    private FhirReferralService fhirReferralServiceMock;
    private Object originalFhirReferralServiceOnSetService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral-set.xml");
        fhirReferralServiceMock = Mockito.mock(FhirReferralService.class);
        originalFhirReferralServiceOnSetService = ReflectionTestUtils.getField(referralSetService,
                "fhirReferralService");
        ReflectionTestUtils.setField(referralSetService, "fhirReferralService", fhirReferralServiceMock);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(referralSetService, "fhirReferralService",
                originalFhirReferralServiceOnSetService);
    }

    @Test
    public void createSaveReferralSetsSamplePatientEntry_ShouldInsertAReferralSets() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        // Items derived from existing referrals carry a referralId; clearing it
        // expresses the "new referral" intent this test covers (the populated-id
        // branch is exercised by _UpdatesInPlaceInsteadOfDuplicating).
        referralItems.forEach(item -> item.setReferralId(null));
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);
        int initialReferralItemsCount = referralItems.size();
        assertEquals(3, initialReferralItemsCount);

        List<Referral> initialReferrals = referralService.getAll();
        int initialReferralCount = initialReferrals.size();
        assertEquals(3, initialReferralCount);

        List<Result> initialResultsList = resultService.getAllResults();
        int initialResultCount = initialResultsList.size();
        assertEquals(2, initialResultCount);

        List<ReferralResult> initialReferralResults = referralResultService.getAll();
        int initialReferralResultCount = initialReferralResults.size();
        assertEquals(4, initialReferralResultCount);

        referralSetService.createSaveReferralSetsSamplePatientEntry(referralItems, updateData);

        List<Referral> newReferrals = referralService.getAll();
        int newReferralCount = newReferrals.size();
        assertEquals(initialReferralCount + 3, newReferralCount);

        List<Result> newResultsList = resultService.getAllResults();
        int newResultCount = newResultsList.size();
        assertEquals(initialResultCount + 3, newResultCount);

        List<ReferralResult> newReferralResults = referralResultService.getAll();
        int newReferralResultCount = newReferralResults.size();
        assertEquals(initialReferralResultCount + 3, newReferralResultCount);

        List<ReferralItem> newReferralItems = referralItemService.getReferralItems();
        assertEquals(initialReferralItemsCount + 3, newReferralItems.size());
    }

    @Test
    public void createSaveReferralSetsSamplePatientEntry_AttachesSubcontractRowToEveryNewReferral() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        referralItems.forEach(item -> item.setReferralId(null));
        // Populate subcontract metadata on one item; leave the rest at fixture defaults
        // to confirm always-create fires even when the user typed nothing.
        ReferralItem firstItem = referralItems.get(0);
        firstItem.setAgreementReference("AGR-2026-099");
        firstItem.setHandoffDatetime("15/05/2026");
        firstItem.setCocContactName("Chain Custody Officer");
        firstItem.setCocContactPhone("+1-555-9999");
        firstItem.setSubcontractNotes("Integration coverage note.");

        List<Referral> initialReferrals = referralService.getAll();
        long initialWithSubcontract = initialReferrals.stream().filter(r -> r.getSubcontract() != null).count();

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);
        referralSetService.createSaveReferralSetsSamplePatientEntry(referralItems, updateData);

        List<Referral> allReferrals = referralService.getAll();
        long newWithSubcontract = allReferrals.stream().filter(r -> r.getSubcontract() != null)
                .filter(r -> r.getStatus() == ReferralStatus.DRAFT).count();
        assertEquals(initialWithSubcontract + 3, newWithSubcontract);

        Referral populated = allReferrals.stream().filter(
                r -> r.getSubcontract() != null && "AGR-2026-099".equals(r.getSubcontract().getAgreementReference()))
                .findFirst().orElseThrow();
        assertEquals(ReferralStatus.DRAFT, populated.getStatus());
        ReferralSubcontract sc = populated.getSubcontract();
        assertEquals("Chain Custody Officer", sc.getCocContactName());
        assertEquals("+1-555-9999", sc.getCocContactPhone());
        assertEquals("Integration coverage note.", sc.getSubcontractNotes());
        assertEquals(Timestamp.valueOf("2026-05-15 00:00:00"), sc.getHandoffDatetime());
        // Reread through the parent Referral to confirm the cascade-attached row
        // is reachable from a fresh fetch (not just the in-memory instance).
        Referral refetched = referralService.getReferralById(populated.getId());
        assertNotNull(refetched.getSubcontract());
        assertEquals(sc.getId(), refetched.getSubcontract().getId());
        assertEquals("AGR-2026-099", refetched.getSubcontract().getAgreementReference());

        // Sanity: a referral built from a ReferralItem without subcontract metadata
        // still gets an attached subcontract row at DRAFT (always-create semantics).
        long withoutMetadata = allReferrals.stream().filter(r -> r.getSubcontract() != null
                && r.getSubcontract().getAgreementReference() == null && r.getStatus() == ReferralStatus.DRAFT).count();
        assertEquals(2, withoutMetadata);

        // S-14 FR-02 audit-trail seed: each new referral gets one initial history row
        // recording null -> DRAFT, attributed to the operator who saved the order.
        List<ReferralStatusHistory> populatedHistory = statusHistoryDAO
                .findByReferralIdOrderedByChangedAt(populated.getId());
        assertEquals(1, populatedHistory.size());
        ReferralStatusHistory initial = populatedHistory.get(0);
        assertNull(initial.getFromStatus());
        assertEquals(ReferralStatus.DRAFT, initial.getToStatus());
        assertEquals("3901", initial.getChangedByUserId());
        assertNotNull(initial.getChangedAt());
    }

    @Test
    public void createSaveReferralSetsSamplePatientEntry_PreservesAllSubcontractFieldsIncludingHandoffTime() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        referralItems.forEach(item -> item.setReferralId(null));
        ReferralItem firstItem = referralItems.get(0);
        firstItem.setAgreementReference("AGR-2026-TIME");
        // dd/MM/yyyy HH:mm — must persist hour+minute, not truncate to midnight.
        firstItem.setHandoffDatetime("15/05/2026 14:30");
        firstItem.setExpectedReturnDate("20/12/2026");
        firstItem.setCocContactName("Time-Bearing Officer");
        firstItem.setCocContactPhone("+1-555-1430");
        firstItem.setSubcontractNotes("Round-trip coverage including time component.");

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);
        referralSetService.createSaveReferralSetsSamplePatientEntry(referralItems, updateData);

        Referral saved = referralService.getAll().stream().filter(
                r -> r.getSubcontract() != null && "AGR-2026-TIME".equals(r.getSubcontract().getAgreementReference()))
                .findFirst().orElseThrow();
        Referral refetched = referralService.getReferralById(saved.getId());
        ReferralSubcontract sc = refetched.getSubcontract();

        assertNotNull(sc);
        assertEquals(ReferralStatus.DRAFT, refetched.getStatus());
        assertEquals("AGR-2026-TIME", sc.getAgreementReference());
        assertEquals(Timestamp.valueOf("2026-05-15 14:30:00"), sc.getHandoffDatetime());
        assertEquals(java.sql.Date.valueOf("2026-12-20"), sc.getExpectedReturnDate());
        assertEquals("Time-Bearing Officer", sc.getCocContactName());
        assertEquals("+1-555-1430", sc.getCocContactPhone());
        assertEquals("Round-trip coverage including time component.", sc.getSubcontractNotes());

        // Pattern A: display fields are repopulated when Hibernate rehydrates the
        // entity via the typed setter, so a fresh fetch must surface the
        // locale-formatted strings the REST layer serializes.
        assertEquals("15/05/2026 14:30", sc.getHandoffDatetimeForDisplay());
        assertEquals("20/12/2026", sc.getExpectedReturnDateForDisplay());
    }

    @Test
    public void createSaveReferralSetsSamplePatientEntry_WithExistingReferralId_UpdatesInPlaceInsteadOfDuplicating() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        // 1) Initial save: 3 referrals, attach distinguishable subcontract metadata to
        // the first.
        List<ReferralItem> firstSaveItems = referralItemService.getReferralItems();
        firstSaveItems.get(0).setAgreementReference("AGR-EDIT-FLOW");
        firstSaveItems.get(0).setHandoffDatetime("15/05/2026 09:00");
        firstSaveItems.get(0).setCocContactName("Original Contact");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);
        referralSetService.createSaveReferralSetsSamplePatientEntry(firstSaveItems, updateData);

        long referralCountAfterFirstSave = referralService.getAll().size();
        Referral original = referralService.getAll().stream().filter(
                r -> r.getSubcontract() != null && "AGR-EDIT-FLOW".equals(r.getSubcontract().getAgreementReference()))
                .findFirst().orElseThrow();
        String originalReferralId = original.getId();
        String originalSubcontractId = original.getSubcontract().getId();

        // 2) Edit save: frontend re-submits the same ReferralItem with referralId
        // populated
        // and modified subcontract fields. Expectation: update in place, no duplicate.
        ReferralItem editedItem = firstSaveItems.get(0);
        editedItem.setReferralId(originalReferralId);
        editedItem.setAgreementReference("AGR-EDIT-FLOW-UPDATED");
        editedItem.setHandoffDatetime("15/05/2026 14:30");
        editedItem.setCocContactName("Edited Contact");

        referralSetService.createSaveReferralSetsSamplePatientEntry(new ArrayList<>(List.of(editedItem)), updateData);

        // Invariant: editing must not increase the referral row count.
        assertEquals(referralCountAfterFirstSave, referralService.getAll().size());

        // The targeted referral now carries the edited values, and its subcontract
        // is the same row (updated in place), not a freshly inserted replacement.
        Referral refetched = referralService.getReferralById(originalReferralId);
        assertEquals("AGR-EDIT-FLOW-UPDATED", refetched.getSubcontract().getAgreementReference());
        assertEquals(Timestamp.valueOf("2026-05-15 14:30:00"), refetched.getSubcontract().getHandoffDatetime());
        assertEquals("Edited Contact", refetched.getSubcontract().getCocContactName());
        assertEquals(originalSubcontractId, refetched.getSubcontract().getId());
    }

    // -- createDraftReferralSetsForOrderEntry: order-entry Step-3 path (all
    // domains) --

    @Test
    public void createDraftReferralSetsForOrderEntry_persistsReferralWithDraftSubcontractAndInitialHistoryRow() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        referralItems.forEach(item -> item.setReferralId(null));
        ReferralItem first = referralItems.get(0);
        first.setAgreementReference("AGR-ENV-2026-01");
        first.setHandoffDatetime("15/05/2026 09:00");
        first.setExpectedReturnDate("20/12/2026");
        first.setCocContactName("Env CoC Contact");
        first.setCocContactPhone("+1-555-0001");
        first.setSubcontractNotes("Created via env/vector draft path.");

        int initialReferralCount = referralService.getAll().size();
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);

        referralSetService.createDraftReferralSetsForOrderEntry(referralItems, updateData);

        // Three new Referral rows, all carrying a DRAFT subcontract — same as the
        // legacy method's always-create semantics, but without the FHIR push.
        List<Referral> after = referralService.getAll();
        assertEquals(initialReferralCount + 3, after.size());
        Referral populated = after.stream().filter(
                r -> r.getSubcontract() != null && "AGR-ENV-2026-01".equals(r.getSubcontract().getAgreementReference()))
                .findFirst().orElseThrow();
        ReferralSubcontract sc = populated.getSubcontract();
        assertEquals(ReferralStatus.DRAFT, populated.getStatus());
        assertEquals("AGR-ENV-2026-01", sc.getAgreementReference());
        assertEquals(Timestamp.valueOf("2026-05-15 09:00:00"), sc.getHandoffDatetime());
        assertEquals(java.sql.Date.valueOf("2026-12-20"), sc.getExpectedReturnDate());
        assertEquals("Env CoC Contact", sc.getCocContactName());
        assertEquals("+1-555-0001", sc.getCocContactPhone());
        assertEquals("Created via env/vector draft path.", sc.getSubcontractNotes());

        // Initial null -> DRAFT history row is the audit-trail seed (FR-02).
        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt(populated.getId());
        assertEquals(1, history.size());
        ReferralStatusHistory initial = history.get(0);
        assertNull(initial.getFromStatus());
        assertEquals(ReferralStatus.DRAFT, initial.getToStatus());
        assertEquals("3901", initial.getChangedByUserId());
        assertNotNull(initial.getChangedAt());
    }

    /**
     * Order Entry sends no referrer, so this column was empty on every referral it
     * raised and the reference lab had nobody to go back to. The user placing the
     * order is who raised it.
     */
    @Test
    public void createDraftReferralSetsForOrderEntry_recordsTheUserWhoRaisedTheReferral() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        referralItems.forEach(item -> {
            item.setReferralId(null);
            item.setReferrer(null);
        });

        SystemUser orderingUser = systemUserService.getAll().get(0);
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(orderingUser.getId());
        updateData.setSampleItemsTests(new ArrayList<>(List.of(sampleTestCollection)));
        Set<String> existingIds = referralService.getAll().stream().map(Referral::getId).collect(Collectors.toSet());

        referralSetService.createDraftReferralSetsForOrderEntry(referralItems, updateData);

        // Picked out by id: getAll has no guaranteed order, so counting from the end
        // asserts against whichever rows the database happened to return last.
        List<Referral> raised = referralService.getAll().stream().filter(r -> !existingIds.contains(r.getId()))
                .collect(Collectors.toList());
        assertEquals(referralItems.size(), raised.size());
        raised.forEach(referral -> assertEquals("the user who placed the order is on the referral",
                orderingUser.getNameForDisplay(), referral.getRequesterName()));
    }

    @Test
    public void createDraftReferralSetsForOrderEntry_doesNotInvokeFhirReferralService() throws Exception {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        referralItems.forEach(item -> item.setReferralId(null));
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);

        referralSetService.createDraftReferralSetsForOrderEntry(referralItems, updateData);

        // Env/vector draft save MUST NOT push to the FHIR store. The push is deferred
        // to the DRAFT -> DISPATCHED transition (covered by transition tests).
        verify(fhirReferralServiceMock, never()).referAnalysisesToOrganization(any(Referral.class));
    }

    @Test
    public void createDraftReferralSetsForOrderEntry_emptyReferralItems_isNoop() {
        int initialCount = referralService.getAll().size();
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");

        referralSetService.createDraftReferralSetsForOrderEntry(new ArrayList<>(), updateData);

        assertEquals(initialCount, referralService.getAll().size());
    }

    @Test
    public void createDraftReferralSetsForOrderEntry_withExistingReferralId_updatesInPlace() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        // 1) Initial draft save creates 3 Referral rows; populate metadata on one to
        // address it back unambiguously.
        List<ReferralItem> firstSaveItems = referralItemService.getReferralItems();
        firstSaveItems.forEach(item -> item.setReferralId(null));
        firstSaveItems.get(0).setAgreementReference("AGR-EDIT-ENV");
        firstSaveItems.get(0).setCocContactName("Original Env Contact");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);
        referralSetService.createDraftReferralSetsForOrderEntry(firstSaveItems, updateData);

        long countAfterFirstSave = referralService.getAll().size();
        Referral original = referralService.getAll().stream().filter(
                r -> r.getSubcontract() != null && "AGR-EDIT-ENV".equals(r.getSubcontract().getAgreementReference()))
                .findFirst().orElseThrow();
        String originalReferralId = original.getId();
        String originalSubcontractId = original.getSubcontract().getId();

        // 2) Edit save: same Referral with referralId populated + modified fields.
        ReferralItem edited = firstSaveItems.get(0);
        edited.setReferralId(originalReferralId);
        edited.setAgreementReference("AGR-EDIT-ENV-UPDATED");
        edited.setCocContactName("Edited Env Contact");
        referralSetService.createDraftReferralSetsForOrderEntry(new ArrayList<>(List.of(edited)), updateData);

        // Invariant: edit must not duplicate the Referral row, and must reuse the
        // same subcontract row id (update in place, not insert + replace).
        assertEquals(countAfterFirstSave, referralService.getAll().size());
        Referral refetched = referralService.getReferralById(originalReferralId);
        assertEquals("AGR-EDIT-ENV-UPDATED", refetched.getSubcontract().getAgreementReference());
        assertEquals("Edited Env Contact", refetched.getSubcontract().getCocContactName());
        assertEquals(originalSubcontractId, refetched.getSubcontract().getId());
    }

    /**
     * Order Entry raised the referral but left {@code Analysis.referred_out} false,
     * so the same test referred from Result Entry and from Order Entry ended up in
     * two different states: every workload report counted the Order Entry one as
     * this laboratory's own work, and Result Entry showed no referral marker on it.
     *
     * <p>
     * The flag belongs to the one analysis the referral is attached to. The same
     * test can sit on two specimens of one sample, and referring one of them does
     * not make the other the reference laboratory's work, so the two analyses here
     * share a test and only one of them may come back flagged.
     */
    @Test
    public void createDraftReferralSetsForOrderEntry_flagsOnlyTheAnalysisCarryingTheReferral() {
        // Refer a test that is not already flagged, so the flag this test is about
        // is the one the save has to write.
        Analysis target = analysisService.getAll().stream().filter(analysis -> !analysis.isReferredOut()).findFirst()
                .orElseThrow();
        String targetTestId = target.getTest().getId();
        String siblingId = insertSecondAnalysisForSameTest(target);

        List<Analysis> analysesForSave = analysisService.getAll();
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysesForSave;
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        ReferralItem referralItem = referralItemService.getReferralItems().get(0);
        referralItem.setReferralId(null);
        referralItem.setReferredTestId(targetTestId);
        List<ReferralItem> referralItems = new ArrayList<>(List.of(referralItem));

        // Two analyses carry the referred test, so "flag every analysis of that
        // test" and "flag the analysis the referral names" cannot both pass.
        assertEquals(2L,
                analysesForSave.stream().filter(analysis -> targetTestId.equals(analysis.getTest().getId())).count());
        Map<String, Boolean> flaggedBeforeSave = flaggedByAnalysisId();
        assertEquals(false, flaggedBeforeSave.get(target.getId()));
        assertEquals(false, flaggedBeforeSave.get(siblingId));
        Set<String> referralIdsBeforeSave = referralService.getAll().stream().map(Referral::getId)
                .collect(Collectors.toSet());

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);
        referralSetService.createDraftReferralSetsForOrderEntry(referralItems, updateData);

        Map<String, Boolean> flaggedAfterSave = flaggedByAnalysisId();
        List<String> newlyFlagged = flaggedAfterSave.entrySet().stream()
                .filter(entry -> entry.getValue() && !flaggedBeforeSave.getOrDefault(entry.getKey(), false))
                .map(Map.Entry::getKey).collect(Collectors.toList());

        // Exactly the analysis the new referral was attached to, and nothing else.
        List<Referral> created = referralService.getAll().stream()
                .filter(referral -> !referralIdsBeforeSave.contains(referral.getId())).collect(Collectors.toList());
        assertEquals(1, created.size());
        String referredAnalysisId = created.get(0).getAnalysis().getId();
        assertEquals(List.of(referredAnalysisId), newlyFlagged);

        // Every other analysis keeps the flag it had, the untouched sibling included.
        Map<String, Boolean> expectedAfterSave = new HashMap<>(flaggedBeforeSave);
        expectedAfterSave.put(referredAnalysisId, true);
        assertEquals(expectedAfterSave, flaggedAfterSave);
    }

    private Map<String, Boolean> flaggedByAnalysisId() {
        return analysisService.getAll().stream().collect(Collectors.toMap(Analysis::getId, Analysis::isReferredOut));
    }

    /**
     * The same test on a second specimen of the sample, so the save has a choice to
     * get wrong. Returns the new analysis id.
     */
    private String insertSecondAnalysisForSameTest(Analysis target) {
        Analysis sibling = new Analysis();
        sibling.setSampleItem(target.getSampleItem());
        sibling.setTest(target.getTest());
        sibling.setAnalysisType("ROUTINE");
        sibling.setRevision("1");
        sibling.setStartedDate(target.getStartedDate());
        sibling.setStatusId(target.getStatusId());
        sibling.setFhirUuid(UUID.randomUUID());
        sibling.setSysUserId("1");
        return analysisService.insert(sibling);
    }

    @Test
    public void createSaveReferralSetsSamplePatientEntry_stillInvokesFhirReferralService_forLegacyPath()
            throws Exception {
        // Regression guard: the legacy clinical Step-1 entry point must keep pushing
        // every new referral to the FHIR store, exactly as before. The env/vector
        // refactor scoped the change to createDraftReferralSetsForOrderEntry only.
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        referralItems.forEach(item -> item.setReferralId(null));
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);

        referralSetService.createSaveReferralSetsSamplePatientEntry(referralItems, updateData);

        verify(fhirReferralServiceMock, atLeastOnce()).referAnalysisesToOrganization(any(Referral.class));
    }

    private static SampleAddService.SampleTestCollection getSampleTestCollection() {
        SampleAddService sampleAddService = new SampleAddService("xml", "3901", new Sample(), "2024-06-03");
        List<org.openelisglobal.test.valueholder.Test> tests = new ArrayList<>();
        List<ObservationHistory> initialConditionList = new ArrayList<>();
        Map<String, String> testIdToUserSectionMap = new HashMap<>();
        Map<String, String> testIdToUserSampleTypeMap = new HashMap<>();
        return sampleAddService.new SampleTestCollection(new SampleItem(), tests, "2024-06-04", initialConditionList,
                testIdToUserSectionMap, testIdToUserSampleTypeMap, new ObservationHistory());
    }
}
