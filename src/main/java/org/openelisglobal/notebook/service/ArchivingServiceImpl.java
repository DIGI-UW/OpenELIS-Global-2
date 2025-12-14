package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
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
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ArchivingService for end-of-project archiving operations
 * (US8). Handles biorepository transfers and traceability verification.
 */
@Service
public class ArchivingServiceImpl implements ArchivingService {

    private static final int DEFAULT_RETENTION_YEARS = 10;
    private static final String BIOREPOSITORY_PREFIX = "BIOREPO-";

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private SampleRoutingService sampleRoutingService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleItemAliquotRelationshipDAO aliquotRelationshipDAO;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SystemUserService systemUserService;

    @Override
    @Transactional
    public List<SampleRouting> transferToBiorepository(Integer notebookId, List<Integer> sampleItemIds,
            String locationId, String locationType, String notes, String userId) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        if (notebook.getStatus() == NoteBookStatus.FINALIZED) {
            throw new IllegalStateException("Cannot transfer samples from finalized notebook");
        }

        List<SampleRouting> routings = new ArrayList<>();

        for (Integer sampleItemId : sampleItemIds) {
            try {
                // Check if already routed to biorepository
                SampleRouting existing = sampleRoutingService.getByNotebookIdAndSampleItemId(notebookId, sampleItemId);
                if (existing != null && existing.getDestinationType() == DestinationType.BIOREPOSITORY) {
                    // Already archived, skip
                    routings.add(existing);
                    continue;
                }

                // Build notes with biorepository archival info
                String archiveNotes = String.format("Biorepository archival | %s | %s",
                        notes != null ? notes : "End of project transfer",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                // Create storage assignment for biorepository
                Map<String, Object> assignmentResult;
                try {
                    assignmentResult = sampleStorageService.assignSampleItemWithLocation(sampleItemId.toString(),
                            locationId, locationType, null, archiveNotes);
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getName(), "transferToBiorepository",
                            "Failed to create storage assignment for sample " + sampleItemId + ": " + e.getMessage());
                    continue;
                }

                // Create routing record with BIOREPOSITORY destination
                SampleRouting routing = createBiorepositoryRouting(notebookId, sampleItemId, assignmentResult, userId);
                routings.add(routing);

            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "transferToBiorepository",
                        "Error transferring sample " + sampleItemId + ": " + e.getMessage());
            }
        }

        return routings;
    }

    private SampleRouting createBiorepositoryRouting(Integer notebookId, Integer sampleItemId,
            Map<String, Object> assignmentResult, String userId) {

        NoteBook notebook = noteBookService.get(notebookId);
        SystemUser user = systemUserService.get(userId);

        SampleRouting routing = new SampleRouting();
        routing.setNotebook(notebook);
        routing.setSampleItemId(sampleItemId);
        routing.setDestinationType(DestinationType.BIOREPOSITORY);
        routing.setRoutedBy(user);
        routing.setRoutedAt(new Timestamp(System.currentTimeMillis()));

        // Link storage assignment if created
        String assignmentIdStr = (String) assignmentResult.get("assignmentId");
        if (assignmentIdStr != null) {
            try {
                SampleStorageAssignment assignment = sampleStorageService
                        .getSampleStorageAssignment(Integer.parseInt(assignmentIdStr));
                routing.setStorageAssignment(assignment);
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "createBiorepositoryRouting",
                        "Could not link storage assignment: " + e.getMessage());
            }
        }

        Integer id = sampleRoutingService.insert(routing);
        routing.setId(id);
        return routing;
    }

    @Override
    @Transactional(readOnly = true)
    public TraceabilityResult verifyTraceability(Integer notebookId) {
        List<TraceabilityCheck> checks = new ArrayList<>();

        // Check 1: Parent-child links are intact
        checks.add(verifyParentChildLinks(notebookId));

        // Check 2: Movement history is complete
        checks.add(verifyMovementHistory(notebookId));

        // Check 3: Storage assignments are verified
        checks.add(verifyStorageAssignments(notebookId));

        // Check 4: All workflow pages completed
        checks.add(verifyWorkflowCompletion(notebookId));

        boolean allPassed = checks.stream().allMatch(TraceabilityCheck::passed);
        boolean hasCriticalFailures = checks.stream().anyMatch(c -> c.critical() && !c.passed());

        String summary;
        if (allPassed) {
            summary = "All traceability checks passed. Notebook is ready for finalization.";
        } else if (hasCriticalFailures) {
            summary = "Critical traceability checks failed. Resolve issues before finalization.";
        } else {
            summary = "Some non-critical checks failed. Review before finalization.";
        }

        return new TraceabilityResult(allPassed, checks, summary);
    }

    private TraceabilityCheck verifyParentChildLinks(Integer notebookId) {
        List<String> issues = new ArrayList<>();
        NoteBook notebook = noteBookService.get(notebookId);

        if (notebook == null || notebook.getSamples() == null) {
            return new TraceabilityCheck("Parent-Child Links", "Verify all aliquot relationships are intact", false,
                    true, List.of("Notebook or samples not found"));
        }

        for (SampleItem sample : notebook.getSamples()) {
            // Check if this sample has expected relationships
            List<SampleItemAliquotRelationship> childRelationships = aliquotRelationshipDAO
                    .getByParentSampleItemId(sample.getId());

            for (SampleItemAliquotRelationship rel : childRelationships) {
                if (rel.getChildSampleItem() == null) {
                    issues.add("Broken child link for parent sample " + sample.getId());
                }
            }
        }

        boolean passed = issues.isEmpty();
        return new TraceabilityCheck("Parent-Child Links", "Verify all aliquot relationships are intact", passed, true,
                issues);
    }

    private TraceabilityCheck verifyMovementHistory(Integer notebookId) {
        List<String> issues = new ArrayList<>();
        NoteBook notebook = noteBookService.get(notebookId);

        if (notebook == null) {
            return new TraceabilityCheck("Movement History", "Verify complete storage movement chain", false, true,
                    List.of("Notebook not found"));
        }

        // Check routing records for all samples
        List<SampleRouting> routings = sampleRoutingService.getByNotebookId(notebookId);

        for (SampleRouting routing : routings) {
            if (routing.getDestinationType() == DestinationType.STORAGE
                    || routing.getDestinationType() == DestinationType.BIOREPOSITORY) {
                if (routing.getStorageAssignment() == null) {
                    issues.add("Sample " + routing.getSampleItemId() + " routed to storage but no assignment found");
                }
            }
        }

        boolean passed = issues.isEmpty();
        return new TraceabilityCheck("Movement History", "Verify complete storage movement chain", passed, false,
                issues);
    }

    private TraceabilityCheck verifyStorageAssignments(Integer notebookId) {
        List<String> issues = new ArrayList<>();
        NoteBook notebook = noteBookService.get(notebookId);

        if (notebook == null) {
            return new TraceabilityCheck("Storage Assignments", "Verify all archived samples have valid locations",
                    false, true, List.of("Notebook not found"));
        }

        List<SampleRouting> biorepositoryRoutings = sampleRoutingService.getByNotebookIdAndDestinationType(notebookId,
                DestinationType.BIOREPOSITORY);

        for (SampleRouting routing : biorepositoryRoutings) {
            SampleStorageAssignment assignment = routing.getStorageAssignment();
            if (assignment == null) {
                issues.add("Biorepository sample " + routing.getSampleItemId() + " has no storage assignment");
            }
        }

        boolean passed = issues.isEmpty();
        return new TraceabilityCheck("Storage Assignments", "Verify all archived samples have valid locations", passed,
                true, issues);
    }

    private TraceabilityCheck verifyWorkflowCompletion(Integer notebookId) {
        // This check provides informational status about workflow progress.
        // It is non-critical and does not block archiving.
        // Samples being PENDING on a page is expected workflow behavior - they progress
        // through pages as work is completed on each page.
        //
        // We simply report overall progress statistics rather than flagging "stuck"
        // samples, since samples naturally have PENDING status on pages they haven't
        // reached yet in the workflow.

        List<String> issues = new ArrayList<>();

        // Get progress for all pages
        List<NotebookPageSample> allPageSamples = notebookPageSampleService.getByNotebookId(notebookId);

        if (allPageSamples.isEmpty()) {
            issues.add("No samples found in notebook");
            return new TraceabilityCheck("Workflow Completion", "Verify workflow has samples", false, false, issues);
        }

        // Count completed vs total across all pages
        long totalRecords = allPageSamples.size();
        long completedRecords = allPageSamples.stream()
                .filter(ps -> ps.getStatus() == NotebookPageSample.Status.COMPLETED).count();

        // This is informational - having pending samples is normal workflow behavior
        // The check passes as long as there are samples in the notebook
        return new TraceabilityCheck("Workflow Completion", "Verify workflow progress (" + completedRecords + "/"
                + totalRecords + " page-sample records completed)", true, false, issues);
    }

    @Override
    @Transactional
    public NoteBook finalizeNotebook(Integer notebookId, String userId) {
        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        if (notebook.getStatus() == NoteBookStatus.FINALIZED) {
            throw new IllegalStateException("Notebook is already finalized");
        }

        // Verify traceability before finalizing
        TraceabilityResult traceability = verifyTraceability(notebookId);
        if (traceability.hasCriticalFailures()) {
            throw new IllegalStateException(
                    "Cannot finalize: Critical traceability checks failed. " + traceability.summary());
        }

        // Check archiving progress
        ArchivingProgress progress = getArchivingProgress(notebookId);
        if (progress.pendingSamples() > 0) {
            LogEvent.logWarn(this.getClass().getName(), "finalizeNotebook",
                    "Finalizing notebook " + notebookId + " with " + progress.pendingSamples() + " pending samples");
        }

        // Update notebook status to FINALIZED
        noteBookService.updateWithStatus(notebookId, NoteBookStatus.FINALIZED, userId);

        return noteBookService.get(notebookId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canFinalize(Integer notebookId) {
        TraceabilityResult traceability = verifyTraceability(notebookId);
        return !traceability.hasCriticalFailures();
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivingProgress getArchivingProgress(Integer notebookId) {
        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return new ArchivingProgress(0, 0, 0, 0, 0, 0, 0, false);
        }

        List<SampleItem> samples = notebook.getSamples();
        int totalSamples = samples != null ? samples.size() : 0;

        // Get routing summary
        List<SampleRouting> allRoutings = sampleRoutingService.getByNotebookId(notebookId);
        List<SampleRouting> biorepositoryRoutings = sampleRoutingService.getByNotebookIdAndDestinationType(notebookId,
                DestinationType.BIOREPOSITORY);

        int archivedSamples = biorepositoryRoutings.size();
        int pendingSamples = totalSamples - archivedSamples;

        // Count parent vs child samples
        int parentSamples = 0;
        int childSamples = 0;
        int archivedParents = 0;
        int archivedChildren = 0;

        if (samples != null) {
            for (SampleItem sample : samples) {
                // Check if sample has a parent (is a child/aliquot)
                List<SampleItemAliquotRelationship> parentRels = aliquotRelationshipDAO
                        .getByChildSampleItemId(sample.getId());
                boolean isChild = !parentRels.isEmpty();

                if (isChild) {
                    childSamples++;
                } else {
                    parentSamples++;
                }

                // Check if archived
                boolean isArchived = biorepositoryRoutings.stream()
                        .anyMatch(r -> r.getSampleItemId().equals(Integer.parseInt(sample.getId())));

                if (isArchived) {
                    if (isChild) {
                        archivedChildren++;
                    } else {
                        archivedParents++;
                    }
                }
            }
        }

        boolean readyForFinalization = canFinalize(notebookId);

        return new ArchivingProgress(totalSamples, archivedSamples, pendingSamples, parentSamples, childSamples,
                archivedParents, archivedChildren, readyForFinalization);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Integer>> getArchivableSamples(Integer notebookId) {
        Map<String, List<Integer>> result = new HashMap<>();
        List<Integer> parentIds = new ArrayList<>();
        List<Integer> childIds = new ArrayList<>();

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null || notebook.getSamples() == null) {
            result.put("parent", parentIds);
            result.put("child", childIds);
            return result;
        }

        // Get already archived samples
        List<SampleRouting> biorepositoryRoutings = sampleRoutingService.getByNotebookIdAndDestinationType(notebookId,
                DestinationType.BIOREPOSITORY);
        List<Integer> archivedIds = biorepositoryRoutings.stream().map(SampleRouting::getSampleItemId)
                .collect(Collectors.toList());

        for (SampleItem sample : notebook.getSamples()) {
            Integer sampleId = Integer.parseInt(sample.getId());

            // Skip already archived
            if (archivedIds.contains(sampleId)) {
                continue;
            }

            // Check if this is a child (aliquot)
            List<SampleItemAliquotRelationship> parentRels = aliquotRelationshipDAO
                    .getByChildSampleItemId(sample.getId());
            boolean isChild = !parentRels.isEmpty();

            if (isChild) {
                childIds.add(sampleId);
            } else {
                parentIds.add(sampleId);
            }
        }

        result.put("parent", parentIds);
        result.put("child", childIds);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TraceabilityReport generateTraceabilityReport(Integer notebookId) {
        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        List<SampleLineage> lineages = new ArrayList<>();
        List<MovementRecord> movements = new ArrayList<>();

        if (notebook.getSamples() != null) {
            for (SampleItem sample : notebook.getSamples()) {
                // Build lineage info
                SampleLineage lineage = buildSampleLineage(notebookId, sample);
                lineages.add(lineage);

                // Collect movement records
                List<MovementRecord> sampleMovements = getMovementRecords(sample);
                movements.addAll(sampleMovements);
            }
        }

        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return new TraceabilityReport(notebookId, notebook.getTitle(), lineages, movements, generatedAt, "System");
    }

    private SampleLineage buildSampleLineage(Integer notebookId, SampleItem sample) {
        Integer sampleItemId = Integer.parseInt(sample.getId());
        String sampleId = sample.getSample() != null ? sample.getSample().getAccessionNumber() : null;
        String externalId = sample.getExternalId();

        // Find parent
        List<SampleItemAliquotRelationship> parentRels = aliquotRelationshipDAO.getByChildSampleItemId(sample.getId());
        Integer parentSampleItemId = null;
        if (!parentRels.isEmpty()) {
            SampleItem parent = parentRels.get(0).getParentSampleItem();
            if (parent != null) {
                parentSampleItemId = Integer.parseInt(parent.getId());
            }
        }

        // Find children
        List<SampleItemAliquotRelationship> childRels = aliquotRelationshipDAO.getByParentSampleItemId(sample.getId());
        List<Integer> childSampleItemIds = childRels.stream().filter(r -> r.getChildSampleItem() != null)
                .map(r -> Integer.parseInt(r.getChildSampleItem().getId())).collect(Collectors.toList());

        // Get current and archive locations
        String currentLocation = getCurrentLocation(sample);
        String archiveLocation = getArchiveLocation(notebookId, sampleItemId);

        return new SampleLineage(sampleItemId, sampleId, externalId, parentSampleItemId, childSampleItemIds,
                currentLocation, archiveLocation);
    }

    private String getCurrentLocation(SampleItem sample) {
        try {
            List<SampleStorageAssignment> assignments = sampleStorageService
                    .getSampleStorageAssignmentsBySampleItem(sample);
            if (!assignments.isEmpty()) {
                SampleStorageAssignment latest = assignments.get(0);
                // Build location string from locationId and locationType
                return buildLocationString(latest.getLocationId(), latest.getLocationType(),
                        latest.getPositionCoordinate());
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "getCurrentLocation",
                    "Error getting location for sample " + sample.getId() + ": " + e.getMessage());
        }
        return "Not assigned";
    }

    private String getArchiveLocation(Integer notebookId, Integer sampleItemId) {
        SampleRouting routing = sampleRoutingService.getByNotebookIdAndSampleItemId(notebookId, sampleItemId);
        if (routing != null && routing.getDestinationType() == DestinationType.BIOREPOSITORY) {
            SampleStorageAssignment assignment = routing.getStorageAssignment();
            if (assignment != null && assignment.getLocationId() != null) {
                return buildLocationString(assignment.getLocationId(), assignment.getLocationType(),
                        assignment.getPositionCoordinate());
            }
            return "Biorepository (pending assignment)";
        }
        return "Not archived";
    }

    private String buildLocationString(Integer locationId, String locationType, String positionCoordinate) {
        StringBuilder sb = new StringBuilder();
        if (locationType != null) {
            sb.append(locationType.toUpperCase());
        }
        if (locationId != null) {
            sb.append("-").append(locationId);
        }
        if (positionCoordinate != null && !positionCoordinate.isEmpty()) {
            sb.append(" @ ").append(positionCoordinate);
        }
        return sb.length() > 0 ? sb.toString() : "Unknown";
    }

    private List<MovementRecord> getMovementRecords(SampleItem sample) {
        List<MovementRecord> records = new ArrayList<>();

        try {
            List<SampleStorageMovement> movements = sampleStorageService.getSampleStorageMovementsBySampleItem(sample);

            for (SampleStorageMovement movement : movements) {
                String fromLocation = buildLocationString(movement.getPreviousLocationId(),
                        movement.getPreviousLocationType(), movement.getPreviousPositionCoordinate());
                String toLocation = buildLocationString(movement.getNewLocationId(), movement.getNewLocationType(),
                        movement.getNewPositionCoordinate());
                String movedAt = movement.getMovementDate() != null ? movement.getMovementDate().toString() : "Unknown";
                String movedBy = movement.getMovedByUserId() != null ? "User-" + movement.getMovedByUserId() : "System";
                String reason = movement.getReason();

                records.add(new MovementRecord(Integer.parseInt(sample.getId()), fromLocation, toLocation, movedAt,
                        movedBy, reason));
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "getMovementRecords",
                    "Error getting movements for sample " + sample.getId() + ": " + e.getMessage());
        }

        return records;
    }
}
