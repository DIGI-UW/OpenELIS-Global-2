package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SampleStorageServiceImplTest {

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private SampleItemDAO sampleItemDAO;

    @InjectMocks
    private SampleStorageServiceImpl sampleStorageService;

    @Test
    public void testGetSampleAssignments_WithPageable_ReturnsCorrectPageSize() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 25);
        List<SampleStorageAssignment> assignments = createTestAssignments(25);
        Page<SampleStorageAssignment> page = new PageImpl<>(assignments, pageable, 100);
        when(sampleStorageAssignmentDAO.findAll(pageable)).thenReturn(page);

        // Act
        Page<SampleStorageAssignment> result = sampleStorageService.getSampleAssignments(pageable);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Page size should be 25", 25, result.getContent().size());
        assertEquals("Total elements should be 100", 100, result.getTotalElements());
    }

    @Test
    public void testGetSampleAssignments_WithPageable_ReturnsTotalElements() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 25);
        List<SampleStorageAssignment> assignments = createTestAssignments(25);
        Page<SampleStorageAssignment> page = new PageImpl<>(assignments, pageable, 100);
        when(sampleStorageAssignmentDAO.findAll(pageable)).thenReturn(page);

        // Act
        Page<SampleStorageAssignment> result = sampleStorageService.getSampleAssignments(pageable);

        // Assert
        assertEquals("Total elements should be 100", 100, result.getTotalElements());
        assertEquals("Total pages should be 4", 4, result.getTotalPages());
    }

    @Test
    public void testGetSampleAssignments_FirstPage_ReturnsFirstNItems() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 25);
        List<SampleStorageAssignment> assignments = createTestAssignments(25);
        Page<SampleStorageAssignment> page = new PageImpl<>(assignments, pageable, 100);
        when(sampleStorageAssignmentDAO.findAll(pageable)).thenReturn(page);

        // Act
        Page<SampleStorageAssignment> result = sampleStorageService.getSampleAssignments(pageable);

        // Assert
        assertEquals("Current page should be 0", 0, result.getNumber());
        assertEquals("Should be first page", true, result.isFirst());
        assertEquals("Should not be last page", false, result.isLast());
    }

    @Test
    public void testGetSampleAssignments_LastPage_ReturnsRemainingItems() {
        // Arrange
        Pageable pageable = PageRequest.of(3, 25); // Page 4 (last page)
        List<SampleStorageAssignment> assignments = createTestAssignments(25); // Last page has 25 items
        Page<SampleStorageAssignment> page = new PageImpl<>(assignments, pageable, 100);
        when(sampleStorageAssignmentDAO.findAll(pageable)).thenReturn(page);

        // Act
        Page<SampleStorageAssignment> result = sampleStorageService.getSampleAssignments(pageable);

        // Assert
        assertEquals("Should be last page", true, result.isLast());
        assertEquals("Current page should be 3", 3, result.getNumber());
    }

    @Test
    public void testGetSampleAssignments_InvalidPageNumber_HandlesGracefully() {
        // Arrange - Page beyond available pages
        Pageable pageable = PageRequest.of(100, 25); // Page 100, but only 4 pages exist
        List<SampleStorageAssignment> assignments = new ArrayList<>(); // Empty page
        Page<SampleStorageAssignment> page = new PageImpl<>(assignments, pageable, 100);
        when(sampleStorageAssignmentDAO.findAll(pageable)).thenReturn(page);

        // Act
        Page<SampleStorageAssignment> result = sampleStorageService.getSampleAssignments(pageable);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return empty list for invalid page", 0, result.getContent().size());
        assertEquals("Total elements should still be 100", 100, result.getTotalElements());
    }

    // ── storageSkipped propagation ─────────────────────────────────────────────

    @Test
    public void testGetAllSamplesWithAssignments_WhenStorageSkippedTrue_AllFieldsCorrect() {
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-001");
        sample.setStorageSkipped(true);

        SampleItem item = buildSampleItem("1", sample);

        when(sampleItemDAO.getAllSampleItems()).thenReturn(List.of(item));
        when(sampleStorageAssignmentDAO.getAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = sampleStorageService.getAllSamplesWithAssignments();

        assertEquals(1, result.size());
        Map<String, Object> row = result.get(0);

        assertEquals("1", row.get("id"));
        assertEquals("1", row.get("sampleItemId"));
        assertEquals("", row.get("sampleItemExternalId"));
        assertEquals("ACC-001", row.get("sampleAccessionNumber"));
        assertTrue("storageSkipped must be true", (Boolean) row.get("storageSkipped"));
        assertEquals("", row.get("type"));
        assertEquals("active", row.get("status"));
        assertEquals("", row.get("location"));
        assertNull("unassigned item must have null assignedBy", row.get("assignedBy"));
        assertEquals("", row.get("date"));
        assertEquals("", row.get("positionCoordinate"));
        assertEquals("", row.get("notes"));
    }

    @Test
    public void testGetAllSamplesWithAssignments_WhenStorageSkippedFalse_AllFieldsCorrect() {
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-002");
        sample.setStorageSkipped(false);

        SampleItem item = buildSampleItem("2", sample);

        when(sampleItemDAO.getAllSampleItems()).thenReturn(List.of(item));
        when(sampleStorageAssignmentDAO.getAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = sampleStorageService.getAllSamplesWithAssignments();

        assertEquals(1, result.size());
        Map<String, Object> row = result.get(0);

        assertEquals("2", row.get("id"));
        assertEquals("2", row.get("sampleItemId"));
        assertEquals("ACC-002", row.get("sampleAccessionNumber"));
        assertFalse("storageSkipped must be false", (Boolean) row.get("storageSkipped"));
        assertEquals("", row.get("location"));
        assertNull(row.get("assignedBy"));
        assertEquals("", row.get("positionCoordinate"));
        assertEquals("", row.get("notes"));
    }

    @Test
    public void testGetAllSamplesWithAssignments_WhenStorageSkippedNull_TreatedAsFalse() {
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-003");
        sample.setStorageSkipped(null);

        SampleItem item = buildSampleItem("3", sample);

        when(sampleItemDAO.getAllSampleItems()).thenReturn(List.of(item));
        when(sampleStorageAssignmentDAO.getAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = sampleStorageService.getAllSamplesWithAssignments();

        assertEquals(1, result.size());
        Map<String, Object> row = result.get(0);

        assertEquals("ACC-003", row.get("sampleAccessionNumber"));
        assertFalse("null storageSkipped must be coerced to false", (Boolean) row.get("storageSkipped"));
        assertEquals("", row.get("location"));
        assertEquals("active", row.get("status"));
    }

    @Test
    public void testGetAllSamplesWithAssignments_WhenSampleIsNull_DefaultsApplied() {
        SampleItem item = buildSampleItem("4", null);

        when(sampleItemDAO.getAllSampleItems()).thenReturn(List.of(item));
        when(sampleStorageAssignmentDAO.getAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = sampleStorageService.getAllSamplesWithAssignments();

        assertEquals(1, result.size());
        Map<String, Object> row = result.get(0);

        assertEquals("4", row.get("id"));
        assertEquals("", row.get("sampleAccessionNumber"));
        assertFalse("null sample must yield storageSkipped=false", (Boolean) row.get("storageSkipped"));
        assertEquals("", row.get("location"));
        assertNull(row.get("assignedBy"));
    }

    @Test
    public void testGetAllSamplesWithAssignments_MixedSkipped_EachRowIndependent() {
        Sample skipped = new Sample();
        skipped.setAccessionNumber("ACC-SKIP");
        skipped.setStorageSkipped(true);

        Sample notSkipped = new Sample();
        notSkipped.setAccessionNumber("ACC-NOSKIP");
        notSkipped.setStorageSkipped(false);

        // IDs "1" < "2" so skipped item sorts first (unassigned sort is by id)
        SampleItem itemA = buildSampleItem("1", skipped);
        SampleItem itemB = buildSampleItem("2", notSkipped);

        when(sampleItemDAO.getAllSampleItems()).thenReturn(List.of(itemA, itemB));
        when(sampleStorageAssignmentDAO.getAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = sampleStorageService.getAllSamplesWithAssignments();

        assertEquals(2, result.size());

        Map<String, Object> first = result.get(0);
        assertEquals("1", first.get("id"));
        assertEquals("ACC-SKIP", first.get("sampleAccessionNumber"));
        assertTrue("item 1 must be storage-skipped", (Boolean) first.get("storageSkipped"));

        Map<String, Object> second = result.get(1);
        assertEquals("2", second.get("id"));
        assertEquals("ACC-NOSKIP", second.get("sampleAccessionNumber"));
        assertFalse("item 2 must not be storage-skipped", (Boolean) second.get("storageSkipped"));
    }

    @Test
    public void testGetAllSamplesWithAssignments_NullAndNullIdItemsFiltered() {
        SampleItem nullItem = null;
        SampleItem noIdItem = new SampleItem(); // id is null

        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-REAL");
        SampleItem realItem = buildSampleItem("5", sample);

        when(sampleItemDAO.getAllSampleItems()).thenReturn(List.of(realItem));
        when(sampleStorageAssignmentDAO.getAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = sampleStorageService.getAllSamplesWithAssignments();

        // Only the item with a real id survives
        assertEquals(1, result.size());
        assertEquals("5", result.get(0).get("id"));
        assertEquals("ACC-REAL", result.get(0).get("sampleAccessionNumber"));
    }

    // Helper method to create test assignments
    private List<SampleStorageAssignment> createTestAssignments(int count) {
        List<SampleStorageAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SampleStorageAssignment assignment = new SampleStorageAssignment();
            assignment.setId(i + 1); // ID is Integer type
            assignments.add(assignment);
        }
        return assignments;
    }

    private SampleItem buildSampleItem(String id, Sample sample) {
        SampleItem item = new SampleItem();
        item.setId(id);
        item.setSample(sample);
        return item;
    }
}
