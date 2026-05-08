package org.openelisglobal.storage.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageDashboardService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class SampleStorageRestControllerDepartmentIsolationTest {

    @InjectMocks
    private SampleStorageRestController controller;

    @Mock
    private SampleStorageService sampleStorageService;
    @Mock
    private StorageLocationService storageLocationService;
    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;
    @Mock
    private SampleItemDAO sampleItemDAO;
    @Mock
    private StorageDashboardService storageDashboardService;
    @Mock
    private IStatusService statusService;
    @Mock
    private DepartmentIsolationService departmentIsolationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void assignSampleItemReturnsForbiddenWhenSampleOutOfDepartmentScope() throws Exception {
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleItemId("EXT-1000");
        form.setLocationId("1000");
        form.setLocationType("device");

        when(departmentIsolationService.canAccessSampleItemIdentifier(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        mockMvc.perform(post("/rest/storage/sample-items/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getSampleItemsExcludesRowsInForeignDepartmentLocations() throws Exception {
        Map<String, Object> row = Map.of(
                "sampleItemId", "42",
                "location", "Foreign Room > Foreign Device");
        SampleStorageAssignment assignment = new SampleStorageAssignment();
        assignment.setSampleItemId(42);
        assignment.setLocationType("room");
        assignment.setLocationId(200);
        StorageRoom room = new StorageRoom();
        room.setId(200);

        when(storageDashboardService.filterSamples(null, null)).thenReturn(new ArrayList<>(List.of(row)));
        when(departmentIsolationService.canAccessSampleItemIdentifier(eq("42"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        when(sampleStorageAssignmentDAO.findBySampleItemId("42")).thenReturn(assignment);
        when(storageLocationService.get(eq(200), eq(StorageRoom.class))).thenReturn(room);
        when(departmentIsolationService.canAccessStorageRoom(eq(room), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        mockMvc.perform(get("/rest/storage/sample-items").param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
