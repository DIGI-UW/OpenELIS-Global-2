package org.openelisglobal.storage.listener;

import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener that creates storage assignments for sample items when an
 * order is created. Listens for SamplePatientUpdateDataCreatedEvent and
 * processes storage location data that was parsed from the sample XML.
 */
@Component
public class SampleStorageAssignmentListener {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageAssignmentListener.class);

    @Autowired
    private SampleStorageService sampleStorageService;

    @EventListener
    @Transactional
    public void handleSampleCreated(SamplePatientUpdateDataCreatedEvent event) {
        SamplePatientUpdateData updateData = event.getUpdateData();

        if (updateData == null || updateData.getSampleItemsTests() == null) {
            return;
        }

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            // Check if storage location was specified for this sample item
            String storageLocationId = sampleTestCollection.storageLocationId;
            String storageLocationType = sampleTestCollection.storageLocationType;
            String storagePositionCoordinate = sampleTestCollection.storagePositionCoordinate;

            // Skip if no storage location specified
            if (storageLocationId == null || storageLocationId.trim().isEmpty() || storageLocationType == null
                    || storageLocationType.trim().isEmpty()) {
                continue;
            }

            SampleItem sampleItem = sampleTestCollection.item;
            if (sampleItem == null || sampleItem.getId() == null) {
                logger.warn("Cannot assign storage location - SampleItem not persisted yet");
                continue;
            }

            applyStorage(sampleItem.getId(), storageLocationId, storageLocationType, storagePositionCoordinate,
                    sampleTestCollection.storageNotes);
        }
    }

    /**
     * Stores the sample item where the save says, as one of three cases: a first
     * assignment, a move to a different place, or the same place with only the
     * notes changed. An item never stored reads back without a location id (its
     * quantity snapshot is still reported), so the id decides, not the map.
     */
    void applyStorage(String sampleItemId, String storageLocationId, String storageLocationType,
            String storagePositionCoordinate, String storageNotes) {
        java.util.Map<String, Object> existing = sampleStorageService.getSampleItemLocation(sampleItemId);
        boolean alreadyAssigned = existing != null && existing.get("locationId") != null
                && !String.valueOf(existing.get("locationId")).isBlank();

        logger.info("Storage assignment [v2]: sampleItemId={}, alreadyAssigned={}, locationId={}, locationType={}",
                sampleItemId, alreadyAssigned, storageLocationId, storageLocationType);

        String notes = storageNotes == null ? "" : storageNotes.trim();
        if (alreadyAssigned
                && sameLocation(existing, storageLocationId, storageLocationType, storagePositionCoordinate)) {
            if (!notes.equals(existing.get("notes"))) {
                sampleStorageService.updateAssignmentMetadata(sampleItemId, storagePositionCoordinate, notes);
            }
        } else if (alreadyAssigned) {
            sampleStorageService.moveSampleItemWithLocation(sampleItemId, storageLocationId, storageLocationType,
                    storagePositionCoordinate, "Reassignment on order save", notes);
        } else {
            sampleStorageService.assignSampleItemWithLocation(sampleItemId, storageLocationId, storageLocationType,
                    storagePositionCoordinate, notes.isEmpty() ? "Auto-assigned on order creation" : notes);
        }

        logger.info("Successfully processed storage location for SampleItem {}", sampleItemId);
    }

    /**
     * A save that repeats the sample's current location is not a move: only its
     * notes may have changed. The location read back carries the location id and
     * type when the storage service reports them; without them the position alone
     * decides.
     */
    private static boolean sameLocation(java.util.Map<String, Object> existing, String locationId, String locationType,
            String positionCoordinate) {
        Object existingId = existing.get("locationId");
        Object existingType = existing.get("locationType");
        if (existingId != null && existingType != null) {
            return locationId.equals(String.valueOf(existingId)) && locationType.equals(String.valueOf(existingType))
                    && samePosition(existing, positionCoordinate);
        }
        return false;
    }

    private static boolean samePosition(java.util.Map<String, Object> existing, String positionCoordinate) {
        String current = existing.get("positionCoordinate") == null ? ""
                : String.valueOf(existing.get("positionCoordinate"));
        return current.equals(positionCoordinate == null ? "" : positionCoordinate);
    }
}
