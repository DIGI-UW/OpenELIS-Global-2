package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleLifecycleEventDTO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BioSampleLifecycleServiceImpl implements BioSampleLifecycleService {

    public static final String TYPE_STORED = "STORED";
    public static final String TYPE_RETRIEVED = "RETRIEVED";
    public static final String TYPE_TRANSFERRED = "TRANSFERRED";
    public static final String TYPE_RETURNED = "RETURNED";

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleTransferService sampleTransferService;

    @Autowired
    private SampleRetrievalService sampleRetrievalService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private ChainOfCustodyService chainOfCustodyService;

    @Autowired
    private SystemUserService systemUserService;

    @Override
    @Transactional(readOnly = true)
    public List<BioSampleLifecycleEventDTO> buildLifecycleEvents(Integer bioSampleId) {
        if (bioSampleId == null) {
            return List.of();
        }
        BioSample bioSample = bioSampleService.get(bioSampleId);
        if (bioSample == null || bioSample.getSampleItem() == null) {
            return List.of();
        }
        SampleItem sampleItem = bioSample.getSampleItem();
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());

        List<MutableEvent> buffer = new ArrayList<>();
        buffer.addAll(buildTransferEvents(bioSampleId, sampleItemId));
        buffer.addAll(buildStorageEvents(sampleItem));
        buffer.addAll(buildRetrievalEvents(bioSampleId));
        buffer.addAll(buildCustodyLogEvents(sampleItemId));

        buffer.sort(Comparator.comparing(MutableEvent::sortKey, Comparator.nullsLast(Comparator.naturalOrder())));

        return buffer.stream().map(MutableEvent::toDto).filter(new java.util.function.Predicate<>() {
            private final java.util.Set<String> seen = new java.util.HashSet<>();

            @Override
            public boolean test(BioSampleLifecycleEventDTO event) {
                return seen.add(dedupKey(event));
            }
        }).collect(Collectors.toList());
    }

    private List<MutableEvent> buildTransferEvents(Integer bioSampleId, Integer sampleItemId) {
        List<MutableEvent> out = new ArrayList<>();
        List<SampleTransferRequest> requests = sampleTransferService.getBySampleItemId(sampleItemId);
        if (requests == null) {
            return out;
        }
        for (SampleTransferRequest request : requests) {
            if (request.getItems() == null) {
                continue;
            }
            for (SampleTransferItem item : request.getItems()) {
                if (!item.isAccepted() || item.getBioSample() == null
                        || !Objects.equals(item.getBioSample().getId(), bioSampleId)) {
                    continue;
                }
                Timestamp ts = request.getProcessedTimestamp() != null ? request.getProcessedTimestamp()
                        : request.getRequestedTimestamp();
                if (ts == null) {
                    continue;
                }
                MutableEvent e = new MutableEvent(ts);
                e.dto.setEventType(TYPE_TRANSFERRED);
                e.dto.setOccurredAt(toIso(ts));
                e.dto.setActor(userLabel(request.getProcessedBy() != null ? request.getProcessedBy()
                        : request.getRequestedBy()));
                e.dto.setSourceLocation(request.getSourceLab());
                e.dto.setDestinationLocation(request.getDestinationLab());
                if (request.getRequestNotes() != null && !request.getRequestNotes().isBlank()) {
                    e.dto.setStatusOrNotes(request.getRequestNotes().trim());
                }
                out.add(e);
            }
        }
        return out;
    }

    private List<MutableEvent> buildCustodyLogEvents(Integer sampleItemId) {
        List<MutableEvent> out = new ArrayList<>();
        List<ChainOfCustodyLog> logs = chainOfCustodyService.getBySampleItemId(sampleItemId);
        if (logs == null) {
            return out;
        }

        for (ChainOfCustodyLog log : logs) {
            if (log == null || log.getActionTimestamp() == null || log.getCustodyAction() == null) {
                continue;
            }
            String eventType = mapCustodyAction(log.getCustodyAction());
            if (eventType == null) {
                continue;
            }

            MutableEvent e = new MutableEvent(log.getActionTimestamp());
            e.dto.setEventType(eventType);
            e.dto.setOccurredAt(toIso(log.getActionTimestamp()));
            e.dto.setActor(userLabel(log.getToCustodian() != null ? log.getToCustodian() : log.getFromCustodian()));
            e.dto.setSourceLocation(log.getFromLocation());
            e.dto.setDestinationLocation(appendCoordinate(log.getToLocation(), log.getStorageCoordinates()));
            e.dto.setStatusOrNotes(formatCustodyNotes(log));
            out.add(e);
        }
        return out;
    }

    private static String mapCustodyAction(CustodyAction action) {
        switch (action) {
        case TRANSFER_INITIATED:
        case TRANSFER_RECEIVED:
            return TYPE_TRANSFERRED;
        case CHECKOUT_RETRIEVED:
        case CHECKOUT_RELEASED:
            return TYPE_RETRIEVED;
        case RETURN_INITIATED:
        case RETURN_RECEIVED:
        case RETURN_INSPECTED:
            return TYPE_RETURNED;
        case RETURN_STORED:
            return TYPE_STORED;
        default:
            return null;
        }
    }

    private static String formatCustodyNotes(ChainOfCustodyLog log) {
        StringBuilder notes = new StringBuilder(formatCustodyAction(log.getCustodyAction()));
        if (log.getNotes() != null && !log.getNotes().isBlank()) {
            notes.append(" — ").append(log.getNotes().trim());
        }
        if (log.getTemperature() != null) {
            notes.append(" | ").append(log.getTemperature()).append(" C");
        }
        return notes.toString();
    }

    private static String formatCustodyAction(CustodyAction action) {
        String[] parts = action.name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return label.toString();
    }

    private static String appendCoordinate(String location, String coordinate) {
        if (coordinate == null || coordinate.isBlank()) {
            return location;
        }
        if (location == null || location.isBlank()) {
            return coordinate.trim();
        }
        return location.trim() + " · " + coordinate.trim();
    }

    private List<MutableEvent> buildStorageEvents(SampleItem sampleItem) {
        List<MutableEvent> out = new ArrayList<>();
        List<SampleStorageMovement> movements = sampleStorageService.getSampleStorageMovementsBySampleItem(sampleItem);
        if (movements == null || movements.isEmpty()) {
            return out;
        }
        // DAO returns newest-first; process oldest-first for stable narrative
        List<SampleStorageMovement> chronological = new ArrayList<>(movements);
        Collections.reverse(chronological);

        for (SampleStorageMovement m : chronological) {
            if (m.getNewLocationId() == null) {
                continue;
            }
            Timestamp ts = m.getMovementDate();
            if (ts == null) {
                continue;
            }
            MutableEvent e = new MutableEvent(ts);
            e.dto.setEventType(TYPE_STORED);
            e.dto.setOccurredAt(toIso(ts));
            e.dto.setActor(userLabelForNumericId(m.getMovedByUserId()));
            e.dto.setSourceLocation(formatMovementEndpoint(m.getPreviousLocationType(), m.getPreviousLocationId(),
                    m.getPreviousPositionCoordinate()));
            e.dto.setDestinationLocation(formatMovementEndpoint(m.getNewLocationType(), m.getNewLocationId(),
                    m.getNewPositionCoordinate()));
            if (m.getReason() != null && !m.getReason().isBlank()) {
                e.dto.setStatusOrNotes(m.getReason().trim());
            }
            out.add(e);
        }
        return out;
    }

    private List<MutableEvent> buildRetrievalEvents(Integer bioSampleId) {
        List<MutableEvent> out = new ArrayList<>();
        List<SampleRetrievalRequest> requests = sampleRetrievalService.getByBioSampleId(bioSampleId);
        if (requests == null) {
            return out;
        }
        for (SampleRetrievalRequest request : requests) {
            if (request.getItems() == null) {
                continue;
            }
            for (SampleRetrievalItem item : request.getItems()) {
                if (item.getBioSample() == null || !Objects.equals(item.getBioSample().getId(), bioSampleId)) {
                    continue;
                }
                if (item.getRetrievedTimestamp() != null) {
                    Timestamp ts = item.getRetrievedTimestamp();
                    MutableEvent e = new MutableEvent(ts);
                    e.dto.setEventType(TYPE_RETRIEVED);
                    e.dto.setOccurredAt(toIso(ts));
                    e.dto.setActor(userLabel(item.getRetrievedBy()));
                    e.dto.setSourceLocation(null);
                    e.dto.setDestinationLocation(request.getDestinationDetails());
                    StringBuilder notes = new StringBuilder();
                    if (request.getRequestPurpose() != null && !request.getRequestPurpose().isBlank()) {
                        notes.append(request.getRequestPurpose().trim());
                    }
                    if (item.getConditionAtRelease() != null && !item.getConditionAtRelease().isBlank()) {
                        if (notes.length() > 0) {
                            notes.append(" — ");
                        }
                        notes.append(item.getConditionAtRelease().trim());
                    }
                    if (item.getConditionNotes() != null && !item.getConditionNotes().isBlank()) {
                        if (notes.length() > 0) {
                            notes.append(" — ");
                        }
                        notes.append(item.getConditionNotes().trim());
                    }
                    if (request.getRequestNumber() != null) {
                        if (notes.length() > 0) {
                            notes.append(" | ");
                        }
                        notes.append("Request ").append(request.getRequestNumber());
                    }
                    if (notes.length() > 0) {
                        e.dto.setStatusOrNotes(notes.toString());
                    }
                    out.add(e);
                }
                if (item.getReturnedTimestamp() != null) {
                    Timestamp ts = item.getReturnedTimestamp();
                    MutableEvent e = new MutableEvent(ts);
                    e.dto.setEventType(TYPE_RETURNED);
                    e.dto.setOccurredAt(toIso(ts));
                    e.dto.setActor(userLabel(item.getReturnedBy()));
                    e.dto.setSourceLocation(null);
                    e.dto.setDestinationLocation(null);
                    StringBuilder notes = new StringBuilder();
                    notes.append(String.valueOf(item.getStatus()));
                    if (item.getReturnedCondition() != null && !item.getReturnedCondition().isBlank()) {
                        notes.append(" — ").append(item.getReturnedCondition().trim());
                    }
                    if (item.getReturnNotes() != null && !item.getReturnNotes().isBlank()) {
                        notes.append(" — ").append(item.getReturnNotes().trim());
                    }
                    e.dto.setStatusOrNotes(notes.toString());
                    out.add(e);
                }
            }
        }
        return out;
    }

    private static String formatMovementEndpoint(String locationType, Integer locationId, String coordinate) {
        if (locationType == null && locationId == null && (coordinate == null || coordinate.isBlank())) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (locationType != null && !locationType.isBlank()) {
            sb.append(locationType.trim());
        }
        if (locationId != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append('#').append(locationId);
        }
        if (coordinate != null && !coordinate.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(coordinate.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String userLabelForNumericId(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            SystemUser u = systemUserService.get(String.valueOf(userId));
            return userLabel(u);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String userLabel(SystemUser user) {
        if (user == null) {
            return null;
        }
        StringBuilder name = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            name.append(user.getFirstName().trim());
        }
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(user.getLastName().trim());
        }
        if (name.length() > 0) {
            return name.toString();
        }
        if (user.getLoginName() != null && !user.getLoginName().isBlank()) {
            return user.getLoginName().trim();
        }
        return user.getId();
    }

    private static String toIso(Timestamp ts) {
        return Instant.ofEpochMilli(ts.getTime()).toString();
    }

    private static String dedupKey(BioSampleLifecycleEventDTO event) {
        return String.join("|", safe(event.getEventType()), safe(event.getOccurredAt()), safe(event.getActor()),
                safe(event.getSourceLocation()), safe(event.getDestinationLocation()), safe(event.getStatusOrNotes()));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class MutableEvent {
        private final long sortKey;
        private final BioSampleLifecycleEventDTO dto = new BioSampleLifecycleEventDTO();

        MutableEvent(Timestamp ts) {
            this.sortKey = ts.getTime();
        }

        long sortKey() {
            return sortKey;
        }

        BioSampleLifecycleEventDTO toDto() {
            return dto;
        }
    }
}
