package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
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
    private SampleItemDAO sampleItemDAO;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private IStatusService statusService;

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

    @Test
    public void testGetAllSamplesWithAssignments_IncludesDerivedIsDisposedFlag() {
        // Arrange
        SampleItem disposedSample = new SampleItem();
        disposedSample.setId("200");
        disposedSample.setStatusId("24");

        SampleItem activeSample = new SampleItem();
        activeSample.setId("201");
        activeSample.setStatusId("1");

        when(sampleItemDAO.getAllSampleItems()).thenReturn(Arrays.asList(disposedSample, activeSample));
        when(sampleStorageAssignmentDAO.getAll()).thenReturn(new ArrayList<>());
        when(statusService.matches("24", SampleStatus.Disposed)).thenReturn(true);
        when(statusService.matches("1", SampleStatus.Disposed)).thenReturn(false);

        // Act
        List<Map<String, Object>> result = sampleStorageService.getAllSamplesWithAssignments();

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return both sample items", 2, result.size());

        boolean foundDisposed = false;
        boolean foundActive = false;
        for (Map<String, Object> row : result) {
            assertTrue("Each row should include derived isDisposed field", row.containsKey("isDisposed"));
            String id = String.valueOf(row.get("id"));
            if ("200".equals(id)) {
                foundDisposed = true;
                assertEquals("Disposed sample should have isDisposed=true", true, row.get("isDisposed"));
            }
            if ("201".equals(id)) {
                foundActive = true;
                assertFalse("Active sample should have isDisposed=false", (Boolean) row.get("isDisposed"));
            }
        }

        assertTrue("Disposed sample row should exist", foundDisposed);
        assertTrue("Active sample row should exist", foundActive);
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
}
