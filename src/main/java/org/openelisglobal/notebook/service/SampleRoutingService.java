package org.openelisglobal.notebook.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.openelisglobal.notebook.valueholder.StorageCondition;

/**
 * Service interface for SampleRouting operations.
 */
public interface SampleRoutingService extends BaseObjectService<SampleRouting, Integer> {

    /**
     * Get all routing records for a notebook.
     */
    List<SampleRouting> getByNotebookId(Integer notebookId);

    /**
     * Get routing record for a sample in a specific notebook.
     */
    SampleRouting getByNotebookIdAndSampleItemId(Integer notebookId, Integer sampleItemId);

    /**
     * Get routing records by destination type.
     */
    List<SampleRouting> getByNotebookIdAndDestinationType(Integer notebookId, DestinationType destinationType);

    /**
     * Find routing by box and well coordinate (for analyzer import matching).
     */
    SampleRouting getByBoxAndWell(Integer notebookId, Integer boxId, String wellCoordinate);

    /**
     * Route a sample to internal analysis with box/well assignment.
     */
    SampleRouting routeToInternalAnalysis(Integer notebookId, Integer sampleItemId, Integer boxId,
            String wellCoordinate, String userId);

    /**
     * Route a sample to internal analysis using an assay plate (temporary plate,
     * NOT connected to storage hierarchy). The assay plate info is stored in the
     * routing record's data field but no StorageBox entity is required.
     *
     * @param notebookId     the notebook ID
     * @param sampleItemId   the sample item ID
     * @param assayPlateName name/identifier of the assay plate
     * @param wellCoordinate well position on the plate
     * @param userId         user ID performing the routing
     * @return the created SampleRouting record
     */
    SampleRouting routeToAssayPlate(Integer notebookId, Integer sampleItemId, String assayPlateName,
            String wellCoordinate, String userId);

    /**
     * Bulk route samples to internal analysis using assay plates.
     *
     * @param notebookId      the notebook ID
     * @param sampleItemIds   list of sample item IDs
     * @param assayPlateName  name of the assay plate
     * @param wellAssignments map of sample ID to well coordinate
     * @param userId          user ID performing the routing
     * @return count of successfully routed samples
     */
    int bulkRouteToAssayPlate(Integer notebookId, List<Integer> sampleItemIds, String assayPlateName,
            Map<Integer, String> wellAssignments, String userId);

    /**
     * Route a sample to external lab.
     */
    SampleRouting routeToExternalLab(Integer notebookId, Integer sampleItemId, String externalLabName,
            java.time.LocalDate shipmentDate, String userId);

    /**
     * Route a sample to storage. If storageAssignmentId is null, creates a routing
     * record without storage assignment (pending assignment).
     */
    SampleRouting routeToStorage(Integer notebookId, Integer sampleItemId, Integer storageAssignmentId, String userId);

    /**
     * Route a sample to storage with specific box and well assignment.
     */
    SampleRouting routeToStorageWithBox(Integer notebookId, Integer sampleItemId, Integer boxId, String wellCoordinate,
            String userId);

    /**
     * Bulk route samples to internal analysis.
     */
    int bulkRouteToInternalAnalysis(Integer notebookId, List<Integer> sampleItemIds, Integer boxId,
            Map<Integer, String> wellAssignments, String userId);

    /**
     * Bulk route samples to storage with box/well assignments.
     */
    int bulkRouteToStorage(Integer notebookId, List<Integer> sampleItemIds, Integer boxId,
            Map<Integer, String> wellAssignments, String userId);

    /**
     * Route a sample to storage with location, storage condition, and retention
     * period. Uses SampleStorageService for actual assignment and creates audit
     * trail.
     *
     * @param notebookId         the notebook ID
     * @param sampleItemId       the sample item ID
     * @param locationId         storage location ID (device, shelf, rack, or box)
     * @param locationType       type of location: 'device', 'shelf', 'rack', 'box'
     * @param positionCoordinate optional position coordinate within the location
     * @param condition          storage condition (temperature requirement)
     * @param retentionYears     retention period in years
     * @param userId             user ID performing the routing
     * @return the created SampleRouting record with storageAssignment set
     */
    SampleRouting routeToStorage(Integer notebookId, Integer sampleItemId, String locationId, String locationType,
            String positionCoordinate, StorageCondition condition, int retentionYears, String userId);

    /**
     * Bulk route samples to storage with conditions and retention.
     *
     * @param notebookId     the notebook ID
     * @param sampleItemIds  list of sample item IDs to route
     * @param locationId     storage location ID
     * @param locationType   type of location
     * @param condition      storage condition for all samples
     * @param retentionYears retention period for all samples
     * @param userId         user ID performing the routing
     * @return count of successfully routed samples
     */
    int bulkRouteToStorage(Integer notebookId, List<Integer> sampleItemIds, String locationId, String locationType,
            StorageCondition condition, int retentionYears, String userId);

    /**
     * Calculate expiry date based on retention period.
     *
     * @param retentionYears number of years to retain
     * @return expiry date (today + retentionYears)
     */
    LocalDate calculateExpiryDate(int retentionYears);

    /**
     * Get routing summary counts by destination type.
     */
    RoutingSummary getRoutingSummary(Integer notebookId);

    /**
     * Check if a well coordinate is available in a box for a notebook.
     */
    boolean isWellAvailable(Integer notebookId, Integer boxId, String wellCoordinate);

    /**
     * Summary class for routing statistics.
     */
    class RoutingSummary {
        private final long internalAnalysis;
        private final long externalLab;
        private final long storage;
        private final long unrouted;

        public RoutingSummary(long internalAnalysis, long externalLab, long storage, long unrouted) {
            this.internalAnalysis = internalAnalysis;
            this.externalLab = externalLab;
            this.storage = storage;
            this.unrouted = unrouted;
        }

        public long internalAnalysis() {
            return internalAnalysis;
        }

        public long externalLab() {
            return externalLab;
        }

        public long storage() {
            return storage;
        }

        public long unrouted() {
            return unrouted;
        }

        public long total() {
            return internalAnalysis + externalLab + storage;
        }
    }

    /**
     * Generate well coordinate in row-major order (A1, A2, ..., A12, B1, ...).
     *
     * @param index         the zero-based index of the sample
     * @param columnsPerRow number of columns per row (e.g., 12 for 96-well plate)
     * @return well coordinate string (e.g., "A1", "H12")
     */
    String generateWellCoordinate(int index, int columnsPerRow);

    /**
     * Auto-assign wells to samples in a box using row-major order. Skips
     * already-occupied wells.
     *
     * @param notebookId    the notebook ID
     * @param sampleItemIds list of sample item IDs to assign
     * @param boxId         the storage box ID
     * @param columnsPerRow columns per row (default 12 for 96-well)
     * @return map of sample ID to assigned well coordinate
     */
    Map<Integer, String> autoAssignWells(Integer notebookId, List<Integer> sampleItemIds, Integer boxId,
            int columnsPerRow);

    /**
     * Get all routing records for samples in a specific box.
     *
     * @param notebookId the notebook ID
     * @param boxId      the box ID
     * @return list of routing records for the box
     */
    List<SampleRouting> getByBoxId(Integer notebookId, Integer boxId);

    /**
     * Get box layout showing occupied wells.
     *
     * @param notebookId the notebook ID
     * @param boxId      the box ID
     * @return map of well coordinate to sample routing info
     */
    Map<String, SampleRouting> getBoxLayout(Integer notebookId, Integer boxId);
}
