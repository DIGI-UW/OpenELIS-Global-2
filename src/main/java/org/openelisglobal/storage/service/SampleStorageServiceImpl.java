package org.openelisglobal.storage.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.biorepository.dao.BioSampleDAO;
import org.openelisglobal.biorepository.service.ChainOfCustodyService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.valueholder.*;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of SampleStorageService - Handles sample assignment and
 * movement
 */
@Service
@Transactional
public class SampleStorageServiceImpl implements SampleStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageServiceImpl.class);
    private static final String DEFAULT_SYSTEM_USER_ID = "1";

    @Autowired
    private SampleItemDAO sampleItemDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private BioSampleDAO bioSampleDAO;

    @Autowired
    private ChainOfCustodyService chainOfCustodyService;

    @Autowired
    private SystemUserService systemUserService;

    @Override
    public CapacityWarning calculateCapacity(StorageRack rack) {
        // Calculate total capacity from boxes in this rack
        List<StorageBox> boxes = storageLocationService.getBoxesByRack(rack.getId());
        int totalCapacity = boxes.stream().mapToInt(box -> box.getCapacity() != null ? box.getCapacity() : 0).sum();

        if (totalCapacity == 0) {
            return null; // No boxes with capacity defined
        }

        int occupied = storageLocationService.countOccupied(rack.getId());
        int percentage = (occupied * 100) / totalCapacity;

        String warningMessage = null;
        if (percentage >= 100) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", rack.getLabel(),
                    percentage);
        } else if (percentage >= 90) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", rack.getLabel(),
                    percentage);
        } else if (percentage >= 80) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", rack.getLabel(),
                    percentage);
        }

        return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSamplesWithAssignments() {
        // Get ALL sample items first, then LEFT JOIN with assignments
        List<SampleItem> allSampleItems = sampleItemDAO.getAllSampleItems();
        logger.info("getAllSamplesWithAssignments: Found {} total sample items", allSampleItems.size());

        // Get all assignments and build a map by sampleItemId for efficient lookup
        // SampleItem.id is String but assignment.sampleItemId is Integer (DB column is
        // numeric)
        List<SampleStorageAssignment> assignments = sampleStorageAssignmentDAO.getAll();
        java.util.Map<String, SampleStorageAssignment> assignmentsBySampleItemId = new java.util.HashMap<>();
        for (SampleStorageAssignment assignment : assignments) {
            if (assignment.getSampleItemId() != null) {
                // Convert Integer to String for map key (SampleItem.id is String)
                assignmentsBySampleItemId.put(assignment.getSampleItemId().toString(), assignment);
            }
        }
        logger.info("getAllSamplesWithAssignments: Found {} total assignments", assignments.size());

        List<Map<String, Object>> response = new java.util.ArrayList<>();

        for (SampleItem sampleItem : allSampleItems) {
            if (sampleItem == null || sampleItem.getId() == null) {
                continue;
            }

            Map<String, Object> map = new java.util.HashMap<>();
            // Numeric ID (String representation) - primary identifier
            map.put("id", sampleItem.getId());
            // @deprecated Use 'id' field instead. Kept for backward compatibility only.
            // This field is identical to 'id' and will be removed in a future release.
            map.put("sampleItemId", sampleItem.getId());
            // External ID - user-friendly identifier (e.g., "EXT-1765401458866")
            map.put("sampleItemExternalId", sampleItem.getExternalId() != null ? sampleItem.getExternalId() : "");

            // Get parent Sample accession number for context
            if (sampleItem.getSample() != null) {
                map.put("sampleAccessionNumber",
                        sampleItem.getSample().getAccessionNumber() != null
                                ? sampleItem.getSample().getAccessionNumber()
                                : "");
            } else {
                map.put("sampleAccessionNumber", "");
            }
            map.put("type",
                    sampleItem.getTypeOfSample() != null && sampleItem.getTypeOfSample().getDescription() != null
                            ? sampleItem.getTypeOfSample().getDescription()
                            : "");
            // Store actual status ID for filtering (OGC-150: supports all status types from
            // dropdown)
            // Frontend dropdown loads all status types and filters by ID
            // Default to "active" if no status ID (backward compatibility)
            map.put("status", sampleItem.getStatusId() != null ? sampleItem.getStatusId() : "active");

            // Check if this sample item has an assignment
            SampleStorageAssignment assignment = assignmentsBySampleItemId.get(sampleItem.getId());
            if (assignment != null && assignment.getLocationId() != null && assignment.getLocationType() != null) {
                // Build hierarchical path based on locationType
                String hierarchicalPath = buildHierarchicalPathForAssignment(assignment);

                map.put("location", hierarchicalPath != null ? hierarchicalPath : "");
                map.put("assignedBy", assignment.getAssignedByUserId());
                map.put("date", assignment.getAssignedDate() != null ? assignment.getAssignedDate().toString() : "");
                // Include position coordinate and notes as separate fields for editing
                String posCoord = assignment.getPositionCoordinate() != null ? assignment.getPositionCoordinate() : "";
                String notesVal = assignment.getNotes() != null ? assignment.getNotes() : "";
                map.put("positionCoordinate", posCoord);
                map.put("notes", notesVal);

                // Debug: Log first 3 samples with assignments
                if (response.size() < 3) {
                    logger.info(
                            "DEBUG getAllSamplesWithAssignments - Sample #{}: ID={}, positionCoordinate='{}', notes='{}', mapKeys={}",
                            response.size() + 1, sampleItem.getId(), posCoord, notesVal, map.keySet());
                }
            } else {
                // No assignment - sample is unassigned
                map.put("location", "");
                map.put("assignedBy", null);
                map.put("date", "");
                map.put("positionCoordinate", "");
                map.put("notes", "");
            }

            response.add(map);
        }

        // Sort by location: assigned samples first (alphabetically by location), then
        // unassigned
        response.sort((a, b) -> {
            String locA = (String) a.get("location");
            String locB = (String) b.get("location");
            boolean aEmpty = locA == null || locA.isEmpty();
            boolean bEmpty = locB == null || locB.isEmpty();

            // Both empty - sort by sample ID
            if (aEmpty && bEmpty) {
                return String.valueOf(a.get("id")).compareTo(String.valueOf(b.get("id")));
            }
            // Empty locations go to the end
            if (aEmpty)
                return 1;
            if (bEmpty)
                return -1;
            // Both have locations - sort alphabetically
            return locA.compareTo(locB);
        });

        logger.info("getAllSamplesWithAssignments: Returning {} SampleItems (assigned and unassigned)",
                response.size());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleItemLocation(String sampleItemId) {
        if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
            return new HashMap<>();
        }

        SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findBySampleItemId(sampleItemId);
        if (assignment == null) {
            return new HashMap<>();
        }

        return buildLocationDetailsFromAssignment(assignment, new HashMap<>(), new HashMap<>(), new HashMap<>(),
                new HashMap<>(), new HashMap<>());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> getSampleItemLocations(List<String> sampleItemIds) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            return result;
        }

        List<Integer> numericSampleItemIds = new java.util.ArrayList<>();
        for (String sampleItemId : sampleItemIds) {
            if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
                continue;
            }
            try {
                numericSampleItemIds.add(Integer.parseInt(sampleItemId.trim()));
            } catch (NumberFormatException e) {
                logger.debug("Skipping non-numeric SampleItem ID in batch location lookup: {}", sampleItemId);
            }
        }

        if (numericSampleItemIds.isEmpty()) {
            return result;
        }

        List<SampleStorageAssignment> assignments = sampleStorageAssignmentDAO.findBySampleItemIds(numericSampleItemIds);
        if (assignments.isEmpty()) {
            return result;
        }

        Map<Integer, StorageRoom> roomCache = new HashMap<>();
        Map<Integer, StorageDevice> deviceCache = new HashMap<>();
        Map<Integer, StorageShelf> shelfCache = new HashMap<>();
        Map<Integer, StorageRack> rackCache = new HashMap<>();
        Map<Integer, StorageBox> boxCache = new HashMap<>();

        for (SampleStorageAssignment assignment : assignments) {
            if (assignment == null || assignment.getSampleItemId() == null) {
                continue;
            }

            result.put(assignment.getSampleItemId().toString(),
                    buildLocationDetailsFromAssignment(assignment, roomCache, deviceCache, shelfCache, rackCache,
                            boxCache));
        }

        return result;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, Object> updateAssignmentMetadata(String sampleItemId, String positionCoordinate, String notes) {
        if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
            throw new LIMSRuntimeException("SampleItem ID is required");
        }
        SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO.findBySampleItemId(sampleItemId);
        if (existingAssignment == null) {
            throw new LIMSRuntimeException("No storage assignment found for SampleItem: " + sampleItemId);
        }

        if (positionCoordinate != null) {
            if (positionCoordinate.trim().isEmpty()) {
                existingAssignment.setPositionCoordinate(null);
            } else {
                existingAssignment.setPositionCoordinate(positionCoordinate.trim());
            }
        }
        if (notes != null) {
            if (notes.trim().isEmpty()) {
                existingAssignment.setNotes(null);
            } else {
                existingAssignment.setNotes(notes.trim());
            }
        }

        sampleStorageAssignmentDAO.update(existingAssignment);

        Map<String, Object> response = new HashMap<>();
        response.put("assignmentId", existingAssignment.getId());
        response.put("sampleItemId", sampleItemId);
        response.put("positionCoordinate", existingAssignment.getPositionCoordinate());
        response.put("notes", existingAssignment.getNotes());
        response.put("updatedDate", new Timestamp(System.currentTimeMillis()).toString());

        String hierarchicalPath = buildHierarchicalPathForAssignment(existingAssignment);
        response.put("hierarchicalPath", hierarchicalPath);
        return response;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, Object> disposeSampleItem(String sampleItemId, String reason, String method, String notes) {
        return disposeSampleItem(sampleItemId, reason, method, notes, DEFAULT_SYSTEM_USER_ID);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, Object> disposeSampleItem(String sampleItemId, String reason, String method, String notes,
            String sysUserId) {
        try {
            // Validate inputs
            if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
                throw new LIMSRuntimeException("SampleItem ID is required");
            }
            if (reason == null || reason.trim().isEmpty()) {
                throw new LIMSRuntimeException("Disposal reason is required");
            }
            if (method == null || method.trim().isEmpty()) {
                throw new LIMSRuntimeException("Disposal method is required");
            }

            // Resolve SampleItem (handles internal ID, accession number, or external ID)
            SampleItem sampleItem = resolveSampleItem(sampleItemId);
            BioSample bioSample = findBioSample(sampleItem);
            String workflowStatusBefore = bioSample != null && bioSample.getWorkflowStatus() != null
                    ? bioSample.getWorkflowStatus().name()
                    : null;

            // Check if already disposed
            if (statusService.matches(sampleItem.getStatusId(),
                    org.openelisglobal.common.services.StatusService.SampleStatus.Disposed)) {
                throw new LIMSRuntimeException("SampleItem is already disposed");
            }

            // Find existing assignment to get previous location
            SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO
                    .findBySampleItemId(sampleItem.getId());
            String previousLocation = null;
            Integer previousLocationId = null;
            String previousLocationType = null;
            String previousPositionCoordinate = null;

            if (existingAssignment != null) {
                previousLocationId = existingAssignment.getLocationId();
                previousLocationType = existingAssignment.getLocationType();
                previousPositionCoordinate = existingAssignment.getPositionCoordinate();

                // Build hierarchical path for audit log
                if (previousLocationId != null && previousLocationType != null) {
                    Object locationEntity = null;
                    switch (previousLocationType) {
                    case "box":
                        locationEntity = storageLocationService.get(previousLocationId, StorageBox.class);
                        break;
                    case "rack":
                        locationEntity = storageLocationService.get(previousLocationId, StorageRack.class);
                        break;
                    case "shelf":
                        locationEntity = storageLocationService.get(previousLocationId, StorageShelf.class);
                        break;
                    case "device":
                        locationEntity = storageLocationService.get(previousLocationId, StorageDevice.class);
                        break;
                    case "room":
                        locationEntity = storageLocationService.get(previousLocationId, StorageRoom.class);
                        break;
                    }
                    if (locationEntity != null) {
                        previousLocation = buildHierarchicalPathForEntity(locationEntity, previousLocationType,
                                previousPositionCoordinate);
                    }
                }

                // Clear the location fields (preserve assignment for audit trail)
                existingAssignment.setLocationId(null);
                existingAssignment.setLocationType(null);
                existingAssignment.setPositionCoordinate(null);
                sampleStorageAssignmentDAO.update(existingAssignment);
            }

            // Update SampleItem status to "SampleDisposed"
            String disposedStatusId = statusService
                    .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Disposed);
            sampleItem.setStatusId(disposedStatusId);
            sampleItemDAO.update(sampleItem);

            // Create audit movement record for disposal
            // Only create if there was a previous location (constraint requires at least
            // one location)
            Integer movementIdInt = null;
            if (previousLocationId != null && previousLocationType != null) {
                SampleStorageMovement movement = new SampleStorageMovement();
                movement.setSampleItem(sampleItem);
                movement.setPreviousLocationId(previousLocationId);
                movement.setPreviousLocationType(previousLocationType);
                movement.setPreviousPositionCoordinate(previousPositionCoordinate);
                // For disposal, new_location is NULL (no new location)
                movement.setNewLocationId(null);
                movement.setNewLocationType(null);
                movement.setNewPositionCoordinate(null);
                movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
                movement.setReason(
                        "Disposal: " + reason + " | Method: " + method + (notes != null ? " | Notes: " + notes : ""));
                movement.setMovedByUserId(resolveNumericSystemUserId(sysUserId));

                movementIdInt = sampleStorageMovementDAO.insert(movement);
            }
            String movementId = movementIdInt != null ? movementIdInt.toString() : null;

            if (bioSample != null) {
                logLifecycleEvent(sampleItem, CustodyAction.DISPOSED, null, null, previousPositionCoordinate,
                        previousLocation, null, null, notes, sysUserId, "SampleStorageMovement", movementIdInt,
                        workflowStatusBefore, WorkflowStatus.DISPOSED.name());
            }

            // Log successful disposal
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} disposed successfully. Movement ID: {}", sampleItem.getId(), movementId);
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("disposalId", movementId);
            response.put("sampleItemId", sampleItem.getId());
            response.put("status", "disposed");
            response.put("previousLocation", previousLocation);
            response.put("disposedDate", new Timestamp(System.currentTimeMillis()).toString());
            response.put("reason", reason);
            response.put("method", method);
            if (notes != null) {
                response.put("notes", notes);
            }

            return response;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Sample was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

    /**
     * Build hierarchical path for an assignment based on its locationType.
     */
    private String buildHierarchicalPathForAssignment(SampleStorageAssignment assignment) {
        if (assignment == null || assignment.getLocationId() == null || assignment.getLocationType() == null) {
            return null;
        }

        String hierarchicalPath = null;
        StorageRoom room = null;
        StorageDevice device = null;
        StorageShelf shelf = null;
        StorageRack rack = null;

        switch (assignment.getLocationType()) {
        case "room":
            room = (StorageRoom) storageLocationService.get(assignment.getLocationId(), StorageRoom.class);
            if (room != null) {
                hierarchicalPath = room.getName();
                if (assignment.getPositionCoordinate() != null
                        && !assignment.getPositionCoordinate().trim().isEmpty()) {
                    hierarchicalPath += " > " + assignment.getPositionCoordinate();
                }
            }
            break;
        case "device":
            device = (StorageDevice) storageLocationService.get(assignment.getLocationId(), StorageDevice.class);
            if (device != null) {
                room = device.getParentRoom();
                if (room != null) {
                    hierarchicalPath = room.getName() + " > " + device.getName();
                    if (assignment.getPositionCoordinate() != null
                            && !assignment.getPositionCoordinate().trim().isEmpty()) {
                        hierarchicalPath += " > " + assignment.getPositionCoordinate();
                    }
                }
            }
            break;
        case "shelf":
            shelf = (StorageShelf) storageLocationService.get(assignment.getLocationId(), StorageShelf.class);
            if (shelf != null) {
                device = shelf.getParentDevice();
                if (device != null) {
                    room = device.getParentRoom();
                }
                if (room != null && device != null) {
                    hierarchicalPath = room.getName() + " > " + device.getName() + " > " + shelf.getLabel();
                    if (assignment.getPositionCoordinate() != null
                            && !assignment.getPositionCoordinate().trim().isEmpty()) {
                        hierarchicalPath += " > " + assignment.getPositionCoordinate();
                    }
                }
            }
            break;
        case "rack":
            rack = (StorageRack) storageLocationService.get(assignment.getLocationId(), StorageRack.class);
            if (rack != null) {
                shelf = rack.getParentShelf();
                if (shelf != null) {
                    device = shelf.getParentDevice();
                    if (device != null) {
                        room = device.getParentRoom();
                    }
                }
                if (room != null && device != null && shelf != null) {
                    hierarchicalPath = room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > "
                            + rack.getLabel();
                    if (assignment.getPositionCoordinate() != null
                            && !assignment.getPositionCoordinate().trim().isEmpty()) {
                        hierarchicalPath += " > " + assignment.getPositionCoordinate();
                    }
                }
            }
            break;
        case "box":
            StorageBox box = (StorageBox) storageLocationService.get(assignment.getLocationId(), StorageBox.class);
            if (box != null) {
                rack = box.getParentRack();
                if (rack != null) {
                    shelf = rack.getParentShelf();
                    if (shelf != null) {
                        device = shelf.getParentDevice();
                        if (device != null) {
                            room = device.getParentRoom();
                        }
                    }
                }
                StringBuilder pathBuilder = new StringBuilder();
                if (room != null) {
                    pathBuilder.append(room.getName());
                }
                if (device != null) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append(" > ");
                    }
                    pathBuilder.append(device.getName());
                }
                if (shelf != null) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append(" > ");
                    }
                    pathBuilder.append(shelf.getLabel());
                }
                if (rack != null) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append(" > ");
                    }
                    pathBuilder.append(rack.getLabel());
                }
                if (box != null) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append(" > ");
                    }
                    pathBuilder.append(box.getLabel());
                }
                if (assignment.getPositionCoordinate() != null
                        && !assignment.getPositionCoordinate().trim().isEmpty()) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append(" > ");
                    }
                    pathBuilder.append(assignment.getPositionCoordinate());
                }
                hierarchicalPath = pathBuilder.length() > 0 ? pathBuilder.toString() : null;
            }
            break;
        }

        return hierarchicalPath;
    }

    /**
     * Build hierarchical path from already-initialized entities. This method
     * assumes all entities are already loaded (not proxies).
     */
    private String buildPathFromEntities(StorageBox box, StorageRack rack, StorageShelf shelf, StorageDevice device,
            StorageRoom room) {
        if (room != null && device != null && shelf != null) {
            return room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel()
                    + " > " + box.getLabel();
        } else if (device != null && shelf != null) {
            return device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel() + " > " + box.getLabel();
        } else if (shelf != null) {
            return shelf.getLabel() + " > " + rack.getLabel() + " > " + box.getLabel();
        } else {
            return rack.getLabel() + " > " + box.getLabel();
        }
    }

    private Map<String, Object> buildLocationDetailsFromAssignment(SampleStorageAssignment assignment,
            Map<Integer, StorageRoom> roomCache, Map<Integer, StorageDevice> deviceCache,
            Map<Integer, StorageShelf> shelfCache, Map<Integer, StorageRack> rackCache,
            Map<Integer, StorageBox> boxCache) {
        Map<String, Object> result = new HashMap<>();
        if (assignment == null) {
            return result;
        }

        String sampleItemId = assignment.getSampleItemId() != null ? assignment.getSampleItemId().toString() : null;
        result.put("sampleItemId", sampleItemId);
        result.put("locationType", assignment.getLocationType());
        result.put("locationId", assignment.getLocationId());
        result.put("assignedBy", assignment.getAssignedByUserId());
        result.put("assignedDate", assignment.getAssignedDate() != null ? assignment.getAssignedDate().toString() : "");
        result.put("positionCoordinate",
                assignment.getPositionCoordinate() != null ? assignment.getPositionCoordinate() : "");
        result.put("notes", assignment.getNotes() != null ? assignment.getNotes() : "");

        StorageRoom room = null;
        StorageDevice device = null;
        StorageShelf shelf = null;
        StorageRack rack = null;
        StorageBox box = null;

        Integer locationId = assignment.getLocationId();
        String locationType = assignment.getLocationType();

        if (locationId != null && locationType != null) {
            switch (locationType) {
            case "room":
                room = getCachedLocation(locationId, StorageRoom.class, roomCache);
                break;
            case "device":
                device = getCachedLocation(locationId, StorageDevice.class, deviceCache);
                room = device != null ? device.getParentRoom() : null;
                break;
            case "shelf":
                shelf = getCachedLocation(locationId, StorageShelf.class, shelfCache);
                device = shelf != null ? shelf.getParentDevice() : null;
                room = device != null ? device.getParentRoom() : null;
                break;
            case "rack":
                rack = getCachedLocation(locationId, StorageRack.class, rackCache);
                shelf = rack != null ? rack.getParentShelf() : null;
                device = shelf != null ? shelf.getParentDevice() : null;
                room = device != null ? device.getParentRoom() : null;
                break;
            case "box":
                box = getCachedLocation(locationId, StorageBox.class, boxCache);
                rack = box != null ? box.getParentRack() : null;
                shelf = rack != null ? rack.getParentShelf() : null;
                device = shelf != null ? shelf.getParentDevice() : null;
                room = device != null ? device.getParentRoom() : null;
                break;
            default:
                break;
            }
        }

        String hierarchicalPath = buildHierarchicalPathForResolvedLocation(locationType, room, device, shelf, rack, box,
                assignment.getPositionCoordinate());
        result.put("location", hierarchicalPath != null ? hierarchicalPath : "");
        result.put("hierarchicalPath", hierarchicalPath != null ? hierarchicalPath : "");

        if (room != null) {
            result.put("roomId", room.getId());
            result.put("roomName", room.getName());
        }
        if (device != null) {
            result.put("deviceId", device.getId());
            result.put("deviceName", device.getName());
            result.put("deviceType", device.getTypeAsString());
            result.put("deviceBiorepositoryStorage", Boolean.TRUE.equals(device.getBiorepositoryStorage()));
        }
        if (shelf != null) {
            result.put("shelfId", shelf.getId());
            result.put("shelfLabel", shelf.getLabel());
        }
        if (rack != null) {
            result.put("rackId", rack.getId());
            result.put("rackLabel", rack.getLabel());
        }
        if (box != null) {
            result.put("boxId", box.getId());
            result.put("boxLabel", box.getLabel());
        }

        return result;
    }

    private <T> T getCachedLocation(Integer id, Class<T> entityClass, Map<Integer, T> cache) {
        if (id == null) {
            return null;
        }
        if (!cache.containsKey(id)) {
            @SuppressWarnings("unchecked")
            T entity = (T) storageLocationService.get(id, entityClass);
            cache.put(id, entity);
        }
        return cache.get(id);
    }

    private String buildHierarchicalPathForResolvedLocation(String locationType, StorageRoom room, StorageDevice device,
            StorageShelf shelf, StorageRack rack, StorageBox box, String positionCoordinate) {
        List<String> parts = new ArrayList<>();

        if (room != null && room.getName() != null && !room.getName().trim().isEmpty()) {
            parts.add(room.getName().trim());
        }
        if (device != null && device.getName() != null && !device.getName().trim().isEmpty()) {
            parts.add(device.getName().trim());
        }
        if (shelf != null && shelf.getLabel() != null && !shelf.getLabel().trim().isEmpty()) {
            parts.add(shelf.getLabel().trim());
        }
        if (rack != null && rack.getLabel() != null && !rack.getLabel().trim().isEmpty()) {
            parts.add(rack.getLabel().trim());
        }
        if (box != null && box.getLabel() != null && !box.getLabel().trim().isEmpty()) {
            parts.add(box.getLabel().trim());
        }

        String path = String.join(" > ", parts);
        if ((path == null || path.isEmpty()) && locationType != null) {
            return locationType;
        }
        if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                && !"box".equals(locationType)
                && (path == null || !path.endsWith(positionCoordinate.trim()))) {
            return path;
        }
        return path;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public java.util.Map<String, Object> assignSampleItemWithLocation(String sampleItemId, String locationId,
            String locationType, String positionCoordinate, String notes) {
        return assignSampleItemWithLocation(sampleItemId, locationId, locationType, positionCoordinate, notes,
                DEFAULT_SYSTEM_USER_ID);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public java.util.Map<String, Object> assignSampleItemWithLocation(String sampleItemId, String locationId,
            String locationType, String positionCoordinate, String notes, String sysUserId) {
        try {
            // Validate inputs - allow flexible storage without specific location
            if (locationType != null && !locationType.trim().isEmpty()) {
                // Validate locationType is valid enum when provided
                // Allow assignment at any level of the hierarchy: room, device, shelf, rack,
                // box, or general
                if (!locationType.equals("room") && !locationType.equals("device") && !locationType.equals("shelf")
                        && !locationType.equals("rack") && !locationType.equals("box")
                        && !locationType.equals("general")) {
                    throw new LIMSRuntimeException("Invalid location type: " + locationType
                            + ". Must be one of: 'room', 'device', 'shelf', 'rack', 'box', 'general'");
                }

                // Location ID is required if location type is specified and not 'general'
                if (!locationType.equals("general") && (locationId == null || locationId.trim().isEmpty())) {
                    throw new LIMSRuntimeException("Location ID is required for location type: " + locationType);
                }
            } else {
                // Default to 'general' storage when no specific location type is provided
                locationType = "general";
            }

            // Resolve SampleItem: accept either SampleItem ID or accession number
            SampleItem sampleItem = resolveSampleItem(sampleItemId);
            BioSample bioSample = findBioSample(sampleItem);
            String workflowStatusBefore = bioSample != null && bioSample.getWorkflowStatus() != null
                    ? bioSample.getWorkflowStatus().name()
                    : null;

            // Prevent duplicate assignments of the same SampleItem (must move first)
            SampleStorageAssignment existingAssignmentForSample = sampleStorageAssignmentDAO
                    .findBySampleItemId(sampleItem.getId());
            if (existingAssignmentForSample != null) {
                String existingType = existingAssignmentForSample.getLocationType();
                Integer existingLocId = existingAssignmentForSample.getLocationId();
                Object existingLocation = null;

                if (existingType != null && existingLocId != null) {
                    switch (existingType) {
                    case "box":
                        existingLocation = storageLocationService.get(existingLocId, StorageBox.class);
                        break;
                    case "rack":
                        existingLocation = storageLocationService.get(existingLocId, StorageRack.class);
                        break;
                    case "shelf":
                        existingLocation = storageLocationService.get(existingLocId, StorageShelf.class);
                        break;
                    case "device":
                        existingLocation = storageLocationService.get(existingLocId, StorageDevice.class);
                        break;
                    case "room":
                        existingLocation = storageLocationService.get(existingLocId, StorageRoom.class);
                        break;
                    default:
                        break;
                    }
                }

                String existingPath = null;
                if (existingLocation != null && existingType != null) {
                    existingPath = buildHierarchicalPathForEntity(existingLocation, existingType,
                            existingAssignmentForSample.getPositionCoordinate());
                }

                StringBuilder msg = new StringBuilder("Sample is already assigned");
                if (existingPath != null && !existingPath.isEmpty()) {
                    msg.append(" to ").append(existingPath);
                } else if (existingType != null && existingLocId != null) {
                    msg.append(" to ").append(existingType).append(" ").append(existingLocId);
                }
                if (existingAssignmentForSample.getPositionCoordinate() != null
                        && !existingAssignmentForSample.getPositionCoordinate().trim().isEmpty()) {
                    msg.append(" at position ").append(existingAssignmentForSample.getPositionCoordinate().trim());
                }
                msg.append(". Please move the sample before assigning it again.");
                throw new LIMSRuntimeException(msg.toString());
            }

            // Load location entity based on locationType
            Integer locationIdInt = null;
            if (locationId != null && !locationId.trim().isEmpty()) {
                locationIdInt = Integer.parseInt(locationId);
            }

            Object locationEntity = null;
            StorageRoom room = null;
            StorageDevice device = null;
            StorageShelf shelf = null;
            StorageRack rack = null;
            StorageBox box = null;

            switch (locationType) {
            case "room":
                room = (StorageRoom) storageLocationService.get(locationIdInt, StorageRoom.class);
                if (room == null) {
                    throw new LIMSRuntimeException("Room not found: " + locationId);
                }
                locationEntity = room;
                break;
            case "device":
                device = (StorageDevice) storageLocationService.get(locationIdInt, StorageDevice.class);
                if (device == null) {
                    throw new LIMSRuntimeException("Device not found: " + locationId);
                }
                locationEntity = device;
                break;
            case "shelf":
                shelf = (StorageShelf) storageLocationService.get(locationIdInt, StorageShelf.class);
                if (shelf == null) {
                    throw new LIMSRuntimeException("Shelf not found: " + locationId);
                }
                locationEntity = shelf;
                break;
            case "rack":
                rack = (StorageRack) storageLocationService.get(locationIdInt, StorageRack.class);
                if (rack == null) {
                    throw new LIMSRuntimeException("Rack not found: " + locationId);
                }
                locationEntity = rack;
                break;
            case "box":
                box = (StorageBox) storageLocationService.get(locationIdInt, StorageBox.class);
                if (box == null) {
                    throw new LIMSRuntimeException("Box not found: " + locationId);
                }
                locationEntity = box;
                rack = box.getParentRack();
                break;
            case "general":
                // General storage without specific location entity
                // locationEntity remains null, locationIdInt remains null
                break;
            default:
                throw new LIMSRuntimeException("Unsupported location type: " + locationType);
            }

            // Validate location is active (check the entity and any existing parents)
            // Skip validation for general storage
            // Note: Hierarchy levels are now optional - can assign to room, device, shelf,
            // rack, or box
            if (!"general".equals(locationType) && !validateLocationActiveForEntity(locationEntity, locationType)) {
                throw new LIMSRuntimeException("Cannot assign to inactive location");
            }

            // Determine effective coordinate
            String effectiveCoordinate = positionCoordinate;
            if ("box".equals(locationType) && (effectiveCoordinate == null || effectiveCoordinate.trim().isEmpty())
                    && box != null && box.getLabel() != null) {
                effectiveCoordinate = box.getLabel();
            }

            // Validate coordinate is not already occupied (for box assignments with
            // coordinates)
            if ("box".equals(locationType) && effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO
                        .findByBoxAndCoordinate(locationIdInt, effectiveCoordinate.trim());
                if (existingAssignment != null) {
                    throw new LIMSRuntimeException(String.format(
                            "Position %s is already occupied by another sample. Please select a different position.",
                            effectiveCoordinate.trim()));
                }
            }

            // Log assignment details for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Assigning SampleItem {} to: locationId={}, locationType={}, positionCoordinate={}",
                        sampleItemId, locationIdInt, locationType, effectiveCoordinate);
            }

            // Create SampleStorageAssignment - always use locationId + locationType
            SampleStorageAssignment assignment = new SampleStorageAssignment();
            assignment.setSampleItem(sampleItem);
            assignment.setLocationId(locationIdInt);
            assignment.setLocationType(locationType);
            if (effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                assignment.setPositionCoordinate(effectiveCoordinate.trim());
            }
            assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
            assignment.setNotes(notes);
            assignment.setAssignedByUserId(resolveNumericSystemUserId(sysUserId));

            Integer assignmentIdInt = sampleStorageAssignmentDAO.insert(assignment);
            String assignmentId = assignmentIdInt != null ? assignmentIdInt.toString() : null;

            // Log successful assignment creation
            if (logger.isDebugEnabled()) {
                logger.debug("Created assignment for SampleItem {}: assignmentId={}, positionCoordinate={}",
                        sampleItemId, assignmentId, assignment.getPositionCoordinate());
            }

            // Build hierarchical path
            String hierarchicalPath = buildHierarchicalPathForEntity(locationEntity, locationType, effectiveCoordinate);

            // Check shelf capacity if applicable (informational warning only)
            String shelfCapacityWarning = null;
            if (locationType.equals("shelf") && shelf != null) {
                shelfCapacityWarning = checkShelfCapacity(shelf);
            } else if (locationType.equals("rack") && rack != null && rack.getParentShelf() != null) {
                shelfCapacityWarning = checkShelfCapacity(rack.getParentShelf());
            }

            // Create audit log entry with flexible assignment model
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSampleItem(sampleItem);

            // Initial assignment - no previous location
            movement.setPreviousLocationId(null);
            movement.setPreviousLocationType(null);
            movement.setPreviousPositionCoordinate(null);

            // Set new location (target location)
            movement.setNewLocationId(locationIdInt);
            movement.setNewLocationType(locationType);
            if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                movement.setNewPositionCoordinate(positionCoordinate.trim());
            } else {
                movement.setNewPositionCoordinate(null);
            }

            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(notes);
            movement.setMovedByUserId(resolveNumericSystemUserId(sysUserId));

            // Log movement audit record for debugging
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Creating movement audit for SampleItem {}: new locationId={}, locationType={}, positionCoordinate={}",
                        sampleItemId, locationIdInt, locationType, positionCoordinate);
            }

            Integer movementIdInt = sampleStorageMovementDAO.insert(movement);

            if (bioSample != null) {
                WorkflowStatus workflowStatusAfter = workflowStatusBeforeAsStoredTarget(bioSample);
                if (workflowStatusAfter != bioSample.getWorkflowStatus()) {
                    bioSample.setWorkflowStatus(workflowStatusAfter);
                    bioSample.setSysUserId(sysUserId);
                    bioSampleDAO.update(bioSample);
                }

                CustodyAction action = isReturnToStorage(sampleItem, workflowStatusBefore)
                        ? CustodyAction.RETURN_STORED
                        : CustodyAction.STORAGE_ASSIGNED;
                logLifecycleEvent(sampleItem, action, null, null, effectiveCoordinate, null, hierarchicalPath, null,
                        notes, sysUserId, "SampleStorageMovement", movementIdInt, workflowStatusBefore,
                        workflowStatusAfter.name());
            }

            // Log successful assignment
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} assigned successfully. Assignment ID: {}", sampleItemId, assignmentId);
            }

            // Prepare response data
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("assignmentId", assignmentId);
            response.put("hierarchicalPath", hierarchicalPath != null ? hierarchicalPath : "Unknown");
            response.put("assignedDate", new Timestamp(System.currentTimeMillis()).toString());
            if (shelfCapacityWarning != null) {
                response.put("shelfCapacityWarning", shelfCapacityWarning);
            }

            return response;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Location was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public String moveSampleItemWithLocation(String sampleItemId, String locationId, String locationType,
            String positionCoordinate, String reason, String notes) {
        return moveSampleItemWithLocation(sampleItemId, locationId, locationType, positionCoordinate, reason, notes,
                DEFAULT_SYSTEM_USER_ID);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public String moveSampleItemWithLocation(String sampleItemId, String locationId, String locationType,
            String positionCoordinate, String reason, String notes, String sysUserId) {
        try {
            // Validate inputs
            if (locationId == null || locationId.trim().isEmpty()) {
                throw new LIMSRuntimeException("Location ID is required");
            }
            if (locationType == null || locationType.trim().isEmpty()) {
                throw new LIMSRuntimeException("Location type is required");
            }

            // Validate locationType is valid enum
            // Allow assignment at any level of the hierarchy: room, device, shelf, rack,
            // box
            if (!locationType.equals("room") && !locationType.equals("device") && !locationType.equals("shelf")
                    && !locationType.equals("rack") && !locationType.equals("box")) {
                throw new LIMSRuntimeException("Invalid location type: " + locationType
                        + ". Must be one of: 'room', 'device', 'shelf', 'rack', 'box'");
            }

            // Resolve SampleItem: accept either accession number or external ID
            SampleItem sampleItem = resolveSampleItem(sampleItemId);
            BioSample bioSample = findBioSample(sampleItem);
            String workflowStatusBefore = bioSample != null && bioSample.getWorkflowStatus() != null
                    ? bioSample.getWorkflowStatus().name()
                    : null;
            // Get the actual numeric ID from the resolved SampleItem for database lookups
            String resolvedSampleItemId = sampleItem.getId();

            // Load target location entity based on locationType
            Integer locationIdInt = Integer.parseInt(locationId);
            Object targetLocationEntity = null;
            StorageRoom targetRoom = null;
            StorageDevice targetDevice = null;
            StorageShelf targetShelf = null;
            StorageRack targetRack = null;
            StorageBox targetBox = null;

            switch (locationType) {
            case "room":
                targetRoom = (StorageRoom) storageLocationService.get(locationIdInt, StorageRoom.class);
                if (targetRoom == null) {
                    throw new LIMSRuntimeException("Target room not found: " + locationId);
                }
                targetLocationEntity = targetRoom;
                break;
            case "device":
                targetDevice = (StorageDevice) storageLocationService.get(locationIdInt, StorageDevice.class);
                if (targetDevice == null) {
                    throw new LIMSRuntimeException("Target device not found: " + locationId);
                }
                targetLocationEntity = targetDevice;
                break;
            case "shelf":
                targetShelf = (StorageShelf) storageLocationService.get(locationIdInt, StorageShelf.class);
                if (targetShelf == null) {
                    throw new LIMSRuntimeException("Target shelf not found: " + locationId);
                }
                targetLocationEntity = targetShelf;
                break;
            case "rack":
                targetRack = (StorageRack) storageLocationService.get(locationIdInt, StorageRack.class);
                if (targetRack == null) {
                    throw new LIMSRuntimeException("Target rack not found: " + locationId);
                }
                targetLocationEntity = targetRack;
                break;
            case "box":
                targetBox = (StorageBox) storageLocationService.get(locationIdInt, StorageBox.class);
                if (targetBox == null) {
                    throw new LIMSRuntimeException("Target box not found: " + locationId);
                }
                targetLocationEntity = targetBox;
                targetRack = targetBox.getParentRack();
                break;
            }

            // Validate target location is active (no minimum level requirement)
            if (!validateLocationActiveForEntity(targetLocationEntity, locationType)) {
                throw new LIMSRuntimeException("Cannot move to inactive location");
            }
            // No occupancy tracking - position is just a text field

            // Find existing assignment for SampleItem (using resolved numeric ID)
            SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO
                    .findBySampleItemId(resolvedSampleItemId);

            // Store previous location details BEFORE updating (for movement audit log)
            Integer previousLocationId = null;
            String previousLocationType = null;
            String previousPositionCoordinate = null;

            if (existingAssignment != null) {
                // Store previous values before updating
                previousLocationId = existingAssignment.getLocationId();
                previousLocationType = existingAssignment.getLocationType();
                previousPositionCoordinate = existingAssignment.getPositionCoordinate();

                // Log previous state for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("Moving SampleItem {} from: locationId={}, locationType={}, positionCoordinate={}",
                            sampleItemId, previousLocationId, previousLocationType, previousPositionCoordinate);
                }

                // Update existing assignment - always use locationId + locationType
                existingAssignment.setLocationId(locationIdInt);
                existingAssignment.setLocationType(locationType);
                String effectiveCoordinate = positionCoordinate;
                if ("box".equals(locationType) && (effectiveCoordinate == null || effectiveCoordinate.trim().isEmpty())
                        && targetBox != null && targetBox.getLabel() != null) {
                    effectiveCoordinate = targetBox.getLabel();
                }
                if (effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                    existingAssignment.setPositionCoordinate(effectiveCoordinate.trim());
                } else {
                    existingAssignment.setPositionCoordinate(null);
                }
                existingAssignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                if (reason != null) {
                    existingAssignment.setNotes(reason);
                }
                sampleStorageAssignmentDAO.update(existingAssignment);

                // Log new state for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Updated assignment for SampleItem {}: locationId={}, locationType={}, positionCoordinate={}",
                            sampleItemId, locationIdInt, locationType, existingAssignment.getPositionCoordinate());
                }
            } else {
                // Create new assignment (SampleItem was not previously assigned) - always use
                // locationId + locationType
                String effectiveCoordinate = positionCoordinate;
                if ("box".equals(locationType) && (effectiveCoordinate == null || effectiveCoordinate.trim().isEmpty())
                        && targetBox != null && targetBox.getLabel() != null) {
                    effectiveCoordinate = targetBox.getLabel();
                }
                SampleStorageAssignment assignment = new SampleStorageAssignment();
                assignment.setSampleItem(sampleItem);
                assignment.setLocationId(locationIdInt);
                assignment.setLocationType(locationType);
                if (effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                    assignment.setPositionCoordinate(effectiveCoordinate.trim());
                }
                assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                if (reason != null) {
                    assignment.setNotes(reason);
                }
                assignment.setAssignedByUserId(resolveNumericSystemUserId(sysUserId));
                sampleStorageAssignmentDAO.insert(assignment);

                // Log initial assignment for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Initial assignment for SampleItem {}: locationId={}, locationType={}, positionCoordinate={}",
                            sampleItemId, locationIdInt, locationType, assignment.getPositionCoordinate());
                }
            }

            // Create audit log entry with flexible assignment model
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSampleItem(sampleItem);

            // Set previous location (from stored values, not from updated assignment)
            if (previousLocationId != null && previousLocationType != null) {
                movement.setPreviousLocationId(previousLocationId);
                movement.setPreviousLocationType(previousLocationType);
                movement.setPreviousPositionCoordinate(previousPositionCoordinate);

                // Log movement audit record for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("Movement audit - previous: locationId={}, locationType={}, positionCoordinate={}",
                            previousLocationId, previousLocationType, previousPositionCoordinate);
                }
            } else {
                // Initial assignment - no previous location
                movement.setPreviousLocationId(null);
                movement.setPreviousLocationType(null);
                movement.setPreviousPositionCoordinate(null);

                // Log initial assignment audit record for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("Movement audit - initial assignment (no previous location)");
                }
            }

            // Set new location (target location)
            movement.setNewLocationId(locationIdInt);
            movement.setNewLocationType(locationType);
            String newPositionCoordinateValue = null;
            if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                newPositionCoordinateValue = positionCoordinate.trim();
                movement.setNewPositionCoordinate(newPositionCoordinateValue);
            } else {
                movement.setNewPositionCoordinate(null);
            }

            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(reason);
            movement.setMovedByUserId(resolveNumericSystemUserId(sysUserId));

            // Log new location for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Movement audit - new: locationId={}, locationType={}, positionCoordinate={}",
                        locationIdInt, locationType, newPositionCoordinateValue);
            }

            Integer movementIdInt = sampleStorageMovementDAO.insert(movement);
            String movementId = movementIdInt != null ? movementIdInt.toString() : null;

            if (bioSample != null) {
                String previousPath = buildPathFromLocation(previousLocationId, previousLocationType,
                        previousPositionCoordinate);
                String newPath = buildHierarchicalPathForEntity(targetLocationEntity, locationType,
                        newPositionCoordinateValue);
                CustodyAction action = isPendingStorageWorkflow(bioSample.getWorkflowStatus())
                        ? CustodyAction.RETURN_STORED
                        : CustodyAction.STORAGE_MOVED;
                String workflowStatusAfter = workflowStatusBefore;

                if (isPendingStorageWorkflow(bioSample.getWorkflowStatus())) {
                    bioSample.setWorkflowStatus(WorkflowStatus.STORED);
                    bioSample.setSysUserId(sysUserId);
                    bioSampleDAO.update(bioSample);
                    workflowStatusAfter = WorkflowStatus.STORED.name();
                }

                logLifecycleEvent(sampleItem, action, null, null, newPositionCoordinateValue, previousPath, newPath,
                        null, reason != null ? reason : notes, sysUserId, "SampleStorageMovement", movementIdInt,
                        workflowStatusBefore, workflowStatusAfter);
            }

            // Log successful movement creation
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} moved successfully. Movement ID: {}", sampleItemId, movementId);
            }

            return movementId;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Location was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

    private BioSample findBioSample(SampleItem sampleItem) {
        if (sampleItem == null || sampleItem.getId() == null) {
            return null;
        }
        try {
            return bioSampleDAO.getBySampleItemId(Integer.valueOf(sampleItem.getId()));
        } catch (NumberFormatException e) {
            logger.debug("Unable to resolve BioSample for non-numeric SampleItem ID {}", sampleItem.getId());
            return null;
        }
    }

    private int resolveNumericSystemUserId(String sysUserId) {
        if (sysUserId != null) {
            try {
                return Integer.parseInt(sysUserId);
            } catch (NumberFormatException e) {
                logger.debug("Falling back to default numeric system user ID for {}", sysUserId);
            }
        }
        return Integer.parseInt(DEFAULT_SYSTEM_USER_ID);
    }

    private WorkflowStatus workflowStatusBeforeAsStoredTarget(BioSample bioSample) {
        if (bioSample == null) {
            return WorkflowStatus.STORED;
        }
        if (bioSample.getWorkflowStatus() == null || isPendingStorageWorkflow(bioSample.getWorkflowStatus())
                || bioSample.getWorkflowStatus() == WorkflowStatus.REGISTERED) {
            return WorkflowStatus.STORED;
        }
        return bioSample.getWorkflowStatus();
    }

    private boolean isPendingStorageWorkflow(WorkflowStatus workflowStatus) {
        return workflowStatus == WorkflowStatus.PENDING_STORAGE || workflowStatus == WorkflowStatus.REGISTERED;
    }

    private boolean isReturnToStorage(SampleItem sampleItem, String workflowStatusBefore) {
        if (sampleItem == null || sampleItem.getId() == null
                || !WorkflowStatus.PENDING_STORAGE.name().equals(workflowStatusBefore)) {
            return false;
        }

        try {
            List<org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog> logs = chainOfCustodyService
                    .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
            if (logs == null || logs.isEmpty()) {
                return false;
            }

            for (int i = logs.size() - 1; i >= 0; i--) {
                CustodyAction action = logs.get(i).getCustodyAction();
                if (action == CustodyAction.RETURN_RECEIVED || action == CustodyAction.RETURN_INSPECTED) {
                    return true;
                }
                if (action == CustodyAction.TRANSFER_RECEIVED || action == CustodyAction.STORAGE_ASSIGNED
                        || action == CustodyAction.STORAGE_MOVED || action == CustodyAction.RETURN_STORED) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            logger.debug("Unable to determine return-to-storage state for non-numeric SampleItem ID {}",
                    sampleItem.getId());
        }

        return false;
    }

    private void logLifecycleEvent(SampleItem sampleItem, CustodyAction action, String storageCoordinates,
            String fromLocation, String toLocation, java.math.BigDecimal temperature, String notes, String sysUserId,
            String sourceRecordType, Integer sourceRecordId, String workflowStatusBefore, String workflowStatusAfter) {
        logLifecycleEvent(sampleItem, action, null, null, storageCoordinates, fromLocation, toLocation, temperature,
                notes, sysUserId, sourceRecordType, sourceRecordId, workflowStatusBefore, workflowStatusAfter);
    }

    private void logLifecycleEvent(SampleItem sampleItem, CustodyAction action,
            org.openelisglobal.biorepository.valueholder.SampleTransferRequest transferRequest,
            org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest retrievalRequest,
            String storageCoordinates, String fromLocation, String toLocation, java.math.BigDecimal temperature,
            String notes, String sysUserId, String sourceRecordType, Integer sourceRecordId,
            String workflowStatusBefore, String workflowStatusAfter) {
        SystemUser actor = systemUserService.get(sysUserId);
        chainOfCustodyService.logCustodyAction(sampleItem, action, transferRequest, retrievalRequest, storageCoordinates,
                actor, fromLocation, toLocation, temperature, notes, sysUserId, sourceRecordType, sourceRecordId,
                workflowStatusBefore, workflowStatusAfter);
    }

    private String buildPathFromLocation(Integer locationId, String locationType, String positionCoordinate) {
        if (locationId == null || locationType == null || locationType.trim().isEmpty()) {
            return null;
        }

        Object locationEntity = switch (locationType) {
        case "box" -> storageLocationService.get(locationId, StorageBox.class);
        case "rack" -> storageLocationService.get(locationId, StorageRack.class);
        case "shelf" -> storageLocationService.get(locationId, StorageShelf.class);
        case "device" -> storageLocationService.get(locationId, StorageDevice.class);
        case "room" -> storageLocationService.get(locationId, StorageRoom.class);
        default -> null;
        };

        return locationEntity != null
                ? buildHierarchicalPathForEntity(locationEntity, locationType, positionCoordinate)
                : null;
    }

    /**
     * Validate that a location entity is active (check entire hierarchy). All
     * hierarchy levels are optional - can assign to room, device, shelf, rack, or
     * box. Validates that the specified entity and all existing parent entities are
     * active.
     */
    private boolean validateLocationActiveForEntity(Object locationEntity, String locationType) {
        if (locationEntity == null) {
            return false;
        }

        StorageRoom room = null;
        StorageDevice device = null;
        StorageShelf shelf = null;
        StorageRack rack = null;

        switch (locationType) {
        case "room":
            room = (StorageRoom) locationEntity;
            // Room is the only entity to validate
            break;
        case "device":
            device = (StorageDevice) locationEntity;
            room = device.getParentRoom();
            break;
        case "shelf":
            shelf = (StorageShelf) locationEntity;
            device = shelf.getParentDevice();
            if (device != null) {
                room = device.getParentRoom();
            }
            break;
        case "rack":
            rack = (StorageRack) locationEntity;
            shelf = rack.getParentShelf();
            if (shelf != null) {
                device = shelf.getParentDevice();
                if (device != null) {
                    room = device.getParentRoom();
                }
            }
            break;
        case "box":
            StorageBox box = (StorageBox) locationEntity;
            rack = box.getParentRack();
            if (rack != null) {
                shelf = rack.getParentShelf();
                if (shelf != null) {
                    device = shelf.getParentDevice();
                    if (device != null) {
                        room = device.getParentRoom();
                    }
                }
            }
            break;
        default:
            break;
        }

        // Validate the specified entity and all existing parents are active
        // All hierarchy levels are optional - no minimum level requirement
        if (room != null && (room.getActive() == null || !room.getActive())) {
            return false;
        }
        if (device != null && (device.getActive() == null || !device.getActive())) {
            return false;
        }
        if (shelf != null && (shelf.getActive() == null || !shelf.getActive())) {
            return false;
        }
        if (rack != null && (rack.getActive() == null || !rack.getActive())) {
            return false;
        }

        return true;
    }

    /**
     * Build hierarchical path for a location entity (room, device, shelf, rack, or
     * box)
     */
    private String buildHierarchicalPathForEntity(Object locationEntity, String locationType,
            String positionCoordinate) {
        if (locationEntity == null) {
            return "Unknown Location";
        }

        StorageRoom room = null;
        StorageDevice device = null;
        StorageShelf shelf = null;
        StorageRack rack = null;

        switch (locationType) {
        case "room":
            room = (StorageRoom) locationEntity;
            return room.getName()
                    + (positionCoordinate != null && !positionCoordinate.trim().isEmpty() ? " > " + positionCoordinate
                            : "");
        case "device":
            device = (StorageDevice) locationEntity;
            room = device.getParentRoom();
            if (room != null && device != null) {
                return room.getName() + " > " + device.getName()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            } else if (device != null) {
                return device.getName() + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                        ? " > " + positionCoordinate
                        : "");
            }
            break;
        case "shelf":
            shelf = (StorageShelf) locationEntity;
            device = shelf.getParentDevice();
            if (device != null) {
                room = device.getParentRoom();
            }
            if (room != null && device != null && shelf != null) {
                return room.getName() + " > " + device.getName() + " > " + shelf.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            } else if (device != null && shelf != null) {
                return device.getName() + " > " + shelf.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            }
            break;
        case "rack":
            rack = (StorageRack) locationEntity;
            shelf = rack.getParentShelf();
            if (shelf != null) {
                device = shelf.getParentDevice();
                if (device != null) {
                    room = device.getParentRoom();
                }
            }
            if (room != null && device != null && shelf != null && rack != null) {
                return room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            } else if (device != null && shelf != null && rack != null) {
                return device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            }
            break;
        case "box":
            StorageBox box = (StorageBox) locationEntity;
            rack = box.getParentRack();
            if (rack != null) {
                shelf = rack.getParentShelf();
                if (shelf != null) {
                    device = shelf.getParentDevice();
                    if (device != null) {
                        room = device.getParentRoom();
                    }
                }
            }
            String coord = positionCoordinate != null && !positionCoordinate.trim().isEmpty() ? positionCoordinate
                    : box.getLabel();
            StringBuilder builder = new StringBuilder();
            if (room != null) {
                builder.append(room.getName());
            }
            if (device != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(device.getName());
            }
            if (shelf != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(shelf.getLabel());
            }
            if (rack != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(rack.getLabel());
            }
            if (coord != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(coord);
            }
            return builder.length() > 0 ? builder.toString() : "Unknown Location";
        default:
            break;
        }

        return "Unknown Location";
    }

    /**
     * Check shelf capacity and return warning message if applicable (informational
     * only)
     */
    private String checkShelfCapacity(StorageShelf shelf) {
        if (shelf == null || shelf.getCapacityLimit() == null || shelf.getCapacityLimit() <= 0) {
            return null;
        }

        int occupied = storageLocationService.countOccupiedInShelf(shelf.getId());
        int capacityLimit = shelf.getCapacityLimit();
        int percentage = (occupied * 100) / capacityLimit;

        if (percentage >= 100) {
            return String.format(
                    "Shelf %s is at or over capacity (%d/%d positions, %d%%). Assignment allowed but shelf is over-occupied.",
                    shelf.getLabel(), occupied, capacityLimit, percentage);
        } else if (percentage >= 90) {
            return String.format("Shelf %s is near capacity (%d/%d positions, %d%%).", shelf.getLabel(), occupied,
                    capacityLimit, percentage);
        }

        return null;
    }

    /**
     * Resolve SampleItem from identifier (internal ID, accession number, or
     * external reference)
     * 
     * @param identifier Internal SampleItem ID, accession number, or external
     *                   reference
     * @return SampleItem entity
     * @throws LIMSRuntimeException if SampleItem not found or multiple SampleItems
     *                              match
     */
    private SampleItem resolveSampleItem(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new LIMSRuntimeException("Sample identifier is required");
        }

        String trimmedId = identifier.trim();

        // Step 0: Try numeric ID lookup (direct SampleItem by internal ID)
        // This handles cases where frontend sends the database ID
        // IMPORTANT: Only attempt this if the identifier is purely numeric, because
        // sampleItemService.get() is @Transactional and throws ObjectNotFoundException
        // when not found. If an exception is thrown inside a nested @Transactional
        // method,
        // it marks the outer transaction for rollback even if the exception is caught.
        if (trimmedId.matches("\\d+")) {
            try {
                SampleItem sampleItemById = sampleItemService.get(trimmedId);
                if (sampleItemById != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found SampleItem by numeric ID: {}", trimmedId);
                    }
                    return sampleItemById;
                }
            } catch (Exception e) {
                // Not found by numeric ID, continue to other lookup methods
                if (logger.isDebugEnabled()) {
                    logger.debug("SampleItem not found by numeric ID '{}', trying other methods", trimmedId);
                }
            }
        }

        // Step 1: Try accession number lookup (Sample → SampleItems)
        Sample sample = sampleService.getSampleByAccessionNumber(trimmedId);
        if (sample != null) {
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            if (sampleItems != null && !sampleItems.isEmpty()) {
                if (sampleItems.size() == 1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found SampleItem by accession number: {}", trimmedId);
                    }
                    return sampleItems.get(0);
                } else {
                    throw new LIMSRuntimeException(String.format(
                            "Sample with accession number '%s' has %d SampleItems. Please provide the external reference number to identify the specific specimen.",
                            trimmedId, sampleItems.size()));
                }
            }
        }

        // Step 2: Try external reference lookup (direct SampleItem lookup)
        List<SampleItem> sampleItemsByExtId = sampleItemService.getSampleItemsByExternalID(trimmedId);
        if (sampleItemsByExtId != null && !sampleItemsByExtId.isEmpty()) {
            if (sampleItemsByExtId.size() == 1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found SampleItem by external reference: {}", trimmedId);
                }
                return sampleItemsByExtId.get(0);
            } else {
                throw new LIMSRuntimeException(String.format(
                        "Multiple SampleItems found with external reference '%s'. This should not happen - external references should be unique.",
                        trimmedId));
            }
        }

        // Step 3: Try direct SampleItem ID lookup (for internal system calls)
        try {
            SampleItem sampleItemById = sampleItemService.get(trimmedId);
            if (sampleItemById != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found SampleItem by internal ID: {}", trimmedId);
                }
                return sampleItemById;
            }
        } catch (Exception e) {
            // ID lookup failed - continue to error
            logger.debug("Failed to find SampleItem by internal ID '{}': {}", trimmedId, e.getMessage());
        }

        // Not found by any method
        throw new LIMSRuntimeException(String.format(
                "Sample not found with identifier '%s'. Please check the accession number or external reference number.",
                trimmedId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SampleStorageAssignment> getSampleAssignments(Pageable pageable) {
        return sampleStorageAssignmentDAO.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment getSampleStorageAssignment(Integer assignmentId) {
        if (assignmentId == null) {
            return null;
        }
        return sampleStorageAssignmentDAO.get(assignmentId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleStorageAssignment> getSampleStorageAssignmentsBySampleItem(SampleItem sampleItem) {
        if (sampleItem == null || sampleItem.getId() == null) {
            return java.util.Collections.emptyList();
        }
        SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findBySampleItemId(sampleItem.getId());
        if (assignment != null) {
            return java.util.Collections.singletonList(assignment);
        }
        return java.util.Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleStorageMovement> getSampleStorageMovementsBySampleItem(SampleItem sampleItem) {
        if (sampleItem == null || sampleItem.getId() == null) {
            return java.util.Collections.emptyList();
        }
        return sampleStorageMovementDAO.findBySampleItemId(sampleItem.getId());
    }
}
