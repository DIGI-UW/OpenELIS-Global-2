package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
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
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;

@RunWith(MockitoJUnitRunner.class)
public class BiorepositoryQcSamplePoolServiceTest {

    private static final int NOTEBOOK_ID = 42;
    private static final int DEPT_ID = 100;

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
            return "disposed-status".equals(statusId);
        });
        when(qcInspectionService.existsByBioSampleId(any())).thenReturn(false);
        when(qcInspectionService.hasInspectionBetween(any(), any(), any())).thenReturn(false);
    }

    @Test
    public void buildStorageOverview_excludesDisposedAndUnassigned() {
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(List.of(
                assignmentRow("1", "Room > Freezer > Shelf > Rack > Box", DEPT_ID, "active"),
                assignmentRow("2", "Room > Freezer > Shelf > Rack > Box", DEPT_ID, "disposed-status"),
                assignmentRow("3", "", DEPT_ID, "active")));

        Map<String, Object> overview = poolService.buildStorageOverview(null, null, null, null, true, NOTEBOOK_ID);

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) overview.get("diagnostics");
        assertEquals(1, diagnostics.get("storageManagementActiveInScope"));
        assertEquals(0, diagnostics.get("qcPoolTotal"));
        verify(bioSampleService, never()).ensureBioSampleForStoredSampleItem(any(), any());
    }

    @Test
    public void buildStorageOverview_ensuresBioSampleForEligibleRow() {
        Map<String, Object> row = assignmentRow("10", "Biorepository Laboratory > Freezer-A > Shelf-1 > Rack-1 > Box-1",
                DEPT_ID, "active");
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(List.of(row));

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("10");
        when(sampleStorageService.resolveSampleItemByIdentifier("10")).thenReturn(sampleItem);
        when(bioSampleService.getBySampleItemId(10)).thenReturn(null);

        BioSample created = new BioSample();
        created.setId(501);
        created.setWorkflowStatus(WorkflowStatus.STORED);
        created.setSampleItem(sampleItem);
        when(bioSampleService.ensureBioSampleForStoredSampleItem(sampleItem, null)).thenReturn(created);

        Map<String, Object> overview = poolService.buildStorageOverview(null, null, null, null, true, NOTEBOOK_ID);

        verify(bioSampleService).ensureBioSampleForStoredSampleItem(sampleItem, null);
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) overview.get("diagnostics");
        assertEquals(1, diagnostics.get("qcPoolTotal"));
        assertEquals(1, diagnostics.get("bioSamplesLazyLinked"));
        @SuppressWarnings("unchecked")
        Map<String, Object> scopeStats = (Map<String, Object>) overview.get("scopeStats");
        assertEquals(1, scopeStats.get("totalStored"));
    }

    @Test
    public void listSamplesForQcTable_returnsSampleItemId() {
        Map<String, Object> row = assignmentRow("11",
                "Biorepository Laboratory > Freezer-A > Shelf-1 > Rack-1 > Box-1", DEPT_ID, "active");
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(List.of(row));

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("11");
        when(sampleStorageService.resolveSampleItemByIdentifier("11")).thenReturn(sampleItem);

        BioSample existing = new BioSample();
        existing.setId(502);
        existing.setWorkflowStatus(WorkflowStatus.STORED);
        existing.setSampleItem(sampleItem);
        when(bioSampleService.getBySampleItemId(11)).thenReturn(existing);

        List<Map<String, Object>> samples = poolService.listSamplesForQcTable(NOTEBOOK_ID);
        assertEquals(1, samples.size());
        assertEquals("11", String.valueOf(samples.get(0).get("sampleItemId")));
        assertNotNull(samples.get(0).get("bioSampleId"));
    }

    private static Map<String, Object> assignmentRow(String id, String location, int departmentId, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("location", location);
        row.put("departmentTestSectionId", departmentId);
        row.put("departmentName", "Biorepository Laboratory");
        row.put("deviceName", "Freezer-A");
        row.put("status", status);
        row.put("positionCoordinate", "A1");
        return row;
    }
}
