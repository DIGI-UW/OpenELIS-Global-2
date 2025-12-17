package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.service.ArchivingService.ArchivingProgress;
import org.openelisglobal.notebook.service.ArchivingService.TraceabilityReport;
import org.openelisglobal.notebook.service.ArchivingService.TraceabilityResult;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.openelisglobal.sampleitem.dao.SampleItemAliquotRelationshipDAO;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleitem.valueholder.SampleItemAliquotRelationship;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Unit tests for ArchivingService (US8). Tests biorepository transfer,
 * traceability verification, and notebook finalization.
 *
 * US8 Goal: Once project concludes, transfer remaining Parent and Child Samples
 * to Biorepository Laboratory with permanent storage logged and complete
 * traceability links verified.
 *
 * Independent Test: Transfer 10 samples (both parent and child) to
 * biorepository location, verify traceability checklist items (parent-child
 * links, movement history, storage assignments), confirm permanent storage is
 * logged, mark page complete, notebook status changes to FINALIZED.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ArchivingServiceTest {

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private SampleRoutingService sampleRoutingService;

    @Mock
    private NotebookPageSampleService notebookPageSampleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private SampleItemAliquotRelationshipDAO aliquotRelationshipDAO;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private ArchivingServiceImpl archivingService;

    private NoteBook testNotebook;
    private SystemUser testUser;
    private List<SampleItem> testSamples;

    @Before
    public void setUp() {
        // Set up test notebook
        testNotebook = new NoteBook();
        testNotebook.setId(1);
        testNotebook.setTitle("Test Immunology Workflow");
        testNotebook.setStatus(NoteBookStatus.SUBMITTED);

        // Set up test user
        testUser = mock(SystemUser.class);
        when(testUser.getId()).thenReturn("1");

        // Set up test samples (5 parent + 5 child = 10 total per US8 test)
        testSamples = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            SampleItem sample = mock(SampleItem.class);
            when(sample.getId()).thenReturn(String.valueOf(i));
            testSamples.add(sample);
        }
        testNotebook.setSamples(testSamples);
    }

    // ==================== Transfer to Biorepository Tests ====================

    /**
     * T132: Transfer samples to biorepository. Verify routing records are created
     * with BIOREPOSITORY destination.
     */
    @Test
    public void testTransferToBiorepository_10Samples_AllTransferred() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(systemUserService.get("1")).thenReturn(testUser);
        when(sampleRoutingService.getByNotebookIdAndSampleItemId(anyInt(), anyInt())).thenReturn(null);
        when(sampleRoutingService.insert(any(SampleRouting.class))).thenReturn(1);

        // Mock storage assignment
        when(sampleStorageService.assignSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                anyString())).thenReturn(Map.of("assignmentId", "100", "hierarchicalPath", "Biorepository > Freezer 1"));

        // Act - transfer 10 samples per US8 test
        List<Integer> sampleIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<SampleRouting> routings = archivingService.transferToBiorepository(1, sampleIds, "1", "device",
                "End of project archival", "1");

        // Assert
        assertEquals("All 10 samples should be transferred", 10, routings.size());
        verify(sampleRoutingService, times(10)).insert(any(SampleRouting.class));
    }

    /**
     * T132a: Verify BIOREPOSITORY destination type is used.
     */
    @Test
    public void testTransferToBiorepository_CreatesRoutingWithBiorepositoryDestination() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(systemUserService.get("1")).thenReturn(testUser);
        when(sampleRoutingService.getByNotebookIdAndSampleItemId(1, 1)).thenReturn(null);
        when(sampleRoutingService.insert(any(SampleRouting.class))).thenAnswer(invocation -> {
            SampleRouting routing = invocation.getArgument(0);
            assertEquals("Destination should be BIOREPOSITORY", DestinationType.BIOREPOSITORY,
                    routing.getDestinationType());
            return 1;
        });

        when(sampleStorageService.assignSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                anyString())).thenReturn(Map.of("assignmentId", "100"));

        // Act
        List<SampleRouting> routings = archivingService.transferToBiorepository(1, Arrays.asList(1), "1", "device",
                "Test transfer", "1");

        // Assert
        assertNotNull("Routing list should not be null", routings);
        verify(sampleRoutingService).insert(any(SampleRouting.class));
    }

    /**
     * T132b: Verify both parent and child samples are transferred together.
     */
    @Test
    public void testTransferToBiorepository_ParentAndChildSamples_AllTransferred() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(systemUserService.get("1")).thenReturn(testUser);
        when(sampleRoutingService.getByNotebookIdAndSampleItemId(anyInt(), anyInt())).thenReturn(null);
        when(sampleRoutingService.insert(any(SampleRouting.class))).thenReturn(1);

        when(sampleStorageService.assignSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                anyString())).thenReturn(Map.of("assignmentId", "100"));

        // Parent samples: 1-5, Child samples: 6-10
        List<Integer> parentIds = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> childIds = Arrays.asList(6, 7, 8, 9, 10);
        List<Integer> allIds = new ArrayList<>();
        allIds.addAll(parentIds);
        allIds.addAll(childIds);

        // Act
        List<SampleRouting> routings = archivingService.transferToBiorepository(1, allIds, "1", "device", "Test", "1");

        // Assert
        assertEquals("All 10 samples (5 parent + 5 child) should be transferred", 10, routings.size());
    }

    /**
     * Cannot transfer from finalized notebook.
     */
    @Test(expected = IllegalStateException.class)
    public void testTransferToBiorepository_FinalizedNotebook_ThrowsException() {
        // Arrange
        testNotebook.setStatus(NoteBookStatus.FINALIZED);
        when(noteBookService.get(1)).thenReturn(testNotebook);

        // Act - should throw
        archivingService.transferToBiorepository(1, Arrays.asList(1, 2, 3), "1", "device", "Test", "1");
    }

    /**
     * Skip already archived samples.
     */
    @Test
    public void testTransferToBiorepository_AlreadyArchived_SkipsAndReturnsExisting() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(systemUserService.get("1")).thenReturn(testUser);

        // Sample 1 already archived
        SampleRouting existingRouting = new SampleRouting();
        existingRouting.setId(100);
        existingRouting.setDestinationType(DestinationType.BIOREPOSITORY);
        when(sampleRoutingService.getByNotebookIdAndSampleItemId(1, 1)).thenReturn(existingRouting);

        // Sample 2 not archived
        when(sampleRoutingService.getByNotebookIdAndSampleItemId(1, 2)).thenReturn(null);
        when(sampleRoutingService.insert(any(SampleRouting.class))).thenReturn(2);
        when(sampleStorageService.assignSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                anyString())).thenReturn(Map.of("assignmentId", "101"));

        // Act
        List<SampleRouting> routings = archivingService.transferToBiorepository(1, Arrays.asList(1, 2), "1", "device",
                "Test", "1");

        // Assert - both returned but only sample 2 had insert called
        assertEquals("Should return 2 routings", 2, routings.size());
        verify(sampleRoutingService, times(1)).insert(any(SampleRouting.class));
    }

    // ==================== Traceability Verification Tests ====================

    /**
     * T133: Verify traceability - all checks pass.
     */
    @Test
    public void testVerifyTraceability_AllChecksPassed_ReturnsPassedResult() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);

        // No broken parent-child links
        when(aliquotRelationshipDAO.getByParentSampleItemId(anyString())).thenReturn(Collections.emptyList());

        // All routings have storage assignments
        List<SampleRouting> routings = new ArrayList<>();
        SampleRouting routing = new SampleRouting();
        routing.setDestinationType(DestinationType.BIOREPOSITORY);
        routing.setStorageAssignment(new SampleStorageAssignment());
        routings.add(routing);
        when(sampleRoutingService.getByNotebookId(1)).thenReturn(routings);
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(routings);

        // All page samples completed
        NotebookPageSample completedPageSample = new NotebookPageSample();
        completedPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(List.of(completedPageSample));

        // Act
        TraceabilityResult result = archivingService.verifyTraceability(1);

        // Assert
        assertTrue("All checks should pass", result.passed());
        assertFalse("Should not have critical failures", result.hasCriticalFailures());
        assertNotNull("Should have summary", result.summary());
    }

    /**
     * T133a: Generate traceability report for all sample lineages.
     */
    @Test
    public void testGenerateTraceabilityReport_ReturnsCompleteReport() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(aliquotRelationshipDAO.getByChildSampleItemId(anyString())).thenReturn(Collections.emptyList());
        when(aliquotRelationshipDAO.getByParentSampleItemId(anyString())).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookIdAndSampleItemId(anyInt(), anyInt())).thenReturn(null);
        when(sampleStorageService.getSampleStorageAssignmentsBySampleItem(any())).thenReturn(Collections.emptyList());
        when(sampleStorageService.getSampleStorageMovementsBySampleItem(any())).thenReturn(Collections.emptyList());

        // Act
        TraceabilityReport report = archivingService.generateTraceabilityReport(1);

        // Assert
        assertNotNull("Report should not be null", report);
        assertEquals("Notebook ID should match", Integer.valueOf(1), report.notebookId());
        assertEquals("Title should match", "Test Immunology Workflow", report.notebookTitle());
        assertEquals("Should have 10 lineage records", 10, report.lineages().size());
        assertNotNull("Generated timestamp should be set", report.generatedAt());
    }

    /**
     * T133b: Verify complete storage chain from reception to archive.
     */
    @Test
    public void testVerifyTraceability_IncompleteStorageChain_FailsCheck() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(aliquotRelationshipDAO.getByParentSampleItemId(anyString())).thenReturn(Collections.emptyList());

        // Routing to biorepository but no storage assignment
        List<SampleRouting> routings = new ArrayList<>();
        SampleRouting routing = new SampleRouting();
        routing.setSampleItemId(1);
        routing.setDestinationType(DestinationType.BIOREPOSITORY);
        routing.setStorageAssignment(null); // Missing assignment
        routings.add(routing);

        when(sampleRoutingService.getByNotebookId(1)).thenReturn(routings);
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(routings);
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(Collections.emptyList());

        // Act
        TraceabilityResult result = archivingService.verifyTraceability(1);

        // Assert - should fail storage assignments check (critical)
        assertTrue("Should have critical failures", result.hasCriticalFailures());
        assertTrue("Storage Assignments check should have issues",
                result.checks().stream().filter(c -> c.checkName().equals("Storage Assignments"))
                        .findFirst().map(c -> !c.passed()).orElse(false));
    }

    /**
     * Verify parent-child link check.
     */
    @Test
    public void testVerifyTraceability_BrokenParentChildLink_FailsCheck() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);

        // Simulate broken child link
        SampleItemAliquotRelationship brokenRelationship = mock(SampleItemAliquotRelationship.class);
        when(brokenRelationship.getChildSampleItem()).thenReturn(null); // Broken link

        for (SampleItem sample : testSamples) {
            when(aliquotRelationshipDAO.getByParentSampleItemId(sample.getId()))
                    .thenReturn(Arrays.asList(brokenRelationship));
        }

        when(sampleRoutingService.getByNotebookId(1)).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(Collections.emptyList());
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(Collections.emptyList());

        // Act
        TraceabilityResult result = archivingService.verifyTraceability(1);

        // Assert - should fail parent-child links check (critical)
        assertTrue("Should have critical failures", result.hasCriticalFailures());
    }

    // ==================== Finalization Tests ====================

    /**
     * T135: Finalize notebook after archiving is complete.
     */
    @Test
    public void testFinalizeNotebook_ValidState_StatusChangesToFinalized() {
        // Arrange - notebook can be finalized
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(aliquotRelationshipDAO.getByParentSampleItemId(anyString())).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookId(1)).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(Collections.emptyList());
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(Collections.emptyList());
        when(aliquotRelationshipDAO.getByChildSampleItemId(anyString())).thenReturn(Collections.emptyList());

        // Mock finalized notebook return
        NoteBook finalizedNotebook = new NoteBook();
        finalizedNotebook.setId(1);
        finalizedNotebook.setStatus(NoteBookStatus.FINALIZED);
        when(noteBookService.get(1)).thenReturn(testNotebook).thenReturn(finalizedNotebook);

        // Act
        NoteBook result = archivingService.finalizeNotebook(1, "1");

        // Assert
        assertEquals("Status should be FINALIZED", NoteBookStatus.FINALIZED, result.getStatus());
        verify(noteBookService).updateWithStatus(1, NoteBookStatus.FINALIZED, "1");
    }

    /**
     * T135a: Cannot modify finalized notebooks.
     */
    @Test(expected = IllegalStateException.class)
    public void testFinalizeNotebook_AlreadyFinalized_ThrowsException() {
        // Arrange
        testNotebook.setStatus(NoteBookStatus.FINALIZED);
        when(noteBookService.get(1)).thenReturn(testNotebook);

        // Act - should throw
        archivingService.finalizeNotebook(1, "1");
    }

    /**
     * Cannot finalize with critical traceability failures.
     */
    @Test(expected = IllegalStateException.class)
    public void testFinalizeNotebook_CriticalFailures_ThrowsException() {
        // Arrange - simulate broken parent-child links (critical failure)
        when(noteBookService.get(1)).thenReturn(testNotebook);

        SampleItemAliquotRelationship brokenRelationship = mock(SampleItemAliquotRelationship.class);
        when(brokenRelationship.getChildSampleItem()).thenReturn(null);

        for (SampleItem sample : testSamples) {
            when(aliquotRelationshipDAO.getByParentSampleItemId(sample.getId()))
                    .thenReturn(Arrays.asList(brokenRelationship));
        }

        when(sampleRoutingService.getByNotebookId(1)).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(Collections.emptyList());
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(Collections.emptyList());

        // Act - should throw
        archivingService.finalizeNotebook(1, "1");
    }

    // ==================== Archiving Progress Tests ====================

    /**
     * T134: Get archiving progress with archived sample counts.
     */
    @Test
    public void testGetArchivingProgress_PartiallyArchived_ReturnsCorrectCounts() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);

        // 6 samples archived (3 parent, 3 child)
        List<SampleRouting> biorepositoryRoutings = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            SampleRouting routing = new SampleRouting();
            routing.setSampleItemId(i);
            routing.setDestinationType(DestinationType.BIOREPOSITORY);
            biorepositoryRoutings.add(routing);
        }

        when(sampleRoutingService.getByNotebookId(1)).thenReturn(biorepositoryRoutings);
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(biorepositoryRoutings);

        // First 5 are parents, next 5 are children
        for (int i = 1; i <= 5; i++) {
            when(aliquotRelationshipDAO.getByChildSampleItemId(String.valueOf(i))).thenReturn(Collections.emptyList());
        }
        for (int i = 6; i <= 10; i++) {
            SampleItemAliquotRelationship rel = mock(SampleItemAliquotRelationship.class);
            when(aliquotRelationshipDAO.getByChildSampleItemId(String.valueOf(i))).thenReturn(Arrays.asList(rel));
        }

        // For canFinalize check
        when(aliquotRelationshipDAO.getByParentSampleItemId(anyString())).thenReturn(Collections.emptyList());
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(Collections.emptyList());

        // Act
        ArchivingProgress progress = archivingService.getArchivingProgress(1);

        // Assert
        assertEquals("Total samples should be 10", 10, progress.totalSamples());
        assertEquals("Archived samples should be 6", 6, progress.archivedSamples());
        assertEquals("Pending samples should be 4", 4, progress.pendingSamples());
    }

    /**
     * Get archivable samples - separates parent and child.
     */
    @Test
    public void testGetArchivableSamples_ReturnsParentAndChildSeparately() {
        // Arrange
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(Collections.emptyList()); // None archived yet

        // First 5 are parents, next 5 are children
        for (int i = 1; i <= 5; i++) {
            when(aliquotRelationshipDAO.getByChildSampleItemId(String.valueOf(i))).thenReturn(Collections.emptyList());
        }
        for (int i = 6; i <= 10; i++) {
            SampleItemAliquotRelationship rel = mock(SampleItemAliquotRelationship.class);
            when(aliquotRelationshipDAO.getByChildSampleItemId(String.valueOf(i))).thenReturn(Arrays.asList(rel));
        }

        // Act
        Map<String, List<Integer>> samples = archivingService.getArchivableSamples(1);

        // Assert
        assertEquals("Should have 5 parent samples", 5, samples.get("parent").size());
        assertEquals("Should have 5 child samples", 5, samples.get("child").size());
    }

    // ==================== canFinalize Tests ====================

    /**
     * canFinalize returns true when no critical failures.
     */
    @Test
    public void testCanFinalize_NoCriticalFailures_ReturnsTrue() {
        // Arrange - all checks pass
        when(noteBookService.get(1)).thenReturn(testNotebook);
        when(aliquotRelationshipDAO.getByParentSampleItemId(anyString())).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookId(1)).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(Collections.emptyList());
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(Collections.emptyList());

        // Act
        boolean canFinalize = archivingService.canFinalize(1);

        // Assert
        assertTrue("Should be able to finalize", canFinalize);
    }

    /**
     * canFinalize returns false when critical failures exist.
     */
    @Test
    public void testCanFinalize_CriticalFailures_ReturnsFalse() {
        // Arrange - broken parent-child link (critical failure)
        when(noteBookService.get(1)).thenReturn(testNotebook);

        SampleItemAliquotRelationship brokenRelationship = mock(SampleItemAliquotRelationship.class);
        when(brokenRelationship.getChildSampleItem()).thenReturn(null);

        for (SampleItem sample : testSamples) {
            when(aliquotRelationshipDAO.getByParentSampleItemId(sample.getId()))
                    .thenReturn(Arrays.asList(brokenRelationship));
        }

        when(sampleRoutingService.getByNotebookId(1)).thenReturn(Collections.emptyList());
        when(sampleRoutingService.getByNotebookIdAndDestinationType(1, DestinationType.BIOREPOSITORY))
                .thenReturn(Collections.emptyList());
        when(notebookPageSampleService.getByNotebookId(1)).thenReturn(Collections.emptyList());

        // Act
        boolean canFinalize = archivingService.canFinalize(1);

        // Assert
        assertFalse("Should not be able to finalize with critical failures", canFinalize);
    }

    /**
     * Notebook not found returns false.
     */
    @Test
    public void testCanFinalize_NotebookNotFound_ReturnsFalse() {
        // Arrange
        when(noteBookService.get(999)).thenReturn(null);

        // Act
        boolean canFinalize = archivingService.canFinalize(999);

        // Assert
        assertFalse("Should not be able to finalize non-existent notebook", canFinalize);
    }
}
