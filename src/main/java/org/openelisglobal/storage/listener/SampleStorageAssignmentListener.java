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

            try {
                String sampleItemId = sampleItem.getId();

                logger.info("Assigning storage location for SampleItem {}: locationId={}, locationType={}",
                        sampleItemId, storageLocationId, storageLocationType);

                sampleStorageService.assignSampleItemWithLocation(sampleItemId, storageLocationId, storageLocationType,
                        storagePositionCoordinate, "Auto-assigned on order creation");

                logger.info("Successfully assigned storage location for SampleItem {}", sampleItemId);

            } catch (Exception e) {
                // Log error but don't fail the entire order creation
                logger.error("Failed to assign storage location for SampleItem {}: {}", sampleItem.getId(),
                        e.getMessage(), e);
            }
        }
    }
}
