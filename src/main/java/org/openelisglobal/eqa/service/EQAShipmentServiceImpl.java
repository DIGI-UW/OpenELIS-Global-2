package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAProgramEnrollmentDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.service.ShipmentService;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See {@link EQAShipmentService}. */
@Service
@Transactional
public class EQAShipmentServiceImpl implements EQAShipmentService {

    private static final String ACTIVE_ENROLLMENT = "Active";

    /** Provider cycle states, in FR-V2.1-18 order. */
    private static final List<EQACycleStatus> PROVIDER_STATES = List.of(EQACycleStatus.PLANNED,
            EQACycleStatus.PREP_IN_PROGRESS, EQACycleStatus.READY_TO_SHIP, EQACycleStatus.SHIPPED,
            EQACycleStatus.DELIVERED, EQACycleStatus.SUBMISSIONS_OPEN, EQACycleStatus.SUBMISSIONS_CLOSED,
            EQACycleStatus.SCORING, EQACycleStatus.SCORED, EQACycleStatus.CLOSED);

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQACycleService eqaCycleService;

    @Autowired
    private EQAPanelDAO eqaPanelDAO;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private EQAProgramEnrollmentDAO eqaProgramEnrollmentDAO;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Autowired
    private ShipmentService shipmentService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProviderCycles() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQACycle cycle : eqaCycleDAO.getAll()) {
            if (cycle.getScheme() == null) {
                continue;
            }
            int participants = activeEnrollments(cycle).size();
            if (participants == 0) {
                continue;
            }
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", cycle.getId());
            dto.put("cycleNumber", cycle.getCycleNumber());
            dto.put("cycleName", cycle.getCycleName());
            dto.put("status", cycle.getStatus() == null ? null : cycle.getStatus().name());
            dto.put("schemeId", cycle.getScheme().getId());
            dto.put("schemeName", cycle.getScheme().getName());
            dto.put("schemeType",
                    cycle.getScheme().getSchemeType() == null ? null : cycle.getScheme().getSchemeType().name());
            dto.put("participantCount", participants);
            dto.put("panelCount", panelsOf(cycle.getId()).size());
            dto.put("plannedEndDate", cycle.getPlannedEndDate() == null ? null : cycle.getPlannedEndDate().toString());
            rows.add(dto);
        }
        rows.sort((a, b) -> compareByProviderProgress(a, b));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPrepStatus(Long cycleId) {
        EQACycle cycle = cycle(cycleId);
        int participants = activeEnrollments(cycle).size();
        List<EQAPanel> panels = panelsOf(cycleId);

        List<Map<String, Object>> panelDtos = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        if (panels.isEmpty()) {
            blockers.add("No panel has been prepared for this cycle");
        }
        for (EQAPanel panel : panels) {
            int needed = eqaCycleService.aliquotsNeeded(panel, participants);
            int produced = zeroIfNull(panel.getAliquotsProduced());

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("panelId", panel.getId());
            dto.put("panelName", panel.getPanelName());
            dto.put("status", panel.getStatus() == null ? null : panel.getStatus().name());
            dto.put("sampleCount", sampleCount(panel.getId()));
            dto.put("aliquotsProduced", produced);
            dto.put("aliquotsReserved", zeroIfNull(panel.getAliquotsReserved()));
            dto.put("aliquotsShipped", zeroIfNull(panel.getAliquotsShipped()));
            dto.put("aliquotsNeeded", needed);
            dto.put("shortfall", Math.max(0, needed - produced));
            dto.put("homogeneityQcPassed", Boolean.TRUE.equals(panel.getHomogeneityQcPassed()));
            dto.put("homogeneityQcNotes", panel.getHomogeneityQcNotes());
            dto.put("storageTemp", panel.getStorageTemp() == null ? null : panel.getStorageTemp().name());
            dto.put("lotNumber", panel.getLotNumber());
            dto.put("expirationDate", panel.getExpirationDate() == null ? null : panel.getExpirationDate().toString());
            panelDtos.add(dto);

            if (!Boolean.TRUE.equals(panel.getHomogeneityQcPassed())) {
                blockers.add("Panel " + panel.getPanelName() + " has not passed homogeneity QC");
            }
            if (produced < needed) {
                blockers.add("Panel " + panel.getPanelName() + " needs " + needed + " aliquots, has " + produced);
            }
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("cycleId", cycleId);
        status.put("cycleName", cycle.getCycleName());
        status.put("cycleStatus", cycle.getStatus() == null ? null : cycle.getStatus().name());
        status.put("participantCount", participants);
        status.put("panels", panelDtos);
        status.put("blockers", blockers);
        // The button state the workbench renders; the gate itself is enforced on the
        // transition (T-10), so a stale client cannot ship past it.
        status.put("readyToShipAllowed", blockers.isEmpty() && cycle.getStatus() == EQACycleStatus.PREP_IN_PROGRESS);
        return status;
    }

    @Override
    public Map<String, Object> savePrep(Long panelId, Integer aliquotsProduced, Integer aliquotsReserved,
            Boolean homogeneityQcPassed, String homogeneityQcNotes, String sysUserId) {
        EQAPanel panel = eqaPanelDAO.get(panelId)
                .orElseThrow(() -> new ObjectNotFoundException(panelId, EQAPanel.class.getName()));

        if (aliquotsProduced != null) {
            panel.setAliquotsProduced(aliquotsProduced);
        }
        if (aliquotsReserved != null) {
            panel.setAliquotsReserved(aliquotsReserved);
        }
        if (zeroIfNull(panel.getAliquotsProduced()) < 0 || zeroIfNull(panel.getAliquotsReserved()) < 0) {
            throw new IllegalArgumentException("Aliquot counts cannot be negative");
        }
        // Mirrors the qa/017 CHECK, so the workbench sees a message instead of a
        // constraint violation.
        if (zeroIfNull(panel.getAliquotsProduced()) < zeroIfNull(panel.getAliquotsReserved())
                + zeroIfNull(panel.getAliquotsShipped())) {
            throw new IllegalArgumentException("Aliquots produced cannot be fewer than reserved plus already shipped");
        }
        if (homogeneityQcPassed != null) {
            panel.setHomogeneityQcPassed(homogeneityQcPassed);
        }
        if (homogeneityQcNotes != null) {
            panel.setHomogeneityQcNotes(homogeneityQcNotes);
        }
        panel.setSysUserId(sysUserId);
        EQAPanel saved = eqaPanelDAO.update(panel);
        if (saved.getCycle() == null) {
            throw new IllegalArgumentException("Panel " + panelId + " is not bound to a cycle yet");
        }
        return getPrepStatus(saved.getCycle().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getShipmentRows(Long cycleId) {
        EQACycle cycle = cycle(cycleId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAProgramEnrollment enrollment : activeEnrollments(cycle)) {
            rows.add(toShipmentRow(cycleId, enrollment, box(cycleId, enrollment.getOrganizationId())));
        }
        return rows;
    }

    @Override
    public Map<String, Object> saveShipmentDetails(Long cycleId, Long organizationId, String courier,
            String trackingNumber, Date estimatedDeliveryDate, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        EQAProgramEnrollment enrollment = enrollment(cycle, organizationId);

        ShippingBox box = box(cycleId, organizationId);
        if (box == null) {
            box = createBox(cycle, organizationId, sysUserId);
        } else if (box.getState() != BoxState.DRAFT && box.getState() != BoxState.READY_TO_SEND) {
            // Courier details of a box already in transit are history, not a draft.
            throw new IllegalStateException(
                    "Box " + box.getBoxId() + " is " + box.getState() + ", so its details can no longer be changed");
        }

        Shipment shipment = shipmentService.getShipmentByShippingBoxId(box.getId());
        boolean isNew = shipment == null;
        if (isNew) {
            shipment = new Shipment();
            shipment.setShippingBox(box);
            shipment.setStatus(ShipmentStatus.PENDING);
        }
        shipment.setCourier(courier);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setEstimatedDeliveryDate(
                estimatedDeliveryDate == null ? null : new Timestamp(estimatedDeliveryDate.getTime()));
        shipment.setSysUserId(sysUserId);
        shipment.setSystemUserId(userId(sysUserId));
        if (isNew) {
            shipment.setId(shipmentService.createShipment(shipment).getId());
        } else {
            shipmentService.updateShipment(shipment);
        }

        // A packed box waiting for dispatch: the EQA prep gate, not the box's own
        // sample count, is what says this box may go out — so this walks the state
        // directly rather than through markReadyToSend(), which refuses a box with
        // no sample items (panel material is not a lab SampleItem).
        if (box.getState() == BoxState.DRAFT) {
            box = shippingBoxService.changeBoxState(box.getId(), BoxState.READY_TO_SEND, userId(sysUserId));
        }
        return toShipmentRow(cycleId, enrollment, box);
    }

    @Override
    public List<Map<String, Object>> markShipped(Long cycleId, List<Long> organizationIds, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        if (cycle.getStatus() != EQACycleStatus.READY_TO_SHIP && cycle.getStatus() != EQACycleStatus.SHIPPED) {
            throw new IllegalStateException(
                    "A cycle in " + cycle.getStatus() + " has not been cleared to ship; complete prep first");
        }
        if (organizationIds == null || organizationIds.isEmpty()) {
            throw new IllegalArgumentException("Name at least one participant to mark shipped");
        }

        // Resolve and validate every participant before writing anything, so a bulk
        // dispatch is all-or-nothing rather than half-sent.
        List<EQAProgramEnrollment> enrollments = new ArrayList<>();
        List<ShippingBox> boxes = new ArrayList<>();
        for (Long organizationId : organizationIds) {
            EQAProgramEnrollment enrollment = enrollment(cycle, organizationId);
            ShippingBox box = box(cycleId, organizationId);
            if (box == null) {
                throw new IllegalArgumentException(
                        "No shipment prepared for organization " + organizationId + "; record courier details first");
            }
            Shipment shipment = shipmentService.getShipmentByShippingBoxId(box.getId());
            if (shipment == null || GenericValidator.isBlankOrNull(shipment.getCourier())) {
                throw new IllegalArgumentException(
                        "Record a courier for organization " + organizationId + " before marking it shipped");
            }
            if (box.getState() != BoxState.READY_TO_SEND) {
                throw new IllegalStateException(
                        "Box " + box.getBoxId() + " is " + box.getState() + ", so it cannot be marked shipped");
            }
            enrollments.add(enrollment);
            boxes.add(box);
        }

        // Refuse a dispatch the inventory cannot cover, before the qa/017 CHECK turns
        // it into a constraint error: produced >= reserved + shipped must still hold
        // after this batch.
        for (EQAPanel panel : panelsOf(cycleId)) {
            int afterBatch = zeroIfNull(panel.getAliquotsShipped()) + sampleCount(panel.getId()) * boxes.size();
            if (afterBatch + zeroIfNull(panel.getAliquotsReserved()) > zeroIfNull(panel.getAliquotsProduced())) {
                throw new IllegalArgumentException("Panel " + panel.getPanelName() + " does not hold enough aliquots"
                        + " for " + boxes.size() + " more participants; produce more before dispatching");
            }
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            ShippingBox box = shippingBoxService.changeBoxState(boxes.get(i).getId(), BoxState.SENT, userId(sysUserId));
            Shipment shipment = shipmentService.getShipmentByShippingBoxId(box.getId());
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            shipment.setShippedDate(now);
            shipment.setSysUserId(sysUserId);
            shipment.setSystemUserId(userId(sysUserId));
            shipmentService.updateShipment(shipment);
            rows.add(toShipmentRow(cycleId, enrollments.get(i), box));
        }

        // Inventory follows the material: each dispatched participant consumes one
        // aliquot per panel sample (FR-V2.5-12).
        for (EQAPanel panel : panelsOf(cycleId)) {
            panel.setAliquotsShipped(
                    zeroIfNull(panel.getAliquotsShipped()) + sampleCount(panel.getId()) * boxes.size());
            panel.setSysUserId(sysUserId);
            eqaPanelDAO.update(panel);
        }

        if (cycle.getStatus() == EQACycleStatus.READY_TO_SHIP) {
            eqaCycleService.transition(cycleId, EQACycleStatus.SHIPPED, EQAStateMachine.PROVIDER, EQATriggerType.AUTO,
                    EQATriggerEvent.FIRST_SHIPMENT_SENT, null, "First participant shipment dispatched", sysUserId);
        }
        return rows;
    }

    // ---- helpers ----

    private EQACycle cycle(Long cycleId) {
        return eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new ObjectNotFoundException(cycleId, EQACycle.class.getName()));
    }

    private List<EQAProgramEnrollment> activeEnrollments(EQACycle cycle) {
        return cycle.getScheme() == null ? List.of()
                : eqaProgramEnrollmentDAO.findByProgramIdAndStatus(cycle.getScheme().getId(), ACTIVE_ENROLLMENT);
    }

    private EQAProgramEnrollment enrollment(EQACycle cycle, Long organizationId) {
        for (EQAProgramEnrollment enrollment : activeEnrollments(cycle)) {
            if (enrollment.getOrganizationId().equals(organizationId)) {
                return enrollment;
            }
        }
        throw new IllegalArgumentException(
                "Organization " + organizationId + " is not an active participant of this cycle's scheme");
    }

    private List<EQAPanel> panelsOf(Long cycleId) {
        return eqaPanelDAO.getAllMatching("cycle.id", cycleId);
    }

    private int sampleCount(Long panelId) {
        return eqaPanelSampleDAO.getAllMatching("panel.id", panelId).size();
    }

    /**
     * One box per participant per cycle, identified structurally rather than by the
     * shipment module's sequential generator: the id is the idempotency key that
     * makes re-saving courier details an update.
     */
    private String boxCode(Long cycleId, Long organizationId) {
        return "EQA-C" + cycleId + "-" + organizationId;
    }

    private ShippingBox box(Long cycleId, Long organizationId) {
        return shippingBoxService.getBoxByBoxId(boxCode(cycleId, organizationId));
    }

    private ShippingBox createBox(EQACycle cycle, Long organizationId, String sysUserId) {
        Organization participant = organizationService.getOrganizationById(String.valueOf(organizationId));
        if (participant == null) {
            throw new IllegalArgumentException("Organization " + organizationId + " does not exist");
        }
        ShippingBox box = new ShippingBox();
        box.setBoxId(boxCode(cycle.getId(), organizationId));
        box.setDestinationFacility(participant);
        box.setEqaCycleId(cycle.getId());
        box.setSystemUserId(userId(sysUserId));
        box.setNotes("EQA panel material for cycle " + displayName(cycle));
        // Panel storage temperature is the box's temperature requirement — the
        // material's constraint, not a separate choice.
        for (EQAPanel panel : panelsOf(cycle.getId())) {
            if (panel.getStorageTemp() != null) {
                box.setTemperatureRequirement(panel.getStorageTemp().name());
                break;
            }
        }
        return shippingBoxService.createBox(box);
    }

    private Map<String, Object> toShipmentRow(Long cycleId, EQAProgramEnrollment enrollment, ShippingBox box) {
        Organization participant = organizationService
                .getOrganizationById(String.valueOf(enrollment.getOrganizationId()));
        Shipment shipment = box == null ? null : shipmentService.getShipmentByShippingBoxId(box.getId());

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cycleId", cycleId);
        row.put("enrollmentId", enrollment.getId());
        row.put("organizationId", enrollment.getOrganizationId());
        row.put("organizationName", participant == null ? null : participant.getOrganizationName());
        row.put("boxId", box == null ? null : box.getId());
        row.put("boxCode", box == null ? null : box.getBoxId());
        row.put("boxState", box == null ? null : box.getState().name());
        row.put("temperatureRequirement", box == null ? null : box.getTemperatureRequirement());
        row.put("shipmentId", shipment == null ? null : shipment.getId());
        row.put("courier", shipment == null ? null : shipment.getCourier());
        row.put("trackingNumber", shipment == null ? null : shipment.getTrackingNumber());
        row.put("estimatedDeliveryDate", shipment == null || shipment.getEstimatedDeliveryDate() == null ? null
                : shipment.getEstimatedDeliveryDate().toString());
        row.put("shippedDate",
                shipment == null || shipment.getShippedDate() == null ? null : shipment.getShippedDate().toString());
        row.put("shipmentStatus", shipment == null ? null : shipment.getStatus().name());
        return row;
    }

    private String displayName(EQACycle cycle) {
        return GenericValidator.isBlankOrNull(cycle.getCycleName()) ? String.valueOf(cycle.getCycleNumber())
                : cycle.getCycleName();
    }

    /** Boxes record the acting user as an Integer; EQA carries it as a String. */
    private Integer userId(String sysUserId) {
        try {
            return Integer.valueOf(sysUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot attribute this shipment to an authenticated user");
        }
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    /** Least-advanced provider cycles first — those are the ones needing work. */
    private static int compareByProviderProgress(Map<String, Object> a, Map<String, Object> b) {
        int byState = Integer.compare(providerStateOrder(a), providerStateOrder(b));
        return byState != 0 ? byState : Long.compare((Long) b.get("id"), (Long) a.get("id"));
    }

    private static int providerStateOrder(Map<String, Object> row) {
        Object status = row.get("status");
        int index = status == null ? -1 : PROVIDER_STATES.indexOf(EQACycleStatus.valueOf((String) status));
        return index < 0 ? PROVIDER_STATES.size() : index;
    }
}
