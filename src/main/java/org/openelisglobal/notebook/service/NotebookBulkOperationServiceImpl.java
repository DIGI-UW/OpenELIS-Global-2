package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of bulk operations for notebook page samples. Processes
 * operations in batches of 50 to prevent timeout.
 *
 * <p>
 * Per FR-033: System MUST process bulk operations in batches of 50.
 */
@Service
public class NotebookBulkOperationServiceImpl implements NotebookBulkOperationService {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private InventoryManagementService inventoryManagementService;

    @Autowired
    private StorageBoxDAO storageBoxDAO;

    @Override
    @Transactional
    public int bulkApplyValues(Integer pageId, List<Integer> sampleIds, Map<String, Object> data, String userId) {
        LogEvent.logInfo(this.getClass().getName(), "bulkApplyValues",
                "Called with pageId=" + pageId + ", sampleIds=" + sampleIds + ", data keys=" + data.keySet());

        if (sampleIds == null || sampleIds.isEmpty() || data == null || data.isEmpty()) {
            LogEvent.logInfo(this.getClass().getName(), "bulkApplyValues", "Empty sampleIds or data, returning 0");
            return 0;
        }

        int updatedCount = 0;

        // Count samples that DON'T already have reagents applied (to avoid double
        // consumption)
        int samplesNeedingReagents = countSamplesWithoutReagents(pageId, sampleIds);
        LogEvent.logInfo(this.getClass().getName(), "bulkApplyValues",
                "Total samples: " + sampleIds.size() + ", samples needing reagents: " + samplesNeedingReagents);

        // Process inventory consumption only for samples that don't have reagents yet
        if (samplesNeedingReagents > 0) {
            consumeReagentsFromInventory(data, samplesNeedingReagents, userId);
        } else {
            LogEvent.logInfo(this.getClass().getName(), "bulkApplyValues",
                    "Skipping reagent consumption - all samples already have reagents applied");
        }

        // Process in batches of BATCH_SIZE (50)
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<Integer> batch = sampleIds.subList(i, endIndex);

            for (Integer sampleId : batch) {
                LogEvent.logInfo(this.getClass().getName(), "bulkApplyValues",
                        "Looking up pageId=" + pageId + ", sampleItemId=" + sampleId);
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    // Sample doesn't have a NotebookPageSample record for this page yet
                    // This can happen with child samples (aliquots) that weren't properly linked
                    // Create a new record for them
                    NoteBookPage page = noteBookPageService.get(pageId);
                    if (page != null) {
                        nps = new NotebookPageSample();
                        nps.setNotebookPage(page);
                        nps.setSampleItemId(String.valueOf(sampleId));
                        nps.setStatus(Status.PENDING);
                        nps.setSysUserId(userId);

                        // Initialize data with identification fields from first page if available
                        Map<String, Object> initialData = new HashMap<>();
                        copyIdentificationFieldsFromFirstPage(page, sampleId, initialData);
                        nps.setData(initialData);

                        notebookPageSampleService.save(nps);
                        LogEvent.logInfo(this.getClass().getName(), "bulkApplyValues",
                                "Created missing NotebookPageSample for sampleId=" + sampleId + " on pageId=" + pageId);
                    } else {
                        LogEvent.logWarn(this.getClass().getName(), "bulkApplyValues",
                                "Page not found: " + pageId + ", skipping sample " + sampleId);
                        continue;
                    }
                }

                // Merge new data with existing data
                Map<String, Object> existingData = nps.getData();
                if (existingData == null) {
                    existingData = new HashMap<>();
                }
                existingData.putAll(data);
                nps.setData(existingData);

                // Update timestamp (BaseObject uses setLastupdated)
                nps.setLastupdated(new Timestamp(System.currentTimeMillis()));

                // If status is PENDING and data is being applied, transition to IN_PROGRESS
                if (nps.getStatus() == Status.PENDING) {
                    nps.setStatus(Status.IN_PROGRESS);
                }

                notebookPageSampleService.update(nps);
                updatedCount++;
            }
        }

        LogEvent.logInfo(this.getClass().getName(), "bulkApplyValues",
                "Completed. Total updated: " + updatedCount + " of " + sampleIds.size());
        return updatedCount;
    }

    /**
     * Copy identification fields from the first page (page 1) to initialize data
     * for samples on subsequent pages. Identification fields (localName,
     * scientificName, plantPart, etc.) originate from the first page and should be
     * preserved across all workflow stages.
     *
     * @param currentPage the current notebook page
     * @param sampleId    the sample item ID
     * @param targetData  the data map to populate with identification fields
     */
    private void copyIdentificationFieldsFromFirstPage(NoteBookPage currentPage, Integer sampleId,
            Map<String, Object> targetData) {
        try {
            // Identification fields that should flow from first page through all stages
            String[] identificationFields = { "localName", "scientificName", "plantPart", "sourceType",
                    "sampleCategory", "collectionDate", "collectedBy", "originLocation", "collectionSite",
                    "intendedUse", "sampleCondition", "speciesIdentification" };

            // Get the notebook from the current page
            if (currentPage == null || currentPage.getNotebook() == null) {
                LogEvent.logDebug(this.getClass().getName(), "copyIdentificationFieldsFromFirstPage",
                        "Current page or notebook is null, cannot copy identification fields");
                return;
            }

            // Get all pages for this notebook and find the first page
            // First pages are typically those with lower page numbers/order
            List<NoteBookPage> notebookPages = noteBookPageService.getByNotebook(currentPage.getNotebook());
            if (notebookPages == null || notebookPages.isEmpty()) {
                LogEvent.logDebug(this.getClass().getName(), "copyIdentificationFieldsFromFirstPage",
                        "No pages found for notebook");
                return;
            }

            // Get the first page (lowest pageNumber or order)
            NoteBookPage firstPage = notebookPages.stream().min((p1, p2) -> {
                // Compare by pageNumber if available, otherwise use natural order
                Integer pn1 = p1.getPageNumber() != null ? p1.getPageNumber() : Integer.MAX_VALUE;
                Integer pn2 = p2.getPageNumber() != null ? p2.getPageNumber() : Integer.MAX_VALUE;
                return pn1.compareTo(pn2);
            }).orElse(null);

            if (firstPage == null || firstPage.getId().equals(currentPage.getId())) {
                // Already on first page or no pages found
                LogEvent.logDebug(this.getClass().getName(), "copyIdentificationFieldsFromFirstPage",
                        "Already on first page or cannot determine first page, skipping copy");
                return;
            }

            // Get the sample from the first page
            NotebookPageSample firstPageSample = notebookPageSampleService.getByPageIdAndSampleItemId(firstPage.getId(),
                    sampleId);
            if (firstPageSample == null || firstPageSample.getData() == null) {
                LogEvent.logDebug(this.getClass().getName(), "copyIdentificationFieldsFromFirstPage",
                        "Sample not found on first page (pageId=" + firstPage.getId() + ", sampleId=" + sampleId + ")");
                return;
            }

            // Copy identification fields from first page
            Map<String, Object> firstPageData = firstPageSample.getData();
            int copiedCount = 0;
            for (String field : identificationFields) {
                if (firstPageData.containsKey(field)) {
                    targetData.put(field, firstPageData.get(field));
                    copiedCount++;
                }
            }

            if (copiedCount > 0) {
                LogEvent.logInfo(this.getClass().getName(), "copyIdentificationFieldsFromFirstPage",
                        "Copied " + copiedCount + " identification fields for sampleId=" + sampleId + " from pageId="
                                + firstPage.getId() + " to pageId=" + currentPage.getId());
            }

        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "copyIdentificationFieldsFromFirstPage",
                    "Error copying identification fields: " + e.getMessage());
            // Continue without identification fields - they may be added later
        }
    }

    /**
     * Count samples that don't already have reagents applied to avoid double
     * consumption when bulk apply is called multiple times.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to check
     * @return count of samples that don't have selectedReagents in their data
     */
    private int countSamplesWithoutReagents(Integer pageId, List<Integer> sampleIds) {
        int count = 0;
        int samplesWithReagents = 0;
        int samplesNotFound = 0;

        for (Integer sampleId : sampleIds) {
            NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
            if (nps != null) {
                Map<String, Object> existingData = nps.getData();
                // Sample needs reagents if it has no data or no selectedReagents key,
                // or if selectedReagents is an empty list
                if (existingData == null) {
                    count++;
                } else {
                    Object selectedReagents = existingData.get("selectedReagents");
                    if (selectedReagents == null) {
                        count++;
                    } else if (selectedReagents instanceof List && ((List<?>) selectedReagents).isEmpty()) {
                        count++;
                    } else {
                        samplesWithReagents++;
                    }
                }
            } else {
                samplesNotFound++;
            }
        }

        LogEvent.logInfo(this.getClass().getName(), "countSamplesWithoutReagents",
                "Page " + pageId + ": Total requested=" + sampleIds.size() + ", needReagents=" + count
                        + ", alreadyHaveReagents=" + samplesWithReagents + ", notFound=" + samplesNotFound);

        return count;
    }

    /**
     * Consume reagents from inventory when they are applied to samples. Each
     * reagent is consumed once per sample being processed.
     *
     * @param data        the data map containing selectedReagents list
     * @param sampleCount number of samples being processed
     * @param userId      user performing the operation
     */
    @SuppressWarnings("unchecked")
    private void consumeReagentsFromInventory(Map<String, Object> data, int sampleCount, String userId) {
        LogEvent.logInfo(this.getClass().getName(), "consumeReagentsFromInventory",
                "Starting inventory consumption for " + sampleCount + " samples. Data keys: " + data.keySet()
                        + ". NOTE: Each reagent will consume " + sampleCount + " units (1 per sample).");

        // Check for selected reagents in the data
        Object selectedReagentsObj = data.get("selectedReagents");
        if (selectedReagentsObj == null) {
            LogEvent.logDebug(this.getClass().getName(), "consumeReagentsFromInventory",
                    "No selectedReagents in data map");
            return;
        }

        LogEvent.logInfo(this.getClass().getName(), "consumeReagentsFromInventory", "Processing selectedReagents: "
                + selectedReagentsObj + " (type: " + selectedReagentsObj.getClass().getName() + ")");

        List<?> rawList;
        if (selectedReagentsObj instanceof List) {
            rawList = (List<?>) selectedReagentsObj;
        } else {
            LogEvent.logWarn(this.getClass().getName(), "consumeReagentsFromInventory",
                    "selectedReagents is not a List, it's: " + selectedReagentsObj.getClass().getName());
            return;
        }

        if (rawList.isEmpty()) {
            LogEvent.logDebug(this.getClass().getName(), "consumeReagentsFromInventory",
                    "selectedReagents list is empty");
            return;
        }

        LogEvent.logInfo(this.getClass().getName(), "consumeReagentsFromInventory",
                "Processing " + rawList.size() + " reagent type(s). Total consumption will be " + rawList.size() + " * "
                        + sampleCount + " = " + (rawList.size() * sampleCount) + " units across all reagents");

        // Consume 1 unit of each reagent per sample
        // (This is a simplification - in reality, different reagents may have different
        // consumption rates)
        double quantityPerSample = 1.0;
        double totalQuantity = quantityPerSample * sampleCount;

        for (Object reagentIdObj : rawList) {
            try {
                // Handle both String and Integer/Long types from JSON deserialization
                Long itemId;
                if (reagentIdObj instanceof Number) {
                    itemId = ((Number) reagentIdObj).longValue();
                } else if (reagentIdObj instanceof String) {
                    itemId = Long.valueOf((String) reagentIdObj);
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "consumeReagentsFromInventory",
                            "Unknown reagent ID type: " + reagentIdObj.getClass().getName() + " value: "
                                    + reagentIdObj);
                    continue;
                }

                LogEvent.logInfo(this.getClass().getName(), "consumeReagentsFromInventory",
                        "Processing reagent itemId: " + itemId + ", quantity: " + totalQuantity);

                // Check if sufficient inventory is available
                if (inventoryManagementService.isSufficientInventoryAvailable(itemId, totalQuantity)) {
                    // Consume inventory using FEFO (First Expired, First Out)
                    inventoryManagementService.consumeInventoryFEFO(itemId, totalQuantity, null, // testResultId - not
                                                                                                 // applicable for
                                                                                                 // sample preparation
                            null, // analysisId - not applicable for sample preparation
                            userId);

                    LogEvent.logInfo(this.getClass().getName(), "consumeReagentsFromInventory", "Successfully consumed "
                            + totalQuantity + " units of reagent ID " + itemId + " for " + sampleCount + " samples");
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "consumeReagentsFromInventory",
                            "Insufficient inventory for reagent ID " + itemId + " - needed " + totalQuantity
                                    + " units");
                }
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getName(), "consumeReagentsFromInventory",
                        "Invalid reagent ID format: " + reagentIdObj + " - " + e.getMessage());
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "consumeReagentsFromInventory",
                        "Error consuming inventory for reagent " + reagentIdObj + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    @Transactional
    public int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status, String userId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return 0;
        }

        // Delegate to NotebookPageSampleService which handles batch processing
        return notebookPageSampleService.bulkUpdateStatus(pageId, sampleIds, status, userId);
    }

    @Override
    @Transactional
    public int bulkUpdateStatusString(Integer pageId, List<String> sampleIds, Status status, String userId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return 0;
        }

        // Delegate to NotebookPageSampleService which handles String-based sample IDs
        return notebookPageSampleService.bulkUpdateStatusString(pageId, sampleIds, status, userId);
    }

    @Override
    @Transactional
    public int bulkApplyValuesString(Integer pageId, List<String> sampleIds, Map<String, Object> data, String userId) {
        if (sampleIds == null || sampleIds.isEmpty() || data == null || data.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        NoteBookPage page = null;

        // Process in batches of BATCH_SIZE (50)
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<String> batch = sampleIds.subList(i, endIndex);

            for (String sampleId : batch) {
                NotebookPageSample nps = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);
                if (nps == null) {
                    // Sample doesn't have a NotebookPageSample record for this page yet
                    // Lazy load page reference only when needed
                    if (page == null) {
                        page = noteBookPageService.get(pageId);
                        if (page == null) {
                            LogEvent.logWarn(this.getClass().getName(), "bulkApplyValuesString",
                                    "Page not found: " + pageId);
                            continue;
                        }
                    }

                    // Create new NotebookPageSample record
                    nps = new NotebookPageSample();
                    nps.setNotebookPage(page);
                    nps.setSampleItemId(sampleId);
                    nps.setStatus(Status.IN_PROGRESS);
                    nps.setSysUserId(userId);
                    nps.setData(new HashMap<>(data));
                    notebookPageSampleService.save(nps);
                    updatedCount++;
                    LogEvent.logInfo(this.getClass().getName(), "bulkApplyValuesString",
                            "Created NotebookPageSample for sampleId=" + sampleId + " on pageId=" + pageId);
                } else {
                    // Merge new data with existing data
                    Map<String, Object> existingData = nps.getData();
                    if (existingData == null) {
                        existingData = new HashMap<>();
                    }
                    existingData.putAll(data);
                    nps.setData(existingData);
                    nps.setSysUserId(userId);
                    notebookPageSampleService.update(nps);
                    updatedCount++;
                }
            }
        }

        return updatedCount;
    }

    @Override
    public NotebookPageSampleService.PageProgress getPageProgress(Integer pageId) {
        return notebookPageSampleService.getPageProgress(pageId);
    }

    @Override
    public List<NotebookPageSample> getSamplesPaginated(Integer pageId, Status status, int page, int size) {
        return notebookPageSampleService.getByPageIdPaginated(pageId, status, page, size);
    }

    @Override
    public long getSamplesCount(Integer pageId, Status status) {
        return notebookPageSampleService.getCountByPageId(pageId, status);
    }

    @Override
    @Transactional
    public boolean markPageComplete(Integer pageId, String userId, boolean requireComplete) {
        // Get progress to check if all samples are done
        NotebookPageSampleService.PageProgress progress = notebookPageSampleService.getPageProgress(pageId);

        if (requireComplete) {
            // All samples must be COMPLETED or SKIPPED (no PENDING or IN_PROGRESS)
            if (progress.pending() > 0 || progress.inProgress() > 0) {
                return false;
            }
        }

        // Get the page and mark it as completed
        NoteBookPage page = noteBookPageService.get(pageId);
        if (page != null) {
            page.setCompleted(true);
            page.setSysUserId(userId);
            noteBookPageService.update(page);
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public Map<String, Object> assignSamplesToStorage(Integer pageId, List<Integer> sampleIds, Integer boxId,
            String wellCoordinate, Map<String, Object> storageData, String userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int assignedCount = 0;

        if (sampleIds == null || sampleIds.isEmpty()) {
            result.put("success", false);
            result.put("error", "No sample IDs provided");
            return result;
        }

        // Box ID is optional for labs that don't use rack/box hierarchy
        // When null, samples can be tracked at higher levels (shelf, device, room)
        if (boxId == null && wellCoordinate != null) {
            result.put("success", false);
            result.put("error", "Well coordinate specified but no box ID provided");
            return result;
        }

        // For single well assignment, assign all samples to the same well
        // This is typically used when assigning one sample at a time
        for (Integer sampleId : sampleIds) {
            try {
                // Get the NotebookPageSample to find the actual SampleItem ID
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    errors.add("Sample " + sampleId + " not found in notebook page");
                    continue;
                }

                // Get the SampleItem ID (the nps.sampleItemId is the actual sample item ID)
                String sampleItemId = String.valueOf(nps.getSampleItemId());

                // Create storage assignment using the storage service
                // Handle box or hierarchy level storage assignment (room, device, shelf, rack,
                // box)
                try {
                    String locationId;
                    String locationType;

                    if (boxId != null) {
                        // Box-level storage (with wells)
                        locationId = String.valueOf(boxId);
                        locationType = "box";
                    } else if (storageData != null && storageData.get("locationId") != null
                            && storageData.get("locationType") != null) {
                        // Hierarchy-level storage (room, device, shelf, or rack)
                        locationId = String.valueOf(storageData.get("locationId"));
                        locationType = String.valueOf(storageData.get("locationType"));
                    } else if (storageData != null && storageData.get("shelfId") != null) {
                        // Legacy shelf-level storage (backward compatibility)
                        locationId = String.valueOf(storageData.get("shelfId"));
                        locationType = "shelf";
                    } else {
                        // Fallback to general storage
                        locationId = null;
                        locationType = "general";
                    }
                    Map<String, Object> assignmentResult = sampleStorageService.assignSampleItemWithLocation(
                            sampleItemId, locationId, locationType, wellCoordinate,
                            storageData != null ? (String) storageData.get("notes") : null);

                    if (assignmentResult != null && assignmentResult.containsKey("assignmentId")) {
                        // Update the notebook page sample data with storage info
                        Map<String, Object> existingData = nps.getData();
                        if (existingData == null) {
                            existingData = new HashMap<>();
                        }
                        if (storageData != null) {
                            existingData.putAll(storageData);
                        }
                        existingData.put("storageWell", wellCoordinate);
                        existingData.put("storageAssignmentId", assignmentResult.get("assignmentId"));

                        // Use hierarchicalPath from storage service, fallback to frontend storagePath
                        String hierarchicalPath = (String) assignmentResult.get("hierarchicalPath");
                        if (hierarchicalPath == null || hierarchicalPath.trim().isEmpty()
                                || "Unknown".equals(hierarchicalPath)) {
                            // Fallback to storagePath from frontend if storage service didn't provide one
                            hierarchicalPath = storageData != null ? (String) storageData.get("storagePath") : null;
                        }
                        if (hierarchicalPath != null && !hierarchicalPath.trim().isEmpty()) {
                            existingData.put("storagePath", hierarchicalPath);
                        }

                        nps.setData(existingData);
                        nps.setLastupdated(new Timestamp(System.currentTimeMillis()));
                        notebookPageSampleService.update(nps);

                        assignedCount++;
                        LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage",
                                "Assigned sample " + sampleItemId + " to box " + boxId + " well " + wellCoordinate);
                    }
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    // Check if it's a duplicate assignment error
                    if (errorMsg != null && errorMsg.contains("already assigned")) {
                        errors.add(
                                "Sample " + sampleId + " is already assigned to storage. Use move operation instead.");
                    } else if (errorMsg != null && errorMsg.contains("already occupied")) {
                        errors.add("Well " + wellCoordinate + " is already occupied");
                    } else {
                        errors.add("Failed to assign sample " + sampleId + ": " + errorMsg);
                    }
                    LogEvent.logError(this.getClass().getName(), "assignSamplesToStorage",
                            "Error assigning sample " + sampleId + ": " + e.getMessage());
                }
            } catch (Exception e) {
                errors.add("Error processing sample " + sampleId + ": " + e.getMessage());
                LogEvent.logError(this.getClass().getName(), "assignSamplesToStorage",
                        "Error processing sample " + sampleId + ": " + e.getMessage());
            }
        }

        // Success if at least some samples were assigned, even if there are individual
        // errors
        result.put("success", assignedCount > 0);
        result.put("assignedCount", assignedCount);
        result.put("requestedCount", sampleIds.size());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> autoAssignSamplesToStorage(Integer pageId, List<Integer> sampleIds, Integer boxId,
            Integer rows, Integer columns, List<String> occupiedWells, Map<String, Object> storageData, String userId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> assignments = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (sampleIds == null || sampleIds.isEmpty()) {
            result.put("success", false);
            result.put("error", "No sample IDs provided");
            return result;
        }

        if (boxId == null) {
            result.put("success", false);
            result.put("error", "Box ID is required for auto-assignment to well coordinates");
            return result;
        }

        // Get box dimensions (default to 8x12 for 96-well plate)
        int numRows = rows != null ? rows : 8;
        int numColumns = columns != null ? columns : 12;

        // Build set of occupied wells
        Set<String> occupied = new HashSet<>();
        if (occupiedWells != null) {
            occupied.addAll(occupiedWells);
        }

        // Generate list of all wells in order (A1, A2, ..., A12, B1, B2, ...)
        List<String> allWells = new ArrayList<>();
        for (int row = 0; row < numRows; row++) {
            char rowLetter = (char) ('A' + row);
            for (int col = 1; col <= numColumns; col++) {
                allWells.add("" + rowLetter + col);
            }
        }

        // Find available wells (not occupied)
        List<String> availableWells = new ArrayList<>();
        for (String well : allWells) {
            if (!occupied.contains(well)) {
                availableWells.add(well);
            }
        }

        // Check if we have enough available wells
        if (availableWells.size() < sampleIds.size()) {
            result.put("success", false);
            result.put("error", "Not enough available wells. Need " + sampleIds.size() + " but only "
                    + availableWells.size() + " available.");
            result.put("availableCount", availableWells.size());
            result.put("requestedCount", sampleIds.size());
            return result;
        }

        // Assign each sample to the next available well
        int assignedCount = 0;
        for (int i = 0; i < sampleIds.size(); i++) {
            Integer sampleId = sampleIds.get(i);
            String wellCoordinate = availableWells.get(i);

            try {
                // Get the NotebookPageSample to find the actual SampleItem ID
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    errors.add("Sample " + sampleId + " not found in notebook page");
                    continue;
                }

                String sampleItemId = String.valueOf(nps.getSampleItemId());

                try {
                    // Create storage assignment
                    Map<String, Object> assignmentResult = sampleStorageService.assignSampleItemWithLocation(
                            sampleItemId, String.valueOf(boxId), "box", wellCoordinate,
                            storageData != null ? (String) storageData.get("notes") : null);

                    if (assignmentResult != null && assignmentResult.containsKey("assignmentId")) {
                        // Update notebook page sample data
                        Map<String, Object> existingData = nps.getData();
                        if (existingData == null) {
                            existingData = new HashMap<>();
                        }
                        if (storageData != null) {
                            existingData.putAll(storageData);
                        }
                        existingData.put("storageWell", wellCoordinate);
                        existingData.put("storageAssignmentId", assignmentResult.get("assignmentId"));
                        existingData.put("storagePath", assignmentResult.get("hierarchicalPath"));
                        nps.setData(existingData);
                        nps.setLastupdated(new Timestamp(System.currentTimeMillis()));
                        notebookPageSampleService.update(nps);

                        // Record the assignment
                        Map<String, Object> assignment = new HashMap<>();
                        assignment.put("sampleId", sampleId);
                        assignment.put("sampleItemId", sampleItemId);
                        assignment.put("wellCoordinate", wellCoordinate);
                        assignment.put("assignmentId", assignmentResult.get("assignmentId"));
                        assignments.add(assignment);

                        // Mark this well as occupied for subsequent iterations
                        occupied.add(wellCoordinate);
                        assignedCount++;

                        LogEvent.logInfo(this.getClass().getName(), "autoAssignSamplesToStorage",
                                "Auto-assigned sample " + sampleItemId + " to box " + boxId + " well "
                                        + wellCoordinate);
                    }
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("already assigned")) {
                        errors.add("Sample " + sampleId + " is already assigned to storage");
                    } else {
                        errors.add(
                                "Failed to assign sample " + sampleId + " to well " + wellCoordinate + ": " + errorMsg);
                    }
                    LogEvent.logError(this.getClass().getName(), "autoAssignSamplesToStorage",
                            "Error auto-assigning sample " + sampleId + ": " + e.getMessage());
                }
            } catch (Exception e) {
                errors.add("Error processing sample " + sampleId + ": " + e.getMessage());
            }
        }

        result.put("success", assignedCount > 0);
        result.put("updatedCount", assignedCount);
        result.put("requestedCount", sampleIds.size());
        result.put("assignments", assignments);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> autoAssignSamplesToStorageString(Integer pageId, List<String> sampleIds, Integer boxId,
            Integer rows, Integer columns, List<String> occupiedWells, Map<String, Object> storageData, String userId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> assignments = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (sampleIds == null || sampleIds.isEmpty()) {
            result.put("success", false);
            result.put("error", "No sample IDs provided");
            return result;
        }

        if (boxId == null) {
            result.put("success", false);
            result.put("error", "Box ID is required for auto-assignment");
            return result;
        }

        // Get box dimensions (default to 8x12 for 96-well plate)
        int numRows = rows != null ? rows : 8;
        int numColumns = columns != null ? columns : 12;

        // Build set of occupied wells
        Set<String> occupied = new HashSet<>();
        if (occupiedWells != null) {
            occupied.addAll(occupiedWells);
        }

        // Generate list of all wells in order (A1, A2, ..., A12, B1, B2, ...)
        List<String> allWells = new ArrayList<>();
        for (int row = 0; row < numRows; row++) {
            char rowLetter = (char) ('A' + row);
            for (int col = 1; col <= numColumns; col++) {
                allWells.add("" + rowLetter + col);
            }
        }

        // Find available wells (not occupied)
        List<String> availableWells = new ArrayList<>();
        for (String well : allWells) {
            if (!occupied.contains(well)) {
                availableWells.add(well);
            }
        }

        // Check if we have enough available wells
        if (availableWells.size() < sampleIds.size()) {
            result.put("success", false);
            result.put("error", "Not enough available wells. Need " + sampleIds.size() + " but only "
                    + availableWells.size() + " available.");
            result.put("availableCount", availableWells.size());
            result.put("requestedCount", sampleIds.size());
            return result;
        }

        // Check if dealing with composite sample IDs (pathology workflow)
        boolean hasCompositeSampleIds = sampleIds.stream().anyMatch(id -> id.contains("_"));

        // Get box info for storage location string
        String boxLabel = "Box " + boxId;
        try {
            org.openelisglobal.storage.valueholder.StorageBox box = storageBoxDAO.get(boxId).orElse(null);
            if (box != null) {
                boxLabel = box.getLabel();
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "autoAssignSamplesToStorageString",
                    "Could not fetch box info for boxId " + boxId + ": " + e.getMessage());
        }

        // Assign each sample to the next available well
        int assignedCount = 0;
        for (int i = 0; i < sampleIds.size(); i++) {
            String sampleIdStr = sampleIds.get(i);
            String wellCoordinate = availableWells.get(i);

            try {
                // Get or create the NotebookPageSample using string-based lookup
                NotebookPageSample nps = notebookPageSampleService.getBySampleItemIdAndPageId(sampleIdStr, pageId);
                if (nps == null) {
                    // Auto-create the page sample for composite IDs
                    notebookPageSampleService.createPageSampleForPageString(pageId, sampleIdStr,
                            NotebookPageSample.Status.IN_PROGRESS);
                    nps = notebookPageSampleService.getBySampleItemIdAndPageId(sampleIdStr, pageId);
                }

                if (nps == null) {
                    errors.add("Sample " + sampleIdStr + " could not be created in notebook page");
                    continue;
                }

                // For composite sample IDs, skip the SampleStorageService calls
                // Just update the NotebookPageSample record directly
                if (hasCompositeSampleIds) {
                    // Update notebook page sample data with storage info
                    Map<String, Object> existingData = nps.getData();
                    if (existingData == null) {
                        existingData = new HashMap<>();
                    }
                    if (storageData != null) {
                        existingData.putAll(storageData);
                    }
                    existingData.put("boxId", boxId);
                    existingData.put("storageBox", boxLabel);
                    existingData.put("storageWell", wellCoordinate);
                    existingData.put("wellCoordinate", wellCoordinate);
                    existingData.put("storageLocation", boxLabel + " - " + wellCoordinate);
                    existingData.put("dateStored", java.time.LocalDate.now().toString());

                    // Update status to IN_PROGRESS if PENDING
                    if (nps.getStatus() == NotebookPageSample.Status.PENDING) {
                        nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                    }

                    nps.setData(existingData);
                    nps.setLastupdated(new Timestamp(System.currentTimeMillis()));
                    notebookPageSampleService.update(nps);

                    // Record the assignment
                    Map<String, Object> assignment = new HashMap<>();
                    assignment.put("sampleId", sampleIdStr);
                    assignment.put("wellCoordinate", wellCoordinate);
                    assignment.put("storageLocation", boxLabel + " - " + wellCoordinate);
                    assignments.add(assignment);

                    occupied.add(wellCoordinate);
                    assignedCount++;

                    LogEvent.logInfo(this.getClass().getName(), "autoAssignSamplesToStorageString",
                            "Auto-assigned composite sample " + sampleIdStr + " to box " + boxId + " well "
                                    + wellCoordinate);
                } else {
                    // For regular integer sample IDs, use the storage service
                    try {
                        Map<String, Object> assignmentResult = sampleStorageService.assignSampleItemWithLocation(
                                sampleIdStr, String.valueOf(boxId), "box", wellCoordinate,
                                storageData != null ? (String) storageData.get("notes") : null);

                        if (assignmentResult != null && assignmentResult.containsKey("assignmentId")) {
                            Map<String, Object> existingData = nps.getData();
                            if (existingData == null) {
                                existingData = new HashMap<>();
                            }
                            if (storageData != null) {
                                existingData.putAll(storageData);
                            }
                            existingData.put("storageWell", wellCoordinate);
                            existingData.put("storageAssignmentId", assignmentResult.get("assignmentId"));
                            existingData.put("storagePath", assignmentResult.get("hierarchicalPath"));

                            if (nps.getStatus() == NotebookPageSample.Status.PENDING) {
                                nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                            }

                            nps.setData(existingData);
                            nps.setLastupdated(new Timestamp(System.currentTimeMillis()));
                            notebookPageSampleService.update(nps);

                            Map<String, Object> assignment = new HashMap<>();
                            assignment.put("sampleId", sampleIdStr);
                            assignment.put("sampleItemId", sampleIdStr);
                            assignment.put("wellCoordinate", wellCoordinate);
                            assignment.put("assignmentId", assignmentResult.get("assignmentId"));
                            assignments.add(assignment);

                            occupied.add(wellCoordinate);
                            assignedCount++;

                            LogEvent.logInfo(this.getClass().getName(), "autoAssignSamplesToStorageString",
                                    "Auto-assigned sample " + sampleIdStr + " to box " + boxId + " well "
                                            + wellCoordinate);
                        }
                    } catch (Exception e) {
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("already assigned")) {
                            errors.add("Sample " + sampleIdStr + " is already assigned to storage");
                        } else {
                            errors.add("Failed to assign sample " + sampleIdStr + " to well " + wellCoordinate + ": "
                                    + errorMsg);
                        }
                        LogEvent.logError(this.getClass().getName(), "autoAssignSamplesToStorageString",
                                "Error auto-assigning sample " + sampleIdStr + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                errors.add("Error processing sample " + sampleIdStr + ": " + e.getMessage());
            }
        }

        result.put("success", assignedCount > 0);
        result.put("updatedCount", assignedCount);
        result.put("requestedCount", sampleIds.size());
        result.put("assignments", assignments);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> assignSamplesToStorageWithWellMap(Integer pageId, List<Integer> sampleIds, Integer boxId,
            Map<String, String> wellAssignments, Map<String, Object> storageData, String userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int assignedCount = 0;

        if (wellAssignments == null || wellAssignments.isEmpty()) {
            result.put("success", false);
            result.put("error", "No well assignments provided");
            return result;
        }

        if (boxId == null) {
            result.put("success", false);
            result.put("error",
                    "Box ID is required for well assignments (well coordinates like 'A1' only make sense within a box)");
            return result;
        }

        // Process each sample with its specific well assignment
        for (Map.Entry<String, String> entry : wellAssignments.entrySet()) {
            String sampleIdStr = entry.getKey();
            String wellCoordinate = entry.getValue();

            try {
                Integer sampleId = Integer.parseInt(sampleIdStr);

                // Get the NotebookPageSample to find the actual SampleItem ID
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    errors.add("Sample " + sampleId + " not found in notebook page");
                    continue;
                }

                String sampleItemId = String.valueOf(nps.getSampleItemId());

                try {
                    // Create storage assignment using the storage service
                    Map<String, Object> assignmentResult = sampleStorageService.assignSampleItemWithLocation(
                            sampleItemId, String.valueOf(boxId), "box", wellCoordinate,
                            storageData != null ? (String) storageData.get("notes") : null);

                    if (assignmentResult != null && assignmentResult.containsKey("assignmentId")) {
                        // Update the notebook page sample data with storage info
                        Map<String, Object> existingData = nps.getData();
                        if (existingData == null) {
                            existingData = new HashMap<>();
                        }
                        if (storageData != null) {
                            existingData.putAll(storageData);
                        }
                        existingData.put("storageWell", wellCoordinate);
                        existingData.put("storageAssignmentId", assignmentResult.get("assignmentId"));
                        existingData.put("storagePath", assignmentResult.get("hierarchicalPath"));
                        nps.setData(existingData);
                        nps.setLastupdated(new Timestamp(System.currentTimeMillis()));
                        notebookPageSampleService.update(nps);

                        assignedCount++;
                        LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorageWithWellMap",
                                "Assigned sample " + sampleItemId + " to box " + boxId + " well " + wellCoordinate);
                    }
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("already assigned")) {
                        errors.add(
                                "Sample " + sampleId + " is already assigned to storage. Use move operation instead.");
                    } else if (errorMsg != null && errorMsg.contains("already occupied")) {
                        errors.add("Well " + wellCoordinate + " is already occupied");
                    } else {
                        errors.add(
                                "Failed to assign sample " + sampleId + " to well " + wellCoordinate + ": " + errorMsg);
                    }
                    LogEvent.logError(this.getClass().getName(), "assignSamplesToStorageWithWellMap",
                            "Error assigning sample " + sampleId + ": " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid sample ID format: " + sampleIdStr);
            } catch (Exception e) {
                errors.add("Error processing sample " + sampleIdStr + ": " + e.getMessage());
                LogEvent.logError(this.getClass().getName(), "assignSamplesToStorageWithWellMap",
                        "Error processing sample " + sampleIdStr + ": " + e.getMessage());
            }
        }

        result.put("success", assignedCount > 0);
        result.put("assignedCount", assignedCount);
        result.put("requestedCount", wellAssignments.size());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReport(Integer pageId, List<Integer> sampleIds, String reportType, String reportFormat,
            String userId) {
        if (pageId == null || sampleIds == null || sampleIds.isEmpty()) {
            return new byte[0];
        }

        try {
            // Get sample data for the report
            // Note: sampleIds are NotebookPageSample.id values (primary keys), not
            // sampleItemIds
            List<Map<String, Object>> sampleData = new ArrayList<>();
            LogEvent.logInfo(this.getClass().getName(), "generateReport",
                    "Processing " + sampleIds.size() + " samples for report, format=" + reportFormat);

            for (Integer sampleId : sampleIds) {
                NotebookPageSample nps = notebookPageSampleService.get(sampleId);
                if (nps == null) {
                    LogEvent.logWarn(this.getClass().getName(), "generateReport", "Sample not found: " + sampleId);
                    continue;
                }

                // Skip page verification - the frontend already filters by page
                // The sampleIds passed are already from the correct page

                Map<String, Object> row = new HashMap<>();
                row.put("sampleId", sampleId);
                row.put("accessionNumber", getAccessionNumber(nps));
                row.put("status", nps.getStatus() != null ? nps.getStatus().name() : "PENDING");
                if (nps.getData() != null) {
                    row.putAll(nps.getData());
                }
                sampleData.add(row);
            }

            LogEvent.logInfo(this.getClass().getName(), "generateReport",
                    "Collected " + sampleData.size() + " samples for report generation");

            // Generate report based on format
            // Note: PDF generation not yet implemented - use CSV as default
            switch (reportFormat.toUpperCase()) {
            case "CSV":
                return generateCSVReport(sampleData, reportType);
            case "JSON":
                return generateJSONReport(sampleData, reportType);
            case "EXCEL":
                return generateExcelReport(sampleData, reportType);
            case "PDF":
                // PDF generation requires Apache PDFBox or iText library
                // For now, return CSV data - controller should be updated to not offer PDF
                LogEvent.logWarn(this.getClass().getName(), "generateReport",
                        "PDF format not yet implemented, returning CSV");
                return generateCSVReport(sampleData, reportType);
            default:
                return generateCSVReport(sampleData, reportType);
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "generateReport",
                    "Error generating report: " + e.getMessage());
            return new byte[0];
        }
    }

    private byte[] generateCSVReport(List<Map<String, Object>> sampleData, String reportType) {
        StringBuilder csv = new StringBuilder();

        // Header row - collect all unique keys
        Set<String> allKeys = new java.util.LinkedHashSet<>();
        allKeys.add("sampleId");
        allKeys.add("accessionNumber");
        allKeys.add("status");
        for (Map<String, Object> row : sampleData) {
            allKeys.addAll(row.keySet());
        }

        // Write header
        csv.append(String.join(",", allKeys)).append("\n");

        // Write data rows
        for (Map<String, Object> row : sampleData) {
            List<String> values = new ArrayList<>();
            for (String key : allKeys) {
                Object value = row.get(key);
                String strValue = value != null ? value.toString().replace(",", ";").replace("\"", "'") : "";
                values.add("\"" + strValue + "\"");
            }
            csv.append(String.join(",", values)).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] generateJSONReport(List<Map<String, Object>> sampleData, String reportType) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", reportType);
            report.put("generatedAt", java.time.Instant.now().toString());
            report.put("sampleCount", sampleData.size());
            report.put("samples", sampleData);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "generateJSONReport", "Error: " + e.getMessage());
            return new byte[0];
        }
    }

    private byte[] generateExcelReport(List<Map<String, Object>> sampleData, String reportType) {
        // For now, return CSV format - Excel generation would require Apache POI
        return generateCSVReport(sampleData, reportType);
    }

    private String getAccessionNumber(NotebookPageSample nps) {
        try {
            String sampleItemIdStr = nps.getSampleItemId();
            if (sampleItemIdStr != null) {
                SampleItem sampleItem = sampleItemService.get(sampleItemIdStr);
                if (sampleItem != null && sampleItem.getSample() != null) {
                    return sampleItem.getSample().getAccessionNumber();
                }
            }
        } catch (Exception e) {
            // Ignore - return empty
        }
        return "";
    }

    private String getExternalId(NotebookPageSample nps) {
        try {
            String sampleItemIdStr = nps.getSampleItemId();
            if (sampleItemIdStr != null) {
                SampleItem sampleItem = sampleItemService.get(sampleItemIdStr);
                if (sampleItem != null && sampleItem.getExternalId() != null) {
                    return sampleItem.getExternalId();
                }
            }
        } catch (Exception e) {
            // Ignore - return empty
        }
        return "";
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateREDCapExport(Integer pageId, List<Integer> sampleIds, String recordIdField, String eventName,
            String instrumentName, String userId) {
        if (pageId == null || sampleIds == null || sampleIds.isEmpty()) {
            return new byte[0];
        }

        try {
            StringBuilder csv = new StringBuilder();

            // REDCap standard fields
            String recordIdFieldName = recordIdField != null && !recordIdField.isEmpty() ? recordIdField : "record_id";

            // Build header - REDCap requires specific column format
            List<String> headers = new ArrayList<>();
            headers.add(recordIdFieldName);
            if (eventName != null && !eventName.isEmpty()) {
                headers.add("redcap_event_name");
            }

            // Add data fields
            headers.add("accession_number");
            headers.add("sample_type");
            headers.add("collection_date");
            headers.add("validation_status");
            headers.add("test_results");
            headers.add("interpretation");
            headers.add("notes");

            // Add instrument completion field if specified
            if (instrumentName != null && !instrumentName.isEmpty()) {
                headers.add(instrumentName + "_complete");
            }

            csv.append(String.join(",", headers)).append("\n");

            // Write data rows
            // Note: sampleIds are NotebookPageSample.id values (primary keys), not
            // sampleItemIds
            int recordNum = 1;
            LogEvent.logInfo(this.getClass().getName(), "generateREDCapExport",
                    "Processing " + sampleIds.size() + " samples for pageId " + pageId);

            for (Integer sampleId : sampleIds) {
                NotebookPageSample nps = notebookPageSampleService.get(sampleId);
                if (nps == null) {
                    LogEvent.logWarn(this.getClass().getName(), "generateREDCapExport",
                            "Sample not found: " + sampleId);
                    continue;
                }

                // Skip page verification for now - the frontend already filters by page
                // The sampleIds passed are already from the correct page

                List<String> values = new ArrayList<>();
                values.add(String.valueOf(recordNum++)); // record_id

                if (eventName != null && !eventName.isEmpty()) {
                    values.add("\"" + eventName + "\"");
                }

                // Get sample data
                String accessionNumber = getAccessionNumber(nps);
                Map<String, Object> data = nps.getData() != null ? nps.getData() : new HashMap<>();

                values.add("\"" + accessionNumber + "\"");
                values.add("\"" + getStringValue(data, "sampleType") + "\"");
                values.add("\"" + getStringValue(data, "collectionDate") + "\"");
                values.add("\"" + getStringValue(data, "validationStatus") + "\"");
                values.add("\"" + getStringValue(data, "testResults") + "\"");
                values.add("\"" + getStringValue(data, "interpretation") + "\"");
                values.add("\"" + getStringValue(data, "notes") + "\"");

                if (instrumentName != null && !instrumentName.isEmpty()) {
                    values.add("2"); // 2 = Complete in REDCap
                }

                csv.append(String.join(",", values)).append("\n");
                LogEvent.logDebug(this.getClass().getName(), "generateREDCapExport",
                        "Added row for sample " + sampleId + ": " + accessionNumber);
            }

            LogEvent.logInfo(this.getClass().getName(), "generateREDCapExport",
                    "Generated CSV with " + (recordNum - 1) + " data rows");

            return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "generateREDCapExport",
                    "Error generating REDCap export: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Generate REDCap export with bioanalytical-specific fields for bioequivalence
     * studies. Includes analytical method, sample type, bioequivalence statistics,
     * and regulatory compliance data.
     *
     * @param pageId        The notebook page ID
     * @param sampleIds     List of NotebookPageSample IDs to export
     * @param recordIdField The field to use as REDCap record_id (defaults to
     *                      "record_id")
     * @param eventName     Optional REDCap event name (for longitudinal studies)
     * @return UTF-8 encoded CSV bytes in REDCap format
     */
    @Transactional(readOnly = true)
    public byte[] generateBioanalyticalREDCapExport(Integer pageId, List<Integer> sampleIds, String recordIdField,
            String eventName) {
        if (pageId == null || sampleIds == null || sampleIds.isEmpty()) {
            return new byte[0];
        }

        try {
            StringBuilder csv = new StringBuilder();

            // REDCap standard fields
            String recordIdFieldName = recordIdField != null && !recordIdField.isEmpty() ? recordIdField : "record_id";

            // Build header with bioanalytical-specific columns
            List<String> headers = new ArrayList<>();
            headers.add(recordIdFieldName);
            if (eventName != null && !eventName.isEmpty()) {
                headers.add("redcap_event_name");
            }

            // Bioanalytical-specific data fields
            headers.add("analytical_method"); // e.g., HPLC-UV, LC-MS/MS
            headers.add("sample_type"); // e.g., Plasma, Urine
            headers.add("mean_accuracy"); // Expected accuracy %
            headers.add("sd"); // Standard deviation
            headers.add("cv"); // Coefficient of variation %
            headers.add("calibration_r_squared"); // R² value
            headers.add("westgard_status"); // PASS, FAIL, or WARNING
            headers.add("regulatory_status"); // COMPLIANT, NON_COMPLIANT, INSUFFICIENT_DATA
            headers.add("qa_approval_date"); // QA approval timestamp
            headers.add("bioequivalence_compliant"); // YES, NO, PENDING
            headers.add("notes"); // Additional comments

            csv.append(String.join(",", headers)).append("\n");

            // Write data rows
            int recordNum = 1;
            LogEvent.logInfo(this.getClass().getName(), "generateBioanalyticalREDCapExport",
                    "Processing " + sampleIds.size() + " samples for pageId " + pageId);

            for (Integer sampleId : sampleIds) {
                NotebookPageSample nps = notebookPageSampleService.get(sampleId);
                if (nps == null) {
                    LogEvent.logWarn(this.getClass().getName(), "generateBioanalyticalREDCapExport",
                            "Sample not found: " + sampleId);
                    continue;
                }

                List<String> values = new ArrayList<>();
                values.add(String.valueOf(recordNum++)); // record_id

                if (eventName != null && !eventName.isEmpty()) {
                    values.add("\"" + eventName + "\"");
                }

                // Get sample data
                Map<String, Object> data = nps.getData() != null ? nps.getData() : new HashMap<>();

                // Extract bioanalytical-specific fields with proper fallback chains
                String analyticalMethod = getStringValue(data, "analyticalMethod");
                if (analyticalMethod.isEmpty()) {
                    Map<String, Object> executionData = (Map<String, Object>) data.get("executionData");
                    if (executionData != null) {
                        analyticalMethod = getStringValue(executionData, "method");
                    }
                }

                String sampleType = getStringValue(data, "sampleType");

                // Extract bioequivalence statistics (priority: backend stats > local data)
                String meanAccuracy = "";
                String sd = "";
                String cv = "";
                String calibrationRSquared = "";
                String westgardStatus = "";
                String regulatoryStatus = "";
                String bioequivalenceCompliant = "";

                Map<String, Object> bioequivalenceStats = (Map<String, Object>) data.get("bioequivalenceStats");
                if (bioequivalenceStats != null) {
                    meanAccuracy = getStringValue(bioequivalenceStats, "meanAccuracy");
                    sd = getStringValue(bioequivalenceStats, "sd");
                    cv = getStringValue(bioequivalenceStats, "cv");
                    regulatoryStatus = getStringValue(bioequivalenceStats, "regulatoryStatus");
                }

                // Extract calibration data
                Map<String, Object> calibrationData = (Map<String, Object>) data.get("calibrationData");
                if (calibrationData != null) {
                    calibrationRSquared = getStringValue(calibrationData, "rSquared");
                }

                // Extract QC validation (Westgard) data
                Map<String, Object> qcValidation = (Map<String, Object>) data.get("qcValidation");
                if (qcValidation != null) {
                    westgardStatus = getStringValue(qcValidation, "westgardStatus");
                }

                // Extract QA approval date
                String qaApprovalDate = "";
                Map<String, Object> qaApprovalData = (Map<String, Object>) data.get("qaApprovalData");
                if (qaApprovalData != null) {
                    qaApprovalDate = getStringValue(qaApprovalData, "approvalDate");
                }

                // Determine bioequivalence compliance
                if ("COMPLIANT".equals(regulatoryStatus)) {
                    bioequivalenceCompliant = "YES";
                } else if ("NON_COMPLIANT".equals(regulatoryStatus)) {
                    bioequivalenceCompliant = "NO";
                } else {
                    bioequivalenceCompliant = "PENDING";
                }

                // Add all values to CSV row
                values.add("\"" + analyticalMethod + "\"");
                values.add("\"" + sampleType + "\"");
                values.add("\"" + meanAccuracy + "\"");
                values.add("\"" + sd + "\"");
                values.add("\"" + cv + "\"");
                values.add("\"" + calibrationRSquared + "\"");
                values.add("\"" + westgardStatus + "\"");
                values.add("\"" + regulatoryStatus + "\"");
                values.add("\"" + qaApprovalDate + "\"");
                values.add("\"" + bioequivalenceCompliant + "\"");
                values.add("\"" + getStringValue(data, "notes") + "\"");

                csv.append(String.join(",", values)).append("\n");
                LogEvent.logDebug(this.getClass().getName(), "generateBioanalyticalREDCapExport",
                        "Added row for sample " + sampleId + ": " + analyticalMethod + " - " + sampleType);
            }

            LogEvent.logInfo(this.getClass().getName(), "generateBioanalyticalREDCapExport",
                    "Generated CSV with " + (recordNum - 1) + " data rows");

            return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "generateBioanalyticalREDCapExport",
                    "Error generating bioanalytical REDCap export: " + e.getMessage());
            return new byte[0];
        }
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return "";
        }
        return value.toString().replace("\"", "'").replace(",", ";");
    }

    @Override
    @Transactional
    public Map<String, Object> parseAndApplyCsvResults(Integer pageId, byte[] csvContent, String machineType,
            String runId, String userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> unmatchedRows = new ArrayList<>();
        int matchedCount = 0;
        int updatedCount = 0;

        if (pageId == null || csvContent == null || csvContent.length == 0) {
            result.put("success", false);
            result.put("error", "Invalid parameters: pageId or CSV content is missing");
            return result;
        }

        try {
            String csvString = new String(csvContent, java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = csvString.split("\n");

            if (lines.length < 2) {
                result.put("success", false);
                result.put("error", "CSV must contain a header row and at least one data row");
                return result;
            }

            // Parse header row
            String[] headers = parseCsvLine(lines[0]);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Find key columns for matching - externalId is the primary matching key
            int externalIdCol = headerIndex.getOrDefault("externalid", -1);
            int accessionCol = headerIndex.getOrDefault("accessionnumber", -1);

            if (externalIdCol == -1 && accessionCol == -1) {
                result.put("success", false);
                result.put("error", "CSV must contain 'externalId' or 'accessionNumber' column for matching samples");
                return result;
            }

            // Get all samples for this page with their identifiers
            // Use getExternalId() helper to get the actual externalId from SampleItem
            List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(pageId);
            Map<String, NotebookPageSample> sampleByExternalId = new HashMap<>();
            Map<String, NotebookPageSample> sampleByAccession = new HashMap<>();

            for (NotebookPageSample nps : pageSamples) {
                // Primary: Build external ID lookup from SampleItem
                String externalId = getExternalId(nps);
                if (externalId != null && !externalId.isEmpty()) {
                    sampleByExternalId.put(externalId.toUpperCase(), nps);
                }
                // Fallback: Build accession number lookup
                String accession = getAccessionNumber(nps);
                if (accession != null && !accession.isEmpty()) {
                    sampleByAccession.put(accession.toUpperCase(), nps);
                }
            }

            // Process data rows
            for (int rowNum = 1; rowNum < lines.length; rowNum++) {
                String line = lines[rowNum].trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] values = parseCsvLine(line);
                if (values.length == 0) {
                    continue;
                }

                // Try to find matching sample - prioritize externalId matching
                NotebookPageSample matchedSample = null;
                String matchKey = "";

                // First: Try to match by externalId (primary matching key)
                if (externalIdCol >= 0 && externalIdCol < values.length && !values[externalIdCol].isEmpty()) {
                    matchKey = values[externalIdCol].trim().toUpperCase();
                    matchedSample = sampleByExternalId.get(matchKey);
                }

                // Fallback: Try to match by accessionNumber if externalId didn't match
                if (matchedSample == null && accessionCol >= 0 && accessionCol < values.length
                        && !values[accessionCol].isEmpty()) {
                    matchKey = values[accessionCol].trim().toUpperCase();
                    matchedSample = sampleByAccession.get(matchKey);
                }

                if (matchedSample == null) {
                    // Record unmatched row
                    Map<String, Object> unmatchedRow = new HashMap<>();
                    unmatchedRow.put("rowNumber", rowNum + 1);
                    unmatchedRow.put("accessionNumber",
                            accessionCol >= 0 && accessionCol < values.length ? values[accessionCol] : "");
                    unmatchedRow.put("externalId",
                            externalIdCol >= 0 && externalIdCol < values.length ? values[externalIdCol] : "");
                    unmatchedRows.add(unmatchedRow);
                    continue;
                }

                matchedCount++;

                // Build data map from CSV columns
                Map<String, Object> newData = matchedSample.getData() != null ? new HashMap<>(matchedSample.getData())
                        : new HashMap<>();

                // Map CSV columns to data fields
                mapCsvValueToData(newData, "testResult", headerIndex, values);
                mapCsvValueToData(newData, "ctValue", headerIndex, values);
                mapCsvValueToData(newData, "concentration", headerIndex, values);
                mapCsvValueToData(newData, "absorbance", headerIndex, values);
                mapCsvValueToData(newData, "runId", headerIndex, values);
                mapCsvValueToData(newData, "kitLot", headerIndex, values);
                mapCsvValueToData(newData, "operator", headerIndex, values);
                mapCsvValueToData(newData, "machineType", headerIndex, values);
                mapCsvValueToData(newData, "runCompleted", headerIndex, values);
                mapCsvValueToData(newData, "runIssues", headerIndex, values);
                mapCsvValueToData(newData, "notes", headerIndex, values);

                // Override with metadata from upload if not in CSV
                if ((newData.get("machineType") == null || newData.get("machineType").toString().isEmpty())
                        && machineType != null && !machineType.isEmpty()) {
                    newData.put("machineType", machineType);
                }
                if ((newData.get("runId") == null || newData.get("runId").toString().isEmpty()) && runId != null
                        && !runId.isEmpty()) {
                    newData.put("runId", runId);
                }

                // Mark as having raw data uploaded
                newData.put("rawDataUploaded", true);
                newData.put("rawDataUploadDate", java.time.LocalDateTime.now().toString());

                // Update sample
                matchedSample.setData(newData);
                matchedSample.setSysUserId(userId);
                notebookPageSampleService.update(matchedSample);
                updatedCount++;
            }

            result.put("success", true);
            result.put("matchedCount", matchedCount);
            result.put("updatedCount", updatedCount);
            result.put("unmatchedRows", unmatchedRows);
            result.put("totalRows", lines.length - 1);

            if (!unmatchedRows.isEmpty()) {
                result.put("warnings", "Some rows could not be matched to samples on this page");
            }

            LogEvent.logInfo(this.getClass().getName(), "parseAndApplyCsvResults",
                    "Processed CSV for page " + pageId + ": matched=" + matchedCount + ", updated=" + updatedCount
                            + ", unmatched=" + unmatchedRows.size());

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "parseAndApplyCsvResults",
                    "Error parsing CSV: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Error parsing CSV file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse a single CSV line, handling quoted values with commas.
     */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    /**
     * Map a CSV value to the data map if the column exists and has a value.
     */
    private void mapCsvValueToData(Map<String, Object> data, String fieldName, Map<String, Integer> headerIndex,
            String[] values) {
        Integer colIndex = headerIndex.get(fieldName.toLowerCase());
        if (colIndex != null && colIndex >= 0 && colIndex < values.length) {
            String value = values[colIndex].trim();
            // Remove surrounding quotes if present
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if (!value.isEmpty()) {
                data.put(fieldName, value);
            }
        }
    }
}
