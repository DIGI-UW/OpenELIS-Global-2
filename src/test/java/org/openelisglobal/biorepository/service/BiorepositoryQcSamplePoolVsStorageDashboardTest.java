package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * QC pool active-in-scope count should align with Storage Management active filter for the same department.
 */
@RunWith(MockitoJUnitRunner.class)
public class BiorepositoryQcSamplePoolVsStorageDashboardTest {

    private static final int NOTEBOOK_ID = 7;
    private static final int DEPT_ID = 55;

    @InjectMocks
    private BiorepositoryQcSamplePoolServiceImpl poolService;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private BiorepositoryQCInspectionService qcInspectionService;

    @Mock
    private StorageLocationService storageLocationService;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private IStatusService statusService;

    @Before
    public void setUp() {
        when(storageLocationService.getAllDevices()).thenReturn(List.of());
        when(storageLocationService.getAllShelves()).thenReturn(List.of());
        when(storageLocationService.getAllRacks()).thenReturn(List.of());
        when(storageLocationService.getAllBoxes()).thenReturn(List.of());

        TestSection department = new TestSection();
        department.setId(String.valueOf(DEPT_ID));
        department.setTestSectionName("Biorepository Laboratory");
        when(noteBookService.getNoteBookDepartments(NOTEBOOK_ID)).thenReturn(Set.of(department));

        when(statusService.matches(any(), eq(SampleStatus.Disposed))).thenAnswer(invocation -> {
            String statusId = invocation.getArgument(0);
            return "disposed".equals(statusId);
        });
        when(qcInspectionService.existsByBioSampleId(any())).thenReturn(false);
        when(qcInspectionService.hasInspectionBetween(any(), any(), any())).thenReturn(false);
    }

    @Test
    public void activeInScopeCount_matchesStorageDashboardActiveFilter() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", String.valueOf(i));
            row.put("location", "Biorepository Laboratory > Freezer > Shelf > Rack > Box-" + i);
            row.put("departmentTestSectionId", DEPT_ID);
            row.put("departmentName", "Biorepository Laboratory");
            row.put("deviceName", "Freezer");
            row.put("status", i == 5 ? "disposed" : "active");
            rows.add(row);
        }
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(rows);

        List<Map<String, Object>> storageActive = rows.stream()
                .filter(row -> {
                    String location = String.valueOf(row.get("location"));
                    if (location.isBlank()) {
                        return false;
                    }
                    String status = String.valueOf(row.get("status"));
                    return !statusService.matches(status, SampleStatus.Disposed);
                })
                .filter(row -> DEPT_ID == ((Number) row.get("departmentTestSectionId")).intValue())
                .toList();

        for (int i = 1; i <= 4; i++) {
            SampleItem sampleItem = new SampleItem();
            sampleItem.setId(String.valueOf(i));
            when(sampleStorageService.resolveSampleItemByIdentifier(String.valueOf(i))).thenReturn(sampleItem);
            BioSample bioSample = new BioSample();
            bioSample.setId(600 + i);
            bioSample.setWorkflowStatus(WorkflowStatus.STORED);
            bioSample.setSampleItem(sampleItem);
            when(bioSampleService.getBySampleItemId(i)).thenReturn(bioSample);
        }

        Map<String, Object> overview = poolService.buildStorageOverview(null, null, null, null, true, NOTEBOOK_ID);
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) overview.get("diagnostics");

        assertEquals(storageActive.size(), diagnostics.get("storageManagementActiveInScope"));
        assertEquals(storageActive.size(), diagnostics.get("qcPoolTotal"));
    }
}
