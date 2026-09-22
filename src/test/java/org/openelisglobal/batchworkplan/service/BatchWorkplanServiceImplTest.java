package org.openelisglobal.batchworkplan.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanItemDAO;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.valueholder.TestSection;

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
