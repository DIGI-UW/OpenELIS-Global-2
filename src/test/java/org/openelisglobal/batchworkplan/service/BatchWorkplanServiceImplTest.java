package org.openelisglobal.batchworkplan.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTestCapability;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanItemDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcReleaseDAO;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.BatchReagentRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.ControlLotQcResponse;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.form.QcRunResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanQcRelease;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.service.NceTypeService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCFrequencyType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;

@RunWith(MockitoJUnitRunner.class)
public class BatchWorkplanServiceImplTest {

    @Mock
    private BatchWorkplanDAO batchWorkplanDAO;

    @Mock
    private BatchWorkplanItemDAO batchWorkplanItemDAO;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private IStatusService statusService;

    @Mock
    private UserService userService;

    @Mock
    private BatchWorkplanQcService qcService;

    @Mock
    private BatchWorkplanQcReleaseDAO releaseDAO;

    @Mock
    private TestReagentLinkService testReagentLinkService;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private NCEventService ncEventService;

    @Mock
    private SystemUserService systemUserService;

    @Mock
    private QCControlLotService controlLotService;

    @Mock
    private NceCategoryService nceCategoryService;

    @Mock
    private NceTypeService nceTypeService;

    @Mock
    private NceSpecimenService nceSpecimenService;

    @InjectMocks
    private BatchWorkplanServiceImpl service;

    @Before
    public void setUp() {
        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("1");
        when(statusService.getStatusID(AnalysisStatus.BiologistRejected)).thenReturn("2");
        when(statusService.getStatusID(AnalysisStatus.TechnicalRejected)).thenReturn("3");
        when(statusService.getStatusID(AnalysisStatus.NonConforming_depricated)).thenReturn("4");
        when(statusService.getStatusNameFromId(any())).thenReturn("Not started");
        when(statusService.matches(any(), eq(AnalysisStatus.NonConforming_depricated))).thenReturn(false);
    }

    @Test
    public void createBatch_persistsDraftWithItems() {
        BatchWorkplanRequest request = new BatchWorkplanRequest();
        request.setName("Morning chemistry");
        request.setAnalysisIds(Arrays.asList("11", "12"));

        when(batchWorkplanItemDAO.getExistingAnalysisIds(anyList(), anyList())).thenReturn(Collections.emptySet());
        List<Analysis> requested = Arrays.asList(analysis("11", "A-001"), analysis("12", "A-002"));
        when(analysisService.getAnalysesByIdsWithDetails(Arrays.asList("11", "12"))).thenReturn(requested);
        when(userService.filterAnalysesByLabUnitRoles("42", requested, Constants.ROLE_RESULTS)).thenReturn(requested);
        when(batchWorkplanDAO.insert(any(BatchWorkplan.class))).thenAnswer(invocation -> {
            BatchWorkplan batch = invocation.getArgument(0);
            batch.setId(7L);
            return 7L;
        });
        when(batchWorkplanDAO.getWithItems(7L)).thenReturn(Optional.empty());

        BatchWorkplanResponse response = service.createBatch(request, "42");

        ArgumentCaptor<BatchWorkplan> captor = ArgumentCaptor.forClass(BatchWorkplan.class);
        verify(batchWorkplanDAO).insert(captor.capture());
        BatchWorkplan saved = captor.getValue();
        assertEquals(BatchWorkplanStatus.DRAFT, saved.getStatus());
        assertEquals("Morning chemistry", saved.getName());
        assertEquals(Integer.valueOf(42), saved.getCreatedByUserId());
        assertEquals(2, saved.getItems().size());
        assertEquals("11", saved.getItems().get(0).getAnalysisId());
        assertEquals(Integer.valueOf(1), saved.getItems().get(0).getSortOrder());
        assertEquals(Long.valueOf(7L), response.getId());
        assertEquals(2, response.getItemCount().intValue());
    }

    @Test
    public void transitionBatch_activeToCompleted_setsCompletedTimestamp() {
        BatchWorkplan batch = new BatchWorkplan();
        batch.setId(8L);
        batch.setName("Active batch");
        batch.setStatus(BatchWorkplanStatus.ACTIVE);
        batch.setCreatedByUserId(42);
        batch.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        BatchWorkplanItem item = new BatchWorkplanItem();
        item.setAnalysisId("11");
        item.setSortOrder(1);
        batch.addItem(item);

        when(batchWorkplanDAO.getWithItems(8L)).thenReturn(Optional.of(batch));
        when(batchWorkplanDAO.update(batch)).thenReturn(batch);
        when(analysisService.getAnalysesByIdsWithDetails(Collections.singletonList("11")))
                .thenReturn(Collections.singletonList(analysis("11", "A-001")));

        BatchWorkplanResponse response = service.transitionBatch(8L, BatchWorkplanStatus.COMPLETED, "42");

        assertEquals(BatchWorkplanStatus.COMPLETED, batch.getStatus());
        assertNotNull(batch.getCompletedAt());
        assertEquals(BatchWorkplanStatus.COMPLETED, response.getStatus());
        verify(batchWorkplanDAO).update(batch);
    }

    @Test
    public void transitionBatch_rejectsInvalidLifecycleJump() {
        BatchWorkplan batch = new BatchWorkplan();
        batch.setId(9L);
        batch.setStatus(BatchWorkplanStatus.DRAFT);
        batch.setCreatedByUserId(42);
        when(batchWorkplanDAO.getWithItems(9L)).thenReturn(Optional.of(batch));

        try {
            service.transitionBatch(9L, BatchWorkplanStatus.COMPLETED, "42");
            fail("Expected invalid lifecycle transition to fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("Cannot transition batch workplan from DRAFT to COMPLETED", expected.getMessage());
        }
    }

    @Test
    public void getPendingTests_pushesBothExclusionsIntoTheQueryAheadOfTheCap() {
        Set<String> batched = new HashSet<>(Arrays.asList("11", "12"));
        when(batchWorkplanItemDAO.getAnalysisIdsInStatuses(anyList())).thenReturn(batched);
        when(userService.getUserTestIdsForLabUnitRoles("42", Constants.ROLE_RESULTS))
                .thenReturn(Arrays.asList("300", "301"));
        when(analysisService.getPendingAnalysesForWorkplan(anyList(), anyList(), any(), anyInt()))
                .thenReturn(Collections.singletonList(analysis("13", "A-003")));

        List<PendingBatchTestResponse> pending = service.getPendingTests(25, "42");

        ArgumentCaptor<List<String>> testIds = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Collection<String>> excluded = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Integer> cap = ArgumentCaptor.forClass(Integer.class);
        verify(analysisService).getPendingAnalysesForWorkplan(anyList(), testIds.capture(), excluded.capture(),
                cap.capture());

        // The cap the DAO receives must be the caller's page size, and both
        // exclusions must reach the query rather than being applied to its output.
        assertEquals(Arrays.asList("300", "301"), testIds.getValue());
        assertEquals(batched, new HashSet<>(excluded.getValue()));
        assertEquals(Integer.valueOf(25), cap.getValue());
        assertEquals(1, pending.size());
        assertEquals("13", pending.get(0).getAnalysisId());
    }

    @Test
    public void getBatches_asksOnlyForTheCallersOwnUnarchivedBatches() {
        BatchWorkplan mine = new BatchWorkplan();
        mine.setId(5L);
        mine.setName("Mine");
        mine.setStatus(BatchWorkplanStatus.DRAFT);
        mine.setCreatedByUserId(42);
        when(batchWorkplanDAO.getForUserInStatuses(any(), anyList())).thenReturn(Collections.singletonList(mine));

        List<BatchWorkplanResponse> batches = service.getBatches("42");

        ArgumentCaptor<Integer> owner = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<List<BatchWorkplanStatus>> statuses = ArgumentCaptor.forClass(List.class);
        verify(batchWorkplanDAO).getForUserInStatuses(owner.capture(), statuses.capture());

        assertEquals(Integer.valueOf(42), owner.getValue());
        // Archived batches are kept for audit but must not reach the working view.
        assertEquals(
                Arrays.asList(BatchWorkplanStatus.DRAFT, BatchWorkplanStatus.ACTIVE, BatchWorkplanStatus.COMPLETED),
                statuses.getValue());
        assertEquals(1, batches.size());
        assertEquals(Long.valueOf(5L), batches.get(0).getId());
    }

    @Test
    public void createBatch_rejectsAnalysisOutsideTheUsersLabUnits() {
        BatchWorkplanRequest request = new BatchWorkplanRequest();
        request.setAnalysisIds(Arrays.asList("11", "12"));

        Analysis mine = analysis("11", "A-001");
        Analysis theirs = analysis("12", "A-002");
        when(batchWorkplanItemDAO.getExistingAnalysisIds(anyList(), anyList())).thenReturn(Collections.emptySet());
        when(analysisService.getAnalysesByIdsWithDetails(Arrays.asList("11", "12")))
                .thenReturn(Arrays.asList(mine, theirs));
        // The user holds only the unit that covers analysis 11.
        when(userService.filterAnalysesByLabUnitRoles(eq("42"), anyList(), eq(Constants.ROLE_RESULTS)))
                .thenReturn(Collections.singletonList(mine));

        try {
            service.createBatch(request, "42");
            fail("Expected a batch spanning another lab unit to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("One or more analyses are outside your lab units", expected.getMessage());
        }
        verify(batchWorkplanDAO, never()).insert(any(BatchWorkplan.class));
    }

    @Test
    public void createBatch_rejectsANameOverOneHundredCharacters() {
        BatchWorkplanRequest request = new BatchWorkplanRequest();
        request.setName("x".repeat(101));
        request.setAnalysisIds(Arrays.asList("11"));
        try {
            service.createBatch(request, "42");
            fail("Expected an over-long name to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("Batch name cannot exceed 100 characters", expected.getMessage());
        }
        verify(batchWorkplanDAO, never()).insert(any(BatchWorkplan.class));
    }

    @Test
    public void anotherUsersBatchIsNotFound() {
        BatchWorkplan theirs = batch(30L, BatchWorkplanStatus.DRAFT, 99, "11");
        when(batchWorkplanDAO.getWithItems(30L)).thenReturn(Optional.of(theirs));

        assertNotFound(() -> service.transitionBatch(30L, BatchWorkplanStatus.ARCHIVED, "42"));
        assertNotFound(() -> service.getQcStatus(30L, "42"));
        assertNotFound(() -> service.generate(30L, true, true, "42"));
        assertEquals(BatchWorkplanStatus.DRAFT, theirs.getStatus());
        verify(batchWorkplanDAO, never()).update(any(BatchWorkplan.class));
    }

    @Test
    public void transitionBatch_refusesActivationOutsideTheRelease() {
        try {
            service.transitionBatch(31L, BatchWorkplanStatus.ACTIVE, "42");
            fail("Expected a plain status change to ACTIVE to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("A batch is activated by generating its workplan", expected.getMessage());
        }
        verify(batchWorkplanDAO, never()).update(any(BatchWorkplan.class));
    }

    @Test
    public void transitionBatch_editReturnsAnActiveBatchToDraftAndClearsTheReleaseStamp() {
        BatchWorkplan batch = batch(32L, BatchWorkplanStatus.ACTIVE, 42, "11");
        batch.setGeneratedAt(new Timestamp(System.currentTimeMillis()));
        batch.setGeneratedByUserId(42);
        when(batchWorkplanDAO.getWithItems(32L)).thenReturn(Optional.of(batch));
        when(batchWorkplanDAO.update(batch)).thenReturn(batch);

        service.transitionBatch(32L, BatchWorkplanStatus.DRAFT, "42");

        assertEquals(BatchWorkplanStatus.DRAFT, batch.getStatus());
        assertNull(batch.getGeneratedAt());
        assertNull(batch.getGeneratedByUserId());
    }

    @Test
    public void generate_qcPassReleasesTheBatchAndRecordsEachControlLotWithoutAnNce() {
        BatchWorkplan batch = setUpBatch(40L, BatchQcStatus.PASS);

        service.generate(40L, false, false, "42");

        assertEquals(BatchWorkplanStatus.ACTIVE, batch.getStatus());
        assertNotNull(batch.getGeneratedAt());
        assertEquals(Integer.valueOf(42), batch.getGeneratedByUserId());
        ArgumentCaptor<BatchWorkplanQcRelease> release = ArgumentCaptor.forClass(BatchWorkplanQcRelease.class);
        verify(releaseDAO).insert(release.capture());
        assertEquals("lot-1", release.getValue().getControlLotId());
        assertEquals(BatchQcStatus.PASS, release.getValue().getQcStatus());
        assertFalse(release.getValue().isOverridden());
        assertNull(release.getValue().getNceId());
        verify(ncEventService, never()).createAutoGenerated(any(), any(), any());
    }

    @Test
    public void generate_qcNotPassWithoutAnOverrideIsRefusedAndNothingChanges() {
        BatchWorkplan batch = setUpBatch(41L, BatchQcStatus.OVERDUE);

        BatchReleaseRefusedException refused = assertRefused(() -> service.generate(41L, false, true, "42"));

        assertEquals(BatchReleaseRefusedException.Reason.OVERRIDE_REQUIRED, refused.getReason());
        assertEquals(BatchQcStatus.OVERDUE, refused.getQc().status());
        assertEquals(BatchWorkplanStatus.DRAFT, batch.getStatus());
        verify(releaseDAO, never()).insert(any());
        verify(ncEventService, never()).createAutoGenerated(any(), any(), any());
    }

    @Test
    public void generate_anOverrideByAUserWithoutThePermissionIsRefused() {
        BatchWorkplan batch = setUpBatch(42L, BatchQcStatus.FAILED);

        BatchReleaseRefusedException refused = assertRefused(() -> service.generate(42L, true, false, "42"));

        assertEquals(BatchReleaseRefusedException.Reason.OVERRIDE_NOT_PERMITTED, refused.getReason());
        assertEquals(BatchWorkplanStatus.DRAFT, batch.getStatus());
        verify(ncEventService, never()).createAutoGenerated(any(), any(), any());
    }

    @Test
    public void generate_anExpiredControlLotNeverReleasesEvenWithAnOverride() {
        BatchWorkplan batch = setUpBatch(43L, BatchQcStatus.LOT_EXPIRED);

        BatchReleaseRefusedException refused = assertRefused(() -> service.generate(43L, true, true, "42"));

        assertEquals(BatchReleaseRefusedException.Reason.BLOCKED, refused.getReason());
        assertEquals(BatchWorkplanStatus.DRAFT, batch.getStatus());
        verify(releaseDAO, never()).insert(any());
    }

    @Test
    public void generate_anOverrideRaisesAnNceAndLinksItToTheRelease() {
        BatchWorkplan batch = setUpBatch(44L, BatchQcStatus.VIOLATION);
        NceCategory analysis = new NceCategory();
        analysis.setId(4);
        analysis.setName("Analysis");
        NceType deviation = new NceType();
        deviation.setId(50);
        deviation.setName("Procedure deviation");
        NceType qcFailure = new NceType();
        qcFailure.setId(49);
        qcFailure.setName("QC failure");
        when(nceCategoryService.getAllNceCategories()).thenReturn(List.of(analysis));
        when(nceTypeService.getNceTypesByCategoryId(4)).thenReturn(List.of(qcFailure, deviation));
        when(ncEventService.createAutoGenerated(any(NcEvent.class), any(), any())).thenAnswer(invocation -> {
            NcEvent nce = invocation.getArgument(0);
            nce.setId(501);
            nce.setNceNumber("NCE-0501");
            return nce;
        });

        service.generate(44L, true, true, "42");

        ArgumentCaptor<NcEvent> nce = ArgumentCaptor.forClass(NcEvent.class);
        ArgumentCaptor<Consumer<NcEvent>> afterCreate = ArgumentCaptor.forClass(Consumer.class);
        verify(ncEventService).createAutoGenerated(nce.capture(), any(), afterCreate.capture());
        afterCreate.getValue().accept(nce.getValue());
        ArgumentCaptor<NceSpecimen> specimen = ArgumentCaptor.forClass(NceSpecimen.class);
        verify(nceSpecimenService).save(specimen.capture());
        assertEquals(Integer.valueOf(501), specimen.getValue().getNceId());
        assertEquals(Integer.valueOf(11), specimen.getValue().getAnalysisId());
        assertEquals(Integer.valueOf(20011), specimen.getValue().getSampleItemId());
        assertEquals(BatchWorkplanServiceImpl.TRIGGER_SOURCE_WORKPLAN_BATCH, nce.getValue().getTriggerSourceType());
        assertTrue(nce.getValue().getTriggerSourceId().startsWith("44:"));
        assertEquals("MAJOR", nce.getValue().getSeverity());
        assertTrue(nce.getValue().getDescription().contains("QC status VIOLATION"));
        assertTrue(nce.getValue().getDescription().contains("rules 1-3s"));
        assertTrue(nce.getValue().getDescription().contains("violation viol-1"));
        assertTrue(nce.getValue().getDescription().contains("A-011 Hemoglobin"));
        assertEquals(Integer.valueOf(4), nce.getValue().getNceCategoryId());
        assertEquals(Integer.valueOf(50), nce.getValue().getNceTypeId());

        ArgumentCaptor<BatchWorkplanQcRelease> release = ArgumentCaptor.forClass(BatchWorkplanQcRelease.class);
        verify(releaseDAO).insert(release.capture());
        assertTrue(release.getValue().isOverridden());
        assertEquals(Integer.valueOf(501), release.getValue().getNceId());
        assertEquals("viol-1", release.getValue().getQcRuleViolationId());
        assertEquals("run-1", release.getValue().getQcResultId());
        assertEquals(BatchWorkplanStatus.ACTIVE, batch.getStatus());
    }

    @Test
    public void generate_requiresAReagentAndLot() {
        BatchWorkplan batch = batch(45L, BatchWorkplanStatus.DRAFT, 42, "11");
        when(batchWorkplanDAO.getWithItems(45L)).thenReturn(Optional.of(batch));
        try {
            service.generate(45L, true, true, "42");
            fail("Expected a batch without a reagent lot to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("Assign a reagent and lot before generating a workplan", expected.getMessage());
        }
        verify(qcService, never()).evaluate(any(), any());
    }

    @Test
    public void generate_refusesALotThatIsNoLongerUsable() {
        BatchWorkplan batch = batch(46L, BatchWorkplanStatus.DRAFT, 42, "11");
        batch.setReagentId(70L);
        batch.setReagentLotId(80L);
        when(batchWorkplanDAO.getWithItems(46L)).thenReturn(Optional.of(batch));
        when(inventoryLotService.getAvailableLotsByItemFEFO(70L)).thenReturn(List.of(inventoryLot(81L)));
        try {
            service.generate(46L, false, false, "42");
            fail("Expected an unusable reagent lot to be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().startsWith("The batch's reagent lot is no longer usable"));
        }
        assertEquals(BatchWorkplanStatus.DRAFT, batch.getStatus());
    }

    @Test
    public void assignReagent_refusesAReagentNoTestInTheBatchIsLinkedTo() {
        BatchWorkplan batch = batch(50L, BatchWorkplanStatus.DRAFT, 42, "11");
        when(batchWorkplanDAO.getWithItems(50L)).thenReturn(Optional.of(batch));
        when(analysisService.getAnalysesByIdsWithDetails(List.of("11"))).thenReturn(List.of(analysis("11", "A-011")));
        try {
            service.assignReagent(50L, new BatchReagentRequest(70L, 80L, null), "42");
            fail("Expected an unlinked reagent to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("The reagent is not linked to any test in this batch", expected.getMessage());
        }
        assertNull(batch.getReagentId());
    }

    @Test
    public void assignReagent_refusesALotTheResultsPageWouldNotOffer() {
        BatchWorkplan batch = linkedBatch(51L);
        when(inventoryLotService.getAvailableLotsByItemFEFO(70L)).thenReturn(List.of(inventoryLot(81L)));
        try {
            service.assignReagent(51L, new BatchReagentRequest(70L, 80L, null), "42");
            fail("Expected an unavailable lot to be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().startsWith("This lot is expired"));
        }
        assertNull(batch.getReagentLotId());
    }

    @Test
    public void assignReagent_refusesAnAnalyzerNotBoundToTheBatchsTests() {
        BatchWorkplan batch = linkedBatch(52L);
        when(inventoryLotService.getAvailableLotsByItemFEFO(70L)).thenReturn(List.of(inventoryLot(80L)));
        when(analyzerService.getCapabilitiesForTest("30011"))
                .thenReturn(List.of(new AnalyzerTestCapability("9", "Sysmex", "HGB")));
        try {
            service.assignReagent(52L, new BatchReagentRequest(70L, 80L, "10"), "42");
            fail("Expected an analyzer that does not run the test to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("The analyzer is not set up to run every test in this batch", expected.getMessage());
        }
        assertNull(batch.getReagentLotId());
    }

    @Test
    public void assignReagent_storesTheReagentLotAndAnalyzer() {
        BatchWorkplan batch = linkedBatch(53L);
        when(inventoryLotService.getAvailableLotsByItemFEFO(70L)).thenReturn(List.of(inventoryLot(80L)));
        when(analyzerService.getCapabilitiesForTest("30011"))
                .thenReturn(List.of(new AnalyzerTestCapability("9", "Sysmex", "HGB")));
        when(batchWorkplanDAO.update(batch)).thenReturn(batch);
        when(qcService.evaluate(any(), eq("9"))).thenReturn(qc(BatchQcStatus.NOT_RUN));

        service.assignReagent(53L, new BatchReagentRequest(70L, 80L, "9"), "42");

        assertEquals(Long.valueOf(70L), batch.getReagentId());
        assertEquals(Long.valueOf(80L), batch.getReagentLotId());
        assertEquals("9", batch.getInstrumentId());
    }

    @Test
    public void assignReagent_onlyChangesADraft() {
        BatchWorkplan batch = batch(54L, BatchWorkplanStatus.ACTIVE, 42, "11");
        when(batchWorkplanDAO.getWithItems(54L)).thenReturn(Optional.of(batch));
        try {
            service.assignReagent(54L, new BatchReagentRequest(70L, 80L, null), "42");
            fail("Expected an active batch to be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().startsWith("Only a draft batch can be changed"));
        }
    }

    @Test
    public void updateBatch_keepsExistingTestsAndChecksOnlyTheAddedOnes() {
        BatchWorkplan batch = batch(60L, BatchWorkplanStatus.DRAFT, 42, "11", "12");
        when(batchWorkplanDAO.getWithItems(60L)).thenReturn(Optional.of(batch));
        when(batchWorkplanItemDAO.getExistingAnalysisIdsOutsideBatch(eq(List.of("13")), anyList(), eq(60L)))
                .thenReturn(Collections.emptySet());
        List<Analysis> added = List.of(analysis("13", "A-013"));
        when(analysisService.getAnalysesByIdsWithDetails(List.of("13"))).thenReturn(added);
        when(userService.filterAnalysesByLabUnitRoles("42", added, Constants.ROLE_RESULTS)).thenReturn(added);
        when(batchWorkplanDAO.update(batch)).thenReturn(batch);

        BatchWorkplanRequest request = new BatchWorkplanRequest();
        request.setName("Renamed");
        request.setAnalysisIds(List.of("12", "13"));
        service.updateBatch(60L, request, "42");

        assertEquals("Renamed", batch.getName());
        assertEquals(List.of("12", "13"),
                batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toList()));
        assertEquals(Integer.valueOf(1), batch.getItems().get(0).getSortOrder());
        assertEquals(Integer.valueOf(2), batch.getItems().get(1).getSortOrder());
    }

    @Test
    public void updateBatch_refusesATestHeldByAnotherOpenBatch() {
        BatchWorkplan batch = batch(61L, BatchWorkplanStatus.DRAFT, 42, "11");
        when(batchWorkplanDAO.getWithItems(61L)).thenReturn(Optional.of(batch));
        when(batchWorkplanItemDAO.getExistingAnalysisIdsOutsideBatch(eq(List.of("14")), anyList(), eq(61L)))
                .thenReturn(Set.of("14"));
        BatchWorkplanRequest request = new BatchWorkplanRequest();
        request.setAnalysisIds(List.of("11", "14"));
        try {
            service.updateBatch(61L, request, "42");
            fail("Expected a test held by another batch to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("Analyses already assigned to an open batch: [14]", expected.getMessage());
        }
        assertEquals(1, batch.getItems().size());
    }

    @Test
    public void updateBatch_refusesAnEmptyTestList() {
        BatchWorkplan batch = batch(62L, BatchWorkplanStatus.DRAFT, 42, "11");
        when(batchWorkplanDAO.getWithItems(62L)).thenReturn(Optional.of(batch));
        BatchWorkplanRequest request = new BatchWorkplanRequest();
        request.setAnalysisIds(List.of(" "));
        try {
            service.updateBatch(62L, request, "42");
            fail("Expected an empty batch to be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("A batch must contain at least one test", expected.getMessage());
        }
    }

    @Test
    public void archiveAll_archivesEveryBatchStillInHand() {
        BatchWorkplan draft = batch(70L, BatchWorkplanStatus.DRAFT, 42, "11");
        BatchWorkplan active = batch(71L, BatchWorkplanStatus.ACTIVE, 42, "12");
        when(batchWorkplanDAO.getForUserInStatuses(eq(42), anyList())).thenReturn(List.of(draft, active));

        assertEquals(2, service.archiveAll("42"));

        assertEquals(BatchWorkplanStatus.ARCHIVED, draft.getStatus());
        assertEquals(BatchWorkplanStatus.ARCHIVED, active.getStatus());
        assertNotNull(draft.getArchivedAt());
    }

    @Test
    public void refreshLifecycle_completesAnActiveBatchOnlyWhenEveryTestIsFinalized() {
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("6");
        when(statusService.getStatusID(AnalysisStatus.Canceled)).thenReturn("7");
        BatchWorkplan done = batch(80L, BatchWorkplanStatus.ACTIVE, 42, "11", "12");
        BatchWorkplan open = batch(81L, BatchWorkplanStatus.ACTIVE, 42, "13");
        when(batchWorkplanDAO.getForUserInStatuses(eq(42), anyList())).thenReturn(List.of(done, open));
        Analysis finalized = analysis("11", "A-011");
        finalized.setStatusId("6");
        Analysis canceled = analysis("12", "A-012");
        canceled.setStatusId("7");
        when(analysisService.getAnalysesByIdsWithDetails(List.of("11", "12"))).thenReturn(List.of(finalized, canceled));
        when(analysisService.getAnalysesByIdsWithDetails(List.of("13"))).thenReturn(List.of(analysis("13", "A-013")));

        assertEquals(1, service.refreshLifecycle("42"));

        assertEquals(BatchWorkplanStatus.COMPLETED, done.getStatus());
        assertNotNull(done.getCompletedAt());
        assertEquals(BatchWorkplanStatus.ACTIVE, open.getStatus());
    }

    @Test
    public void refreshLifecycle_archivesACompletedBatchAfterThirtyDays() {
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("6");
        when(statusService.getStatusID(AnalysisStatus.Canceled)).thenReturn("7");
        BatchWorkplan old = batch(82L, BatchWorkplanStatus.COMPLETED, 42, "11");
        old.setCompletedAt(Timestamp.from(Instant.now().minus(31, ChronoUnit.DAYS)));
        BatchWorkplan recent = batch(83L, BatchWorkplanStatus.COMPLETED, 42, "12");
        recent.setCompletedAt(Timestamp.from(Instant.now().minus(29, ChronoUnit.DAYS)));
        when(batchWorkplanDAO.getForUserInStatuses(eq(42), anyList())).thenReturn(List.of(old, recent));

        assertEquals(1, service.refreshLifecycle("42"));

        assertEquals(BatchWorkplanStatus.ARCHIVED, old.getStatus());
        assertEquals(BatchWorkplanStatus.COMPLETED, recent.getStatus());
    }

    private BatchWorkplan setUpBatch(Long id, BatchQcStatus status) {
        BatchWorkplan batch = batch(id, BatchWorkplanStatus.DRAFT, 42, "11");
        batch.setReagentId(70L);
        batch.setReagentLotId(80L);
        when(batchWorkplanDAO.getWithItems(id)).thenReturn(Optional.of(batch));
        when(batchWorkplanDAO.update(batch)).thenReturn(batch);
        when(inventoryLotService.getAvailableLotsByItemFEFO(70L)).thenReturn(List.of(inventoryLot(80L)));
        when(analysisService.getAnalysesByIdsWithDetails(List.of("11"))).thenReturn(List.of(analysis("11", "A-011")));
        when(qcService.evaluate(any(), any())).thenReturn(qc(status));
        return batch;
    }

    private BatchWorkplan linkedBatch(Long id) {
        BatchWorkplan batch = batch(id, BatchWorkplanStatus.DRAFT, 42, "11");
        when(batchWorkplanDAO.getWithItems(id)).thenReturn(Optional.of(batch));
        when(analysisService.getAnalysesByIdsWithDetails(List.of("11"))).thenReturn(List.of(analysis("11", "A-011")));
        when(testReagentLinkService.getByTestIdAndReagentId("30011", 70L)).thenReturn(new TestReagentLink());
        return batch;
    }

    private static BatchQcStatusResponse qc(BatchQcStatus status) {
        QcRunResponse run = new QcRunResponse("run-1", new Timestamp(System.currentTimeMillis()), "Tech", null, null,
                null, status == BatchQcStatus.FAILED ? "REJECTED" : "ACCEPTED", null, List.of());
        ControlLotQcResponse lot = new ControlLotQcResponse("lot-1", "L1-2026", "Lyphochek", "Bio-Rad", "LOW", "30011",
                "Hemoglobin", "40011", null, true, "ACTIVE", null, QCFrequencyType.DAILY, null, 8, true, status, null,
                null, false, false, status == BatchQcStatus.VIOLATION ? List.of("1-3s") : List.of(),
                status == BatchQcStatus.VIOLATION ? List.of("viol-1") : List.of(), run, List.of(run));
        return new BatchQcStatusResponse(status, status.isBlocking(), !status.isPass() && !status.isBlocking(), false,
                null, null, List.of(lot), List.of());
    }

    private static InventoryLot inventoryLot(Long id) {
        InventoryLot lot = new InventoryLot();
        lot.setId(id);
        lot.setLotNumber("R-" + id);
        return lot;
    }

    private BatchWorkplan batch(Long id, BatchWorkplanStatus status, Integer owner, String... analysisIds) {
        BatchWorkplan batch = new BatchWorkplan();
        batch.setId(id);
        batch.setName("Batch " + id);
        batch.setStatus(status);
        batch.setCreatedByUserId(owner);
        batch.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        int order = 1;
        for (String analysisId : analysisIds) {
            BatchWorkplanItem item = new BatchWorkplanItem();
            item.setAnalysisId(analysisId);
            item.setSortOrder(order++);
            batch.addItem(item);
        }
        return batch;
    }

    private static void assertNotFound(Runnable call) {
        try {
            call.run();
            fail("Expected another user's batch to be answered as not found");
        } catch (BatchWorkplanNotFoundException expected) {
            // the batch exists, but not for this caller
        }
    }

    private static BatchReleaseRefusedException assertRefused(Runnable call) {
        try {
            call.run();
        } catch (BatchReleaseRefusedException refused) {
            return refused;
        }
        fail("Expected the release to be refused");
        return null;
    }

    private Analysis analysis(String id, String accessionNumber) {
        Sample sample = new Sample();
        sample.setId("100" + id);
        sample.setAccessionNumber(accessionNumber);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("200" + id);
        sampleItem.setSample(sample);

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("300" + id);
        test.setDescription("Hemoglobin");

        TestSection section = new TestSection();
        section.setId("400" + id);
        section.setTestSectionName("Hematology");

        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setStatusId("1");
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);
        analysis.setTestSection(section);
        return analysis;
    }
}
