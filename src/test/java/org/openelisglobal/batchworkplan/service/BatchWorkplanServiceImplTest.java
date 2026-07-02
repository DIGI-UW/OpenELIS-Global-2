package org.openelisglobal.batchworkplan.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
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
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
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
        when(analysisService.getAnalysesByIdsWithDetails(Arrays.asList("11", "12")))
                .thenReturn(Arrays.asList(analysis("11", "A-001"), analysis("12", "A-002")));
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
