package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.SampleRoutingDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.openelisglobal.notebook.valueholder.StorageCondition;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for SampleRouting operations.
 */
@Service
public class SampleRoutingServiceImpl extends AuditableBaseObjectServiceImpl<SampleRouting, Integer>
        implements SampleRoutingService {

    @Autowired
    private SampleRoutingDAO baseObjectDAO;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private StorageBoxDAO storageBoxDAO;

    @Autowired
    private SampleStorageAssignmentDAO storageAssignmentDAO;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SystemUserService systemUserService;

    public SampleRoutingServiceImpl() {
        super(SampleRouting.class);
    }

    @Override
    protected BaseDAO<SampleRouting, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRouting> getByNotebookId(Integer notebookId) {
        return baseObjectDAO.getByNotebookId(notebookId);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleRouting getByNotebookIdAndSampleItemId(Integer notebookId, Integer sampleItemId) {
        return baseObjectDAO.getByNotebookIdAndSampleItemId(notebookId, sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRouting> getByNotebookIdAndDestinationType(Integer notebookId, DestinationType destinationType) {
        return baseObjectDAO.getByNotebookIdAndDestinationType(notebookId, destinationType);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleRouting getByBoxAndWell(Integer notebookId, Integer boxId, String wellCoordinate) {
        return baseObjectDAO.getByBoxAndWell(notebookId, boxId, wellCoordinate);
    }

    @Override
    @Transactional
    public SampleRouting routeToInternalAnalysis(Integer notebookId, Integer sampleItemId, Integer boxId,
            String wellCoordinate, String userId) {
        // Validate box exists
        StorageBox box = storageBoxDAO.get(boxId).orElse(null);
        if (box == null) {
            throw new IllegalArgumentException("StorageBox not found: " + boxId);
        }

        // Check well availability
        if (!isWellAvailable(notebookId, boxId, wellCoordinate)) {
            throw new IllegalStateException("Well " + wellCoordinate + " is already assigned in box " + boxId);
        }

        SampleRouting routing = createBaseRouting(notebookId, sampleItemId, userId);
        routing.setDestinationType(DestinationType.INTERNAL_ANALYSIS);
        routing.setBox(box);
        routing.setWellCoordinate(wellCoordinate);

        Integer id = insert(routing);
        routing.setId(id);
        return routing;
    }

    @Override
    @Transactional
    public SampleRouting routeToAssayPlate(Integer notebookId, Integer sampleItemId, String assayPlateName,
            String wellCoordinate, String userId) {
        if (assayPlateName == null || assayPlateName.isBlank()) {
            throw new IllegalArgumentException("Assay plate name is required");
        }
        if (wellCoordinate == null || wellCoordinate.isBlank()) {
            throw new IllegalArgumentException("Well coordinate is required");
        }

        SampleRouting routing = createBaseRouting(notebookId, sampleItemId, userId);
        routing.setDestinationType(DestinationType.INTERNAL_ANALYSIS);
        routing.setAssayPlateName(assayPlateName);
        routing.setWellCoordinate(wellCoordinate);
        // Note: box is null for assay plates - they are temporary and not in storage
        // hierarchy

        Integer id = insert(routing);
        routing.setId(id);
        return routing;
    }

    @Override
    @Transactional
    public int bulkRouteToAssayPlate(Integer notebookId, List<Integer> sampleItemIds, String assayPlateName,
            Map<Integer, String> wellAssignments, String userId) {
        int routed = 0;

        for (Integer sampleItemId : sampleItemIds) {
            String wellCoordinate = wellAssignments.get(sampleItemId);
            if (wellCoordinate != null) {
                try {
                    routeToAssayPlate(notebookId, sampleItemId, assayPlateName, wellCoordinate, userId);
                    // NOTE: Assay plates are temporary and NOT connected to hierarchical storage.
                    // No SampleStorageAssignment is created.
                    routed++;
                } catch (IllegalStateException e) {
                    // Skip already routed samples
                    LogEvent.logWarn(this.getClass().getName(), "bulkRouteToAssayPlate",
                            "Skipping already routed sample " + sampleItemId + ": " + e.getMessage());
                }
            }
        }
        return routed;
    }

    @Override
    @Transactional
    public SampleRouting routeToExternalLab(Integer notebookId, Integer sampleItemId, String externalLabName,
            LocalDate shipmentDate, String userId) {
        if (externalLabName == null || externalLabName.isBlank()) {
            throw new IllegalArgumentException("External lab name is required");
        }

        SampleRouting routing = createBaseRouting(notebookId, sampleItemId, userId);
        routing.setDestinationType(DestinationType.EXTERNAL_LAB);
        routing.setExternalLabName(externalLabName);
        routing.setShipmentDate(shipmentDate);

        Integer id = insert(routing);
        routing.setId(id);
        return routing;
    }

    @Override
    @Transactional
    public SampleRouting routeToStorage(Integer notebookId, Integer sampleItemId, Integer storageAssignmentId,
            String userId) {
        // If storageAssignmentId is provided, use existing assignment
        if (storageAssignmentId != null) {
            SampleStorageAssignment assignment = storageAssignmentDAO.get(storageAssignmentId).orElse(null);
            if (assignment == null) {
                throw new IllegalArgumentException("StorageAssignment not found: " + storageAssignmentId);
            }

            SampleRouting routing = createBaseRouting(notebookId, sampleItemId, userId);
            routing.setDestinationType(DestinationType.STORAGE);
            routing.setStorageAssignment(assignment);

            Integer id = insert(routing);
            routing.setId(id);
            return routing;
        }

        // If no assignment ID, just create a STORAGE routing record without assignment
        // The actual storage assignment can be done later via the Storage Management
        // page
        SampleRouting routing = createBaseRouting(notebookId, sampleItemId, userId);
        routing.setDestinationType(DestinationType.STORAGE);

        Integer id = insert(routing);
        routing.setId(id);
        return routing;
    }

    @Override
    @Transactional
    public SampleRouting routeToStorageWithBox(Integer notebookId, Integer sampleItemId, Integer boxId,
            String wellCoordinate, String userId) {
        // Validate box exists
        StorageBox box = storageBoxDAO.get(boxId).orElse(null);
        if (box == null) {
            throw new IllegalArgumentException("StorageBox not found: " + boxId);
        }

        // Check well availability (using notebook context for storage)
        if (!isWellAvailable(notebookId, boxId, wellCoordinate)) {
            throw new IllegalStateException("Well " + wellCoordinate + " is already assigned in box " + boxId);
        }

        SampleRouting routing = createBaseRouting(notebookId, sampleItemId, userId);
        routing.setDestinationType(DestinationType.STORAGE);
        routing.setBox(box);
        routing.setWellCoordinate(wellCoordinate);

        Integer id = insert(routing);
        routing.setId(id);
        return routing;
    }

    @Override
    @Transactional
    public int bulkRouteToStorage(Integer notebookId, List<Integer> sampleItemIds, Integer boxId,
            Map<Integer, String> wellAssignments, String userId) {
        int count = 0;
        NoteBook notebook = noteBookService.get(notebookId);
        String notebookTitle = notebook != null ? notebook.getTitle() : String.valueOf(notebookId);

        for (Integer sampleItemId : sampleItemIds) {
            String well = wellAssignments.get(sampleItemId);
            if (well == null) {
                // Skip samples without well assignments
                continue;
            }
            routeToStorageWithBox(notebookId, sampleItemId, boxId, well, userId);

            // Also create/update SampleStorageAssignment for global Storage Management
            // integration
            assignOrMoveToStorage(sampleItemId, boxId, well, notebookTitle, "Storage");

            count++;
        }
        return count;
    }

    /**
     * Helper method to assign or move a sample to storage. Checks if sample already
     * has assignment RECORD (not just location) and uses move if so, otherwise
     * creates new assignment.
     *
     * Note: SampleStorageAssignment has a UNIQUE constraint on sample_item_id, so
     * we must check for assignment record existence, not just whether it has a
     * location set. An assignment can exist but have null location (e.g., after
     * disposal).
     */
    private void assignOrMoveToStorage(Integer sampleItemId, Integer boxId, String wellCoordinate, String notebookTitle,
            String routingType) {
        try {
            String notes = String.format("Notebook: %s | %s | Box: %d | Well: %s", notebookTitle, routingType, boxId,
                    wellCoordinate);

            // Check if sample already has a storage assignment RECORD (not just location)
            // getSampleItemLocation returns empty map if no assignment exists, but we need
            // to check the actual assignment record because it could exist with null
            // location
            Map<String, Object> existingLocation = sampleStorageService.getSampleItemLocation(sampleItemId.toString());

            // Check if there's ANY data in the map (indicating an assignment record exists)
            // The map will have sampleItemId key if an assignment was found, even if
            // location is empty
            boolean hasExistingAssignment = existingLocation != null && !existingLocation.isEmpty()
                    && existingLocation.containsKey("sampleItemId");

            if (hasExistingAssignment) {
                // Use move instead of assign (move updates existing assignment)
                sampleStorageService.moveSampleItemWithLocation(sampleItemId.toString(), boxId.toString(), "box",
                        wellCoordinate, notes, null);
                LogEvent.logInfo(this.getClass().getName(), "assignOrMoveToStorage",
                        "Moved SampleStorageAssignment for sample " + sampleItemId + " to box " + boxId + " well "
                                + wellCoordinate);
            } else {
                // Create new assignment (no existing assignment record)
                sampleStorageService.assignSampleItemWithLocation(sampleItemId.toString(), boxId.toString(), "box",
                        wellCoordinate, notes);
                LogEvent.logInfo(this.getClass().getName(), "assignOrMoveToStorage",
                        "Created SampleStorageAssignment for sample " + sampleItemId + " at box " + boxId + " well "
                                + wellCoordinate);
            }
        } catch (Exception e) {
            // Log but don't fail - the routing record was created successfully
            LogEvent.logWarn(this.getClass().getName(), "assignOrMoveToStorage",
                    "Failed to create/move SampleStorageAssignment for sample " + sampleItemId + ": " + e.getMessage());
        }
    }

    private SampleRouting createBaseRouting(Integer notebookId, Integer sampleItemId, String userId) {
        // Check if routing already exists
        SampleRouting existing = getByNotebookIdAndSampleItemId(notebookId, sampleItemId);
        if (existing != null) {
            throw new IllegalStateException("Sample " + sampleItemId + " is already routed in notebook " + notebookId);
        }

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        SystemUser user = systemUserService.get(userId);

        SampleRouting routing = new SampleRouting();
        routing.setNotebook(notebook);
        routing.setSampleItemId(sampleItemId);
        routing.setRoutedBy(user);
        routing.setRoutedAt(new Timestamp(System.currentTimeMillis()));

        return routing;
    }

    @Override
    @Transactional
    public int bulkRouteToInternalAnalysis(Integer notebookId, List<Integer> sampleItemIds, Integer boxId,
            Map<Integer, String> wellAssignments, String userId) {
        int routed = 0;

        for (Integer sampleItemId : sampleItemIds) {
            String wellCoordinate = wellAssignments.get(sampleItemId);
            if (wellCoordinate != null) {
                try {
                    routeToInternalAnalysis(notebookId, sampleItemId, boxId, wellCoordinate, userId);
                    // NOTE: Internal Analysis uses assay plates (temporary), NOT connected to
                    // hierarchical storage. Only STORAGE routing creates SampleStorageAssignment.
                    routed++;
                } catch (IllegalStateException e) {
                    // Skip already routed samples
                }
            }
        }
        return routed;
    }

    @Override
    @Transactional(readOnly = true)
    public RoutingSummary getRoutingSummary(Integer notebookId) {
        long internal = baseObjectDAO.getCountByDestinationType(notebookId, DestinationType.INTERNAL_ANALYSIS);
        long external = baseObjectDAO.getCountByDestinationType(notebookId, DestinationType.EXTERNAL_LAB);
        long storage = baseObjectDAO.getCountByDestinationType(notebookId, DestinationType.STORAGE);
        long unrouted = baseObjectDAO.getUnroutedSampleCount(notebookId);

        return new RoutingSummary(internal, external, storage, unrouted);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWellAvailable(Integer notebookId, Integer boxId, String wellCoordinate) {
        SampleRouting existing = baseObjectDAO.getByBoxAndWell(notebookId, boxId, wellCoordinate);
        return existing == null;
    }

    @Override
    public String generateWellCoordinate(int index, int columnsPerRow) {
        if (index < 0 || columnsPerRow <= 0) {
            throw new IllegalArgumentException("Index and columns must be positive");
        }
        int row = index / columnsPerRow;
        int col = (index % columnsPerRow) + 1;
        char rowLetter = (char) ('A' + row);
        return String.valueOf(rowLetter) + col;
    }

    @Override
    @Transactional
    public Map<Integer, String> autoAssignWells(Integer notebookId, List<Integer> sampleItemIds, Integer boxId,
            int columnsPerRow) {
        Map<Integer, String> assignments = new HashMap<>();
        int maxWells = columnsPerRow * 8; // 8 rows for standard 96-well plate
        int currentIndex = 0;

        for (Integer sampleItemId : sampleItemIds) {
            // Find the next available well
            while (currentIndex < maxWells) {
                String wellCoordinate = generateWellCoordinate(currentIndex, columnsPerRow);
                if (isWellAvailable(notebookId, boxId, wellCoordinate)) {
                    assignments.put(sampleItemId, wellCoordinate);
                    currentIndex++;
                    break;
                }
                currentIndex++;
            }

            // If we've exhausted all wells, stop
            if (currentIndex >= maxWells) {
                break;
            }
        }

        return assignments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRouting> getByBoxId(Integer notebookId, Integer boxId) {
        return baseObjectDAO.getByBoxId(notebookId, boxId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SampleRouting> getBoxLayout(Integer notebookId, Integer boxId) {
        List<SampleRouting> routings = getByBoxId(notebookId, boxId);
        Map<String, SampleRouting> layout = new HashMap<>();
        for (SampleRouting routing : routings) {
            if (routing.getWellCoordinate() != null) {
                layout.put(routing.getWellCoordinate(), routing);
            }
        }
        return layout;
    }

    @Override
    @Transactional
    public SampleRouting routeToStorage(Integer notebookId, Integer sampleItemId, String locationId,
            String locationType, String positionCoordinate, StorageCondition condition, int retentionYears,
            String userId) {

        // Build notes with storage condition and retention info
        LocalDate expiryDate = calculateExpiryDate(retentionYears);
        String notes = String.format("Storage condition: %s | Retention: %d years | Expiry: %s",
                condition != null ? condition.name() : "UNSPECIFIED", retentionYears, expiryDate);

        // Use SampleStorageService to create the storage assignment (with audit trail)
        Map<String, Object> assignmentResult;
        try {
            assignmentResult = sampleStorageService.assignSampleItemWithLocation(sampleItemId.toString(), locationId,
                    locationType, positionCoordinate, notes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create storage assignment: " + e.getMessage(), e);
        }

        // Get the assignment ID from the result
        String assignmentIdStr = (String) assignmentResult.get("assignmentId");
        Integer assignmentId = assignmentIdStr != null ? Integer.parseInt(assignmentIdStr) : null;

        // Retrieve the created assignment to link to routing
        SampleStorageAssignment assignment = null;
        if (assignmentId != null) {
            assignment = storageAssignmentDAO.get(assignmentId).orElse(null);
        }

        // Create the routing record
        SampleRouting routing = createBaseRouting(notebookId, sampleItemId, userId);
        routing.setDestinationType(DestinationType.STORAGE);
        routing.setStorageAssignment(assignment);

        Integer id = insert(routing);
        routing.setId(id);
        return routing;
    }

    @Override
    @Transactional
    public int bulkRouteToStorage(Integer notebookId, List<Integer> sampleItemIds, String locationId,
            String locationType, StorageCondition condition, int retentionYears, String userId) {

        int routed = 0;
        for (Integer sampleItemId : sampleItemIds) {
            try {
                routeToStorage(notebookId, sampleItemId, locationId, locationType, null, condition, retentionYears,
                        userId);
                routed++;
            } catch (IllegalStateException e) {
                // Skip already routed samples
            }
        }
        return routed;
    }

    @Override
    public LocalDate calculateExpiryDate(int retentionYears) {
        return LocalDate.now().plusYears(retentionYears);
    }
}
