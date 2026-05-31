package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleEventDTO;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleResponseDTO;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleStateDTO;
import org.openelisglobal.biorepository.controller.rest.dto.SampleTransferSummaryDTO;
import org.openelisglobal.biorepository.util.SampleTransferNotesHelper;
import org.openelisglobal.biorepository.util.SampleTransferNotesHelper.ParsedTransferNotes;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SampleLifecycleServiceImpl implements SampleLifecycleService {

    @Autowired
    private ChainOfCustodyService chainOfCustodyService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Autowired
    private SampleTransferService sampleTransferService;

    @Autowired
    private SampleRetrievalService sampleRetrievalService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Override
    public SampleLifecycleResponseDTO getBySampleItemId(Integer sampleItemId) {
        if (sampleItemId == null) {
            return null;
        }

        SampleItem sampleItem = sampleItemService.get(String.valueOf(sampleItemId));
        if (sampleItem == null) {
            return null;
        }

        BioSample bioSample = bioSampleService.getBySampleItemId(sampleItemId);
        List<ChainOfCustodyLog> custodyLogs = chainOfCustodyService.getBySampleItemId(sampleItemId);
        List<SampleStorageMovement> storageMovements = sampleStorageMovementDAO.findBySampleItemId(String.valueOf(sampleItemId));
        List<SampleTransferRequest> transferRequests = new ArrayList<>(
                safeCollection(sampleTransferService.getBySampleItemId(sampleItemId)));
        List<SampleLifecycleEventDTO> events = mergeLifecycleEvents(sampleItem, bioSample, custodyLogs, storageMovements,
                transferRequests);

        SampleLifecycleResponseDTO response = new SampleLifecycleResponseDTO();
        response.setSampleItemId(sampleItemId);
        response.setBioSampleId(bioSample != null ? bioSample.getId() : null);
        response.setSampleExternalId(sampleItem.getExternalId());
        response.setAccessionNumber(sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber() : null);
        response.setCurrentState(buildCurrentState(sampleItem, bioSample, custodyLogs, events));
        response.setTransferSummary(buildTransferSummary(sampleItem, bioSample, transferRequests));
        response.setEvents(events);
        return response;
    }

    @Override
    public SampleLifecycleResponseDTO getByBioSampleId(Integer bioSampleId) {
        if (bioSampleId == null) {
            return null;
        }

        BioSample bioSample = bioSampleService.get(bioSampleId);
        if (bioSample == null || bioSample.getSampleItem() == null || bioSample.getSampleItem().getId() == null) {
            return null;
        }

        return getBySampleItemId(Integer.valueOf(bioSample.getSampleItem().getId()));
    }

    @Override
    public Map<String, Object> search(String sampleExternalId, CustodyAction action, Timestamp startDate,
            Timestamp endDate, int page, int pageSize) {
        Set<Integer> sampleItemIds = resolveSampleItemIds(sampleExternalId);
        List<SampleLifecycleEventDTO> events = new ArrayList<>();

        if (!sampleItemIds.isEmpty()) {
            for (Integer sampleItemId : sampleItemIds) {
                SampleLifecycleResponseDTO lifecycle = getBySampleItemId(sampleItemId);
                if (lifecycle != null && lifecycle.getEvents() != null) {
                    events.addAll(lifecycle.getEvents());
                }
            }
        } else {
            List<ChainOfCustodyLog> logs = chainOfCustodyService.searchCustodyLogs(sampleExternalId, action, null,
                    startDate, endDate, 0, Math.max(pageSize * 3, 200));
            Set<Integer> idsFromLogs = logs.stream().map(ChainOfCustodyLog::getSampleItemId)
                    .filter(id -> id != null).collect(Collectors.toCollection(LinkedHashSet::new));
            for (Integer sampleItemId : idsFromLogs) {
                SampleLifecycleResponseDTO lifecycle = getBySampleItemId(sampleItemId);
                if (lifecycle != null && lifecycle.getEvents() != null) {
                    events.addAll(lifecycle.getEvents());
                }
            }
        }

        List<SampleLifecycleEventDTO> filtered = events.stream().filter(event -> matchesAction(event, action))
                .filter(event -> matchesDateRange(event, startDate, endDate)).sorted(searchComparator())
                .collect(Collectors.toList());

        int safePage = Math.max(page, 0);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min(safePage * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        return Map.of("data", filtered.subList(fromIndex, toIndex), "page", safePage, "pageSize", safePageSize,
                "totalCount", filtered.size(), "totalPages",
                filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / safePageSize));
    }

    private SampleLifecycleStateDTO buildCurrentState(SampleItem sampleItem, BioSample bioSample,
            List<ChainOfCustodyLog> custodyLogs, List<SampleLifecycleEventDTO> lifecycleEvents) {
        SampleLifecycleStateDTO state = new SampleLifecycleStateDTO();
        state.setWorkflowStatus(bioSample != null && bioSample.getWorkflowStatus() != null
                ? bioSample.getWorkflowStatus().name()
                : null);

        Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItem.getId());
        String locationPath = extractLocationPath(location);
        if (locationPath == null || locationPath.isBlank()) {
            locationPath = resolveLastKnownStorageLocation(lifecycleEvents);
        }
        state.setLastKnownStorageLocation(locationPath);
        state.setPhysicallyInStorage(bioSample != null && bioSample.getWorkflowStatus() == WorkflowStatus.STORED
                && locationPath != null && !locationPath.isBlank());

        if (custodyLogs != null && !custodyLogs.isEmpty()) {
            ChainOfCustodyLog latest = custodyLogs.get(custodyLogs.size() - 1);
            if (latest.getToCustodian() != null) {
                state.setCurrentCustodian(latest.getToCustodian().getNameForDisplay());
            } else if (latest.getFromCustodian() != null) {
                state.setCurrentCustodian(latest.getFromCustodian().getNameForDisplay());
            }
            state.setAwaitingRestorage(latest.getCustodyAction() == CustodyAction.RETURN_RECEIVED
                    || latest.getCustodyAction() == CustodyAction.RETURN_INSPECTED);
        }

        Integer sampleItemId = Integer.valueOf(sampleItem.getId());
        state.setActiveTransferRequestId(safeCollection(sampleTransferService.getBySampleItemId(sampleItemId)).stream()
                .filter(request -> request.getStatus() == SampleTransferRequest.TransferStatus.PENDING)
                .map(SampleTransferRequest::getId).findFirst().orElse(null));

        if (bioSample != null) {
            state.setActiveRetrievalRequestId(safeCollection(sampleRetrievalService.getByBioSampleId(bioSample.getId()))
                    .stream()
                    .filter(request -> request.getStatus() != SampleRetrievalRequest.RequestStatus.COMPLETED
                            && request.getStatus() != SampleRetrievalRequest.RequestStatus.CANCELLED
                            && request.getStatus() != SampleRetrievalRequest.RequestStatus.REJECTED)
                    .map(SampleRetrievalRequest::getId).findFirst().orElse(null));
        }

        return state;
    }

    private List<SampleLifecycleEventDTO> mergeLifecycleEvents(SampleItem sampleItem, BioSample bioSample,
            List<ChainOfCustodyLog> custodyLogs, List<SampleStorageMovement> storageMovements,
            List<SampleTransferRequest> transferRequests) {
        List<SampleLifecycleEventDTO> events = new ArrayList<>();
        Set<String> custodyMovementKeys = new HashSet<>();
        Set<String> rejectionEventKeys = new HashSet<>();

        for (ChainOfCustodyLog log : safeCollection(custodyLogs)) {
            events.add(mapCustodyEvent(sampleItem, bioSample, log));
            if ("SampleStorageMovement".equals(log.getSourceRecordType()) && log.getSourceRecordId() != null) {
                custodyMovementKeys.add(log.getSourceRecordType() + ":" + log.getSourceRecordId());
            }
            if ("SampleTransferItem".equals(log.getSourceRecordType()) && log.getSourceRecordId() != null
                    && log.getCustodyAction() != null && log.getCustodyAction().name().contains("REJECT")) {
                rejectionEventKeys.add("SampleTransferItem:" + log.getSourceRecordId());
            }
        }

        for (SampleStorageMovement movement : safeCollection(storageMovements)) {
            String movementKey = "SampleStorageMovement:" + movement.getId();
            if (!custodyMovementKeys.contains(movementKey)) {
                events.add(mapMovementEvent(sampleItem, bioSample, movement));
            }
        }

        for (SampleTransferRequest request : safeCollection(transferRequests)) {
            for (SampleTransferItem item : safeCollection(request.getItems())) {
                if (item.getSampleItem() == null || item.getSampleItem().getId() == null
                        || !item.getSampleItem().getId().equals(sampleItem.getId())) {
                    continue;
                }
                if (item.isRejected() && item.getRejectionReason() != null && !item.getRejectionReason().isBlank()) {
                    String rejectionKey = "SampleTransferItem:" + item.getId();
                    if (!rejectionEventKeys.contains(rejectionKey)) {
                        events.add(mapSyntheticRejectionEvent(sampleItem, bioSample, request, item));
                    }
                }
            }
        }

        return events.stream().sorted(lifecycleComparator()).collect(Collectors.toList());
    }

    private SampleTransferSummaryDTO buildTransferSummary(SampleItem sampleItem, BioSample bioSample,
            List<SampleTransferRequest> transferRequests) {
        TransferContext context = findLatestTransferContext(sampleItem, transferRequests);
        if (context == null) {
            return null;
        }

        SampleTransferRequest request = context.request();
        SampleTransferItem item = context.item();
        ParsedTransferNotes parsedNotes = SampleTransferNotesHelper.parseStructuredNotes(request.getRequestNotes());

        SampleTransferSummaryDTO summary = new SampleTransferSummaryDTO();
        summary.setTransferRequestId(request.getId());
        summary.setTransferItemId(item.getId());
        summary.setSourceLab(request.getSourceLab());
        summary.setDestinationLab(request.getDestinationLab());
        summary.setTransferStatus(request.getStatus() != null ? request.getStatus().name() : null);
        summary.setItemStatus(item.getStatus() != null ? item.getStatus().name() : null);
        summary.setProjectName(parsedNotes.getProjectName());
        summary.setTransferReason(parsedNotes.getTransferReason());
        summary.setRequestNotes(request.getRequestNotes());
        if (request.getRequestedBy() != null) {
            summary.setRequestedByName(request.getRequestedBy().getNameForDisplay());
        }
        summary.setRequestedTimestamp(request.getRequestedTimestamp());
        if (request.getProcessedBy() != null) {
            summary.setProcessedByName(request.getProcessedBy().getNameForDisplay());
        }
        summary.setProcessedTimestamp(request.getProcessedTimestamp());
        summary.setRejectionReason(item.getRejectionReason());

        summary.setSampleExternalId(sampleItem.getExternalId());
        summary.setAccessionNumber(
                sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber() : null);
        if (sampleItem.getTypeOfSample() != null) {
            summary.setSampleType(sampleItem.getTypeOfSample().getDescription());
        }
        summary.setQuantity(sampleItem.getQuantity());
        summary.setCollectionDate(sampleItem.getCollectionDate());
        summary.setSampleCondition(item.getSampleCondition());
        summary.setPreservationMedium(item.getPreservationMedium());
        if (item.getUnitOfMeasure() != null && !item.getUnitOfMeasure().isBlank()) {
            summary.setUnitOfMeasure(item.getUnitOfMeasure());
        } else if (sampleItem.getUnitOfMeasureName() != null && !sampleItem.getUnitOfMeasureName().isBlank()) {
            summary.setUnitOfMeasure(sampleItem.getUnitOfMeasureName());
        }

        if (bioSample != null) {
            if (bioSample.getBiosafetyLevel() != null) {
                summary.setBiosafetyLevel(bioSample.getBiosafetyLevel().name());
            }
            summary.setEthicsApprovalRef(bioSample.getEthicsApprovalRef());
            summary.setOriginLab(bioSample.getOriginLab());
            summary.setPrincipalInvestigator(bioSample.getPrincipalInvestigator());
            summary.setProjectId(bioSample.getProjectId());
        }

        return summary;
    }

    private TransferContext findLatestTransferContext(SampleItem sampleItem, List<SampleTransferRequest> transferRequests) {
        if (sampleItem == null || sampleItem.getId() == null || transferRequests.isEmpty()) {
            return null;
        }

        SampleTransferRequest latestRequest = null;
        SampleTransferItem latestItem = null;
        Timestamp latestTimestamp = null;

        for (SampleTransferRequest request : transferRequests) {
            for (SampleTransferItem item : safeCollection(request.getItems())) {
                if (item.getSampleItem() == null || item.getSampleItem().getId() == null
                        || !item.getSampleItem().getId().equals(sampleItem.getId())) {
                    continue;
                }
                Timestamp candidate = request.getRequestedTimestamp();
                if (latestTimestamp == null || (candidate != null && candidate.after(latestTimestamp))) {
                    latestTimestamp = candidate;
                    latestRequest = request;
                    latestItem = item;
                }
            }
        }

        if (latestRequest == null || latestItem == null) {
            return null;
        }
        return new TransferContext(latestRequest, latestItem);
    }

    private SampleLifecycleEventDTO mapSyntheticRejectionEvent(SampleItem sampleItem, BioSample bioSample,
            SampleTransferRequest request, SampleTransferItem item) {
        SampleLifecycleEventDTO event = baseEvent(sampleItem, bioSample);
        event.setEventType("TRANSFER_REJECTED");
        event.setCustodyAction("TRANSFER_REJECTED");
        Timestamp eventTimestamp = request.getProcessedTimestamp() != null ? request.getProcessedTimestamp()
                : request.getRequestedTimestamp();
        event.setEventTimestamp(eventTimestamp);
        event.setActionTimestamp(eventTimestamp);
        event.setStage("TRANSFER");
        if (request.getProcessedBy() != null) {
            event.setActorUserId(request.getProcessedBy().getId());
            event.setActorDisplayName(request.getProcessedBy().getNameForDisplay());
        }
        event.setFromLocationDisplay(request.getSourceLab());
        event.setToLocationDisplay(request.getDestinationLab());
        event.setSourceRecordType("SampleTransferItem");
        event.setSourceRecordId(item.getId());
        event.setNotes("Transfer rejected: " + item.getRejectionReason());
        return event;
    }

    private record TransferContext(SampleTransferRequest request, SampleTransferItem item) {
    }

    private SampleLifecycleEventDTO mapCustodyEvent(SampleItem sampleItem, BioSample bioSample, ChainOfCustodyLog log) {
        SampleLifecycleEventDTO event = baseEvent(sampleItem, bioSample);
        event.setEventType(log.getCustodyAction().name());
        event.setCustodyAction(log.getCustodyAction().name());
        event.setEventTimestamp(log.getActionTimestamp());
        event.setActionTimestamp(log.getActionTimestamp());
        event.setStage(mapStage(log.getCustodyAction()));
        SystemUser actor = log.getToCustodian() != null ? log.getToCustodian() : log.getFromCustodian();
        if (actor != null) {
            event.setActorUserId(actor.getId());
            event.setActorDisplayName(actor.getNameForDisplay());
        }
        event.setFromWorkflowStatus(log.getWorkflowStatusBefore());
        event.setToWorkflowStatus(log.getWorkflowStatusAfter());
        event.setFromLocationDisplay(log.getFromLocation());
        event.setToLocationDisplay(log.getToLocation());
        event.setStorageCoordinates(log.getStorageCoordinates());
        event.setTemperature(log.getTemperature());
        event.setSourceRecordType(log.getSourceRecordType() != null ? log.getSourceRecordType() : "ChainOfCustodyLog");
        event.setSourceRecordId(log.getSourceRecordId() != null ? log.getSourceRecordId() : log.getId());
        event.setNotes(log.getNotes());
        return event;
    }

    private SampleLifecycleEventDTO mapMovementEvent(SampleItem sampleItem, BioSample bioSample,
            SampleStorageMovement movement) {
        SampleLifecycleEventDTO event = baseEvent(sampleItem, bioSample);
        boolean initialAssignment = movement.getPreviousLocationId() == null && movement.getPreviousLocationType() == null;
        String eventType = initialAssignment ? CustodyAction.STORAGE_ASSIGNED.name() : CustodyAction.STORAGE_MOVED.name();
        event.setEventType(eventType);
        event.setCustodyAction(eventType);
        event.setEventTimestamp(movement.getMovementDate());
        event.setActionTimestamp(movement.getMovementDate());
        event.setStage("STORAGE");
        if (movement.getMovedByUserId() != null) {
            SystemUser actor = systemUserService.get(String.valueOf(movement.getMovedByUserId()));
            if (actor != null) {
                event.setActorUserId(actor.getId());
                event.setActorDisplayName(actor.getNameForDisplay());
            } else {
                event.setActorUserId(String.valueOf(movement.getMovedByUserId()));
            }
        }
        event.setFromLocationDisplay(buildPathFromLocation(movement.getPreviousLocationId(), movement.getPreviousLocationType(),
                movement.getPreviousPositionCoordinate()));
        event.setToLocationDisplay(buildPathFromLocation(movement.getNewLocationId(), movement.getNewLocationType(),
                movement.getNewPositionCoordinate()));
        event.setStorageCoordinates(movement.getNewPositionCoordinate());
        event.setSourceRecordType("SampleStorageMovement");
        event.setSourceRecordId(movement.getId());
        event.setNotes(movement.getReason());
        return event;
    }

    private SampleLifecycleEventDTO baseEvent(SampleItem sampleItem, BioSample bioSample) {
        SampleLifecycleEventDTO event = new SampleLifecycleEventDTO();
        event.setSampleItemId(sampleItem != null && sampleItem.getId() != null ? Integer.valueOf(sampleItem.getId()) : null);
        event.setBioSampleId(bioSample != null ? bioSample.getId() : null);
        event.setSampleExternalId(sampleItem != null ? sampleItem.getExternalId() : null);
        event.setAccessionNumber(sampleItem != null && sampleItem.getSample() != null
                ? sampleItem.getSample().getAccessionNumber()
                : null);
        return event;
    }

    private Comparator<SampleLifecycleEventDTO> lifecycleComparator() {
        return Comparator.comparing(SampleLifecycleEventDTO::getEventTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SampleLifecycleEventDTO::getSourceRecordType, Comparator.nullsLast(String::compareTo))
                .thenComparing(SampleLifecycleEventDTO::getSourceRecordId, Comparator.nullsLast(Integer::compareTo));
    }

    private Comparator<SampleLifecycleEventDTO> searchComparator() {
        return Comparator.comparing(SampleLifecycleEventDTO::getEventTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SampleLifecycleEventDTO::getSourceRecordType, Comparator.nullsLast(String::compareTo))
                .thenComparing(SampleLifecycleEventDTO::getSourceRecordId, Comparator.nullsLast(Integer::compareTo));
    }

    private boolean matchesAction(SampleLifecycleEventDTO event, CustodyAction action) {
        return action == null || (event.getEventType() != null && event.getEventType().equals(action.name()));
    }

    private boolean matchesDateRange(SampleLifecycleEventDTO event, Timestamp startDate, Timestamp endDate) {
        Timestamp eventTimestamp = event.getEventTimestamp();
        if (eventTimestamp == null) {
            return false;
        }
        if (startDate != null && eventTimestamp.before(startDate)) {
            return false;
        }
        if (endDate != null && eventTimestamp.after(endDate)) {
            return false;
        }
        return true;
    }

    private Set<Integer> resolveSampleItemIds(String searchValue) {
        Set<Integer> sampleItemIds = new LinkedHashSet<>();
        if (searchValue == null || searchValue.trim().isEmpty()) {
            return sampleItemIds;
        }

        String normalized = searchValue.trim();
        try {
            sampleItemIds.add(Integer.valueOf(normalized));
        } catch (NumberFormatException e) {
            // Ignore non-numeric lookup.
        }

        for (SampleItem sampleItem : safeCollection(sampleItemService.getSampleItemsByExternalID(normalized))) {
            if (sampleItem != null && sampleItem.getId() != null) {
                sampleItemIds.add(Integer.valueOf(sampleItem.getId()));
            }
        }

        Sample sample = sampleService.getSampleByAccessionNumber(normalized);
        if (sample != null && sample.getId() != null) {
            for (SampleItem sampleItem : safeCollection(sampleItemService.getSampleItemsBySampleId(sample.getId()))) {
                if (sampleItem != null && sampleItem.getId() != null) {
                    sampleItemIds.add(Integer.valueOf(sampleItem.getId()));
                }
            }
        }

        return sampleItemIds;
    }

    private <T> Collection<T> safeCollection(Collection<T> values) {
        return values != null ? values : List.of();
    }

    private String mapStage(CustodyAction action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
        case TRANSFER_INITIATED, TRANSFER_RECEIVED -> "TRANSFER";
        case STORAGE_ASSIGNED, STORAGE_MOVED -> "STORAGE";
        case CHECKOUT_REQUESTED, CHECKOUT_APPROVED, CHECKOUT_RETRIEVED, CHECKOUT_RELEASED -> "RETRIEVAL";
        case RETURN_RECEIVED, RETURN_INSPECTED, RETURN_STORED -> "RETURN";
        case DISPOSED -> "DISPOSAL";
        default -> "OTHER";
        };
    }

    private String extractLocationPath(Map<String, Object> location) {
        if (location == null || location.isEmpty()) {
            return null;
        }
        Object hierarchicalPath = location.get("hierarchicalPath");
        if (hierarchicalPath instanceof String path && !path.isBlank()) {
            return path;
        }
        Object simplePath = location.get("location");
        if (simplePath instanceof String path && !path.isBlank()) {
            return path;
        }
        return null;
    }

    private String resolveLastKnownStorageLocation(List<SampleLifecycleEventDTO> lifecycleEvents) {
        if (lifecycleEvents == null || lifecycleEvents.isEmpty()) {
            return null;
        }

        for (int i = lifecycleEvents.size() - 1; i >= 0; i--) {
            SampleLifecycleEventDTO event = lifecycleEvents.get(i);
            if (event == null) {
                continue;
            }

            if (event.getToLocationDisplay() != null && !event.getToLocationDisplay().isBlank()
                    && ("STORAGE".equals(event.getStage()) || CustodyAction.RETURN_STORED.name().equals(event.getEventType())
                            || CustodyAction.RETURN_RECEIVED.name().equals(event.getEventType())
                            || CustodyAction.RETURN_INSPECTED.name().equals(event.getEventType()))) {
                return event.getToLocationDisplay();
            }

            if (event.getStorageCoordinates() != null && !event.getStorageCoordinates().isBlank()) {
                return event.getStorageCoordinates();
            }
        }

        return null;
    }

    private String buildPathFromLocation(Integer locationId, String locationType, String positionCoordinate) {
        if (locationId == null || locationType == null || locationType.isBlank()) {
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

        if (locationEntity == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if (locationEntity instanceof StorageBox box) {
            if (box.getParentRack() != null && box.getParentRack().getParentShelf() != null
                    && box.getParentRack().getParentShelf().getParentDevice() != null
                    && box.getParentRack().getParentShelf().getParentDevice().getParentRoom() != null) {
                parts.add(box.getParentRack().getParentShelf().getParentDevice().getParentRoom().getName());
            }
            if (box.getParentRack() != null && box.getParentRack().getParentShelf() != null
                    && box.getParentRack().getParentShelf().getParentDevice() != null) {
                parts.add(box.getParentRack().getParentShelf().getParentDevice().getName());
            }
            if (box.getParentRack() != null && box.getParentRack().getParentShelf() != null) {
                parts.add(box.getParentRack().getParentShelf().getLabel());
            }
            if (box.getParentRack() != null) {
                parts.add(box.getParentRack().getLabel());
            }
            parts.add(box.getLabel());
        } else if (locationEntity instanceof StorageRack rack) {
            if (rack.getParentShelf() != null && rack.getParentShelf().getParentDevice() != null
                    && rack.getParentShelf().getParentDevice().getParentRoom() != null) {
                parts.add(rack.getParentShelf().getParentDevice().getParentRoom().getName());
            }
            if (rack.getParentShelf() != null && rack.getParentShelf().getParentDevice() != null) {
                parts.add(rack.getParentShelf().getParentDevice().getName());
            }
            if (rack.getParentShelf() != null) {
                parts.add(rack.getParentShelf().getLabel());
            }
            parts.add(rack.getLabel());
        } else if (locationEntity instanceof StorageShelf shelf) {
            if (shelf.getParentDevice() != null && shelf.getParentDevice().getParentRoom() != null) {
                parts.add(shelf.getParentDevice().getParentRoom().getName());
            }
            if (shelf.getParentDevice() != null) {
                parts.add(shelf.getParentDevice().getName());
            }
            parts.add(shelf.getLabel());
        } else if (locationEntity instanceof StorageDevice device) {
            if (device.getParentRoom() != null) {
                parts.add(device.getParentRoom().getName());
            }
            parts.add(device.getName());
        } else if (locationEntity instanceof StorageRoom room) {
            parts.add(room.getName());
        }

        if (positionCoordinate != null && !positionCoordinate.isBlank()
                && (parts.isEmpty() || !positionCoordinate.equals(parts.get(parts.size() - 1)))) {
            parts.add(positionCoordinate);
        }

        return parts.stream().filter(part -> part != null && !part.isBlank()).collect(Collectors.joining(" > "));
    }
}
