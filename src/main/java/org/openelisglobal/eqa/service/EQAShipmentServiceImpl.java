package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQAPrepGate.PanelRequirement;
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

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQACycleService eqaCycleService;

    @Autowired
    private EQAPanelDAO eqaPanelDAO;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private EQAProgramEnrollmentService eqaProgramEnrollmentService;

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
        // Newest first. The active-enrollment test is what separates the cycles this
        // lab provides from the ones it takes part in: an externally provided scheme
        // has no participants of ours, so its cycles belong on My Cycles instead.
        for (EQACycle cycle : eqaCycleDAO.getAllOrdered("id", true)) {
            if (cycle.getScheme() == null) {
                continue;
            }
            long participants = eqaProgramEnrollmentService.countActiveEnrollments(cycle.getScheme().getId());
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
            dto.put("distributionMethod",
                    cycle.getDistributionMethod() == null ? null : cycle.getDistributionMethod().name());
            dto.put("participantCount", (int) participants);
            dto.put("panelCount", panelsOf(cycle.getId()).size());
            rows.add(dto);
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPrepStatus(Long cycleId) {
        EQACycle cycle = cycle(cycleId);
        // The gate the transition enforces, evaluated once (T-10): its blockers and its
        // arithmetic are what this renders, so the button and the rule agree.
        EQAPrepGate gate = eqaCycleService.evaluatePrepGate(cycle);

        List<Map<String, Object>> panelDtos = new ArrayList<>();
        for (PanelRequirement requirement : gate.panels()) {
            EQAPanel panel = requirement.panel();
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("panelId", panel.getId());
            dto.put("panelName", panel.getPanelName());
            dto.put("sampleCount", requirement.sampleCount());
            dto.put("aliquotsProduced", requirement.produced());
            dto.put("aliquotsReserved", zeroIfNull(panel.getAliquotsReserved()));
            dto.put("aliquotsShipped", zeroIfNull(panel.getAliquotsShipped()));
            dto.put("aliquotsNeeded", requirement.aliquotsNeeded());
            dto.put("shortfall", requirement.shortfall());
            dto.put("homogeneityQcPassed", Boolean.TRUE.equals(panel.getHomogeneityQcPassed()));
            dto.put("homogeneityQcNotes", panel.getHomogeneityQcNotes());
            panelDtos.add(dto);
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("cycleId", cycleId);
        status.put("cycleName", cycle.getCycleName());
        status.put("cycleStatus", cycle.getStatus() == null ? null : cycle.getStatus().name());
        status.put("participantCount", gate.participantCount());
        status.put("panels", panelDtos);
        status.put("blockers", gate.blockers());
        // The button state the workbench renders; the gate itself is enforced on the
        // transition (T-10), so a stale client cannot ship past it.
        status.put("readyToShipAllowed", gate.isClear() && cycle.getStatus() == EQACycleStatus.PREP_IN_PROGRESS);
        return status;
    }

    @Override
    public Map<String, Object> savePrep(Long panelId, Integer aliquotsProduced, Integer aliquotsReserved,
            Boolean homogeneityQcPassed, String homogeneityQcNotes, String sysUserId) {
        EQAPanel panel = eqaPanelDAO.get(panelId)
                .orElseThrow(() -> new ObjectNotFoundException(panelId, EQAPanel.class.getName()));
        if (panel.getCycle() == null) {
            throw new IllegalArgumentException("Panel " + panelId + " is not bound to a cycle yet");
        }

        // Validate the counts this save would leave behind before touching the entity:
        // a refused save must not depend on the rollback to stay invisible.
        int produced = aliquotsProduced == null ? zeroIfNull(panel.getAliquotsProduced()) : aliquotsProduced;
        int reserved = aliquotsReserved == null ? zeroIfNull(panel.getAliquotsReserved()) : aliquotsReserved;
        if (produced < 0 || reserved < 0) {
            throw new IllegalArgumentException("Aliquot counts cannot be negative");
        }
        // Mirrors the qa/017 CHECK, so the workbench sees a message instead of a
        // constraint violation.
        if (produced < reserved + zeroIfNull(panel.getAliquotsShipped())) {
            throw new IllegalArgumentException("Aliquots produced cannot be fewer than reserved plus already shipped");
        }

        panel.setAliquotsProduced(produced);
        panel.setAliquotsReserved(reserved);
        if (homogeneityQcPassed != null) {
            panel.setHomogeneityQcPassed(homogeneityQcPassed);
        }
        if (homogeneityQcNotes != null) {
            panel.setHomogeneityQcNotes(homogeneityQcNotes);
        }
        panel.setSysUserId(sysUserId);
        EQAPanel saved = eqaPanelDAO.update(panel);
        return getPrepStatus(saved.getCycle().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getShipmentRows(Long cycleId) {
        EQACycle cycle = cycle(cycleId);
        Map<String, ShippingBox> boxes = boxesByCode(cycleId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAProgramEnrollment enrollment : activeEnrollments(cycle)) {
            ShippingBox box = boxes.get(boxCode(cycleId, enrollment.getOrganizationId()));
            rows.add(toShipmentRow(enrollment, box, box == null ? null : box.getShipment()));
        }
        return rows;
    }

    @Override
    public Map<String, Object> saveShipmentDetails(Long cycleId, Long organizationId, String courier,
            String trackingNumber, Date estimatedDeliveryDate, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        EQAProgramEnrollment enrollment = participant(cycle, organizationId);

        ShippingBox box = shippingBoxService.getBoxByBoxId(boxCode(cycleId, organizationId));
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
        shipment = isNew ? shipmentService.createShipment(shipment) : shipmentService.updateShipment(shipment);

        // A packed box waiting for dispatch: the EQA prep gate, not the box's own
        // sample count, is what says this box may go out — so this walks the state
        // directly rather than through markReadyToSend(), which refuses a box with
        // no sample items (panel material is not a lab SampleItem).
        if (box.getState() == BoxState.DRAFT) {
            box = shippingBoxService.changeBoxState(box.getId(), BoxState.READY_TO_SEND, userId(sysUserId));
        }
        return toShipmentRow(enrollment, box, shipment);
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
        // One dispatch per participant however often the client names them, so a
        // repeated id cannot inflate the inventory arithmetic below.
        List<Long> targets = new ArrayList<>(new LinkedHashSet<>(organizationIds));

        // Resolve and validate every participant before writing anything, so a bulk
        // dispatch is all-or-nothing rather than half-sent. Enrollments and boxes are
        // each read once for the whole batch.
        List<EQAProgramEnrollment> participants = activeEnrollments(cycle);
        Map<String, ShippingBox> boxes = boxesByCode(cycleId);
        List<EQAProgramEnrollment> dispatchTo = new ArrayList<>();
        List<ShippingBox> dispatching = new ArrayList<>();
        for (Long organizationId : targets) {
            EQAProgramEnrollment enrollment = participant(participants, organizationId);
            ShippingBox box = boxes.get(boxCode(cycleId, organizationId));
            if (box == null) {
                throw new IllegalArgumentException(
                        "No shipment prepared for organization " + organizationId + "; record courier details first");
            }
            Shipment shipment = box.getShipment();
            if (shipment == null || GenericValidator.isBlankOrNull(shipment.getCourier())) {
                throw new IllegalArgumentException(
                        "Record a courier for organization " + organizationId + " before marking it shipped");
            }
            if (box.getState() != BoxState.READY_TO_SEND) {
                throw new IllegalStateException(
                        "Box " + box.getBoxId() + " is " + box.getState() + ", so it cannot be marked shipped");
            }
            dispatchTo.add(enrollment);
            dispatching.add(box);
        }

        // Refuse a dispatch the inventory cannot cover, before the qa/017 CHECK turns
        // it into a constraint error: produced >= reserved + shipped must still hold
        // after this batch. A shortfall is a conflict with the panel's current state,
        // like the box-state refusals above.
        List<EQAPanel> panels = panelsOf(cycleId);
        Map<Long, Integer> samplesPerPanel = new LinkedHashMap<>();
        for (EQAPanel panel : panels) {
            int samples = sampleCount(panel.getId());
            samplesPerPanel.put(panel.getId(), samples);
            int afterBatch = zeroIfNull(panel.getAliquotsShipped()) + samples * dispatching.size();
            if (afterBatch + zeroIfNull(panel.getAliquotsReserved()) > zeroIfNull(panel.getAliquotsProduced())) {
                throw new IllegalStateException("Panel " + panel.getPanelName() + " does not hold enough aliquots"
                        + " for " + dispatching.size() + " more participants; produce more before dispatching");
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < dispatching.size(); i++) {
            ShippingBox box = shippingBoxService.changeBoxState(dispatching.get(i).getId(), BoxState.SENT,
                    userId(sysUserId));
            // updateShipmentStatus stamps the shipped date with the status — one owner for
            // "this shipment left", rather than a second copy of the rule here. It has no
            // user to record, so the dispatcher is stamped on the returned managed entity
            // and flushes with the transaction.
            Shipment shipment = shipmentService.updateShipmentStatus(dispatching.get(i).getShipment().getId(),
                    ShipmentStatus.IN_TRANSIT);
            shipment.setSysUserId(sysUserId);
            shipment.setSystemUserId(userId(sysUserId));
            rows.add(toShipmentRow(dispatchTo.get(i), box, shipment));
        }

        // Inventory follows the material: each dispatched participant consumes one
        // aliquot per panel sample (FR-V2.5-12).
        for (EQAPanel panel : panels) {
            panel.setAliquotsShipped(
                    zeroIfNull(panel.getAliquotsShipped()) + samplesPerPanel.get(panel.getId()) * dispatching.size());
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
                : eqaProgramEnrollmentService.findActiveByProgramId(cycle.getScheme().getId());
    }

    private EQAProgramEnrollment participant(EQACycle cycle, Long organizationId) {
        return participant(activeEnrollments(cycle), organizationId);
    }

    private EQAProgramEnrollment participant(List<EQAProgramEnrollment> participants, Long organizationId) {
        for (EQAProgramEnrollment enrollment : participants) {
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
     * makes re-saving courier details an update, and box_id is UNIQUE.
     */
    private String boxCode(Long cycleId, Long organizationId) {
        return "EQA-C" + cycleId + "-" + organizationId;
    }

    /**
     * Every box of the cycle in one query, keyed by its code — this is what
     * qa/028's shipping_box.eqa_cycle_id index is for, and it is why rendering the
     * workbench does not cost a query per participant. Shipments ride along on the
     * same fetch.
     */
    private Map<String, ShippingBox> boxesByCode(Long cycleId) {
        Map<String, ShippingBox> boxes = new LinkedHashMap<>();
        for (ShippingBox box : shippingBoxService.getBoxesByEqaCycle(cycleId)) {
            boxes.put(box.getBoxId(), box);
        }
        return boxes;
    }

    /**
     * Creating the box publishes it to the FHIR store as a SupplyDelivery, as any
     * other shipping box would be (failures are logged, not fatal). Nothing on it
     * carries a target value.
     */
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

    private Map<String, Object> toShipmentRow(EQAProgramEnrollment enrollment, ShippingBox box, Shipment shipment) {
        Organization participant = organizationService
                .getOrganizationById(String.valueOf(enrollment.getOrganizationId()));

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("organizationId", enrollment.getOrganizationId());
        row.put("organizationName", participant == null ? null : participant.getOrganizationName());
        row.put("boxId", box == null ? null : box.getId());
        row.put("boxCode", box == null ? null : box.getBoxId());
        row.put("boxState", box == null ? null : box.getState().name());
        row.put("temperatureRequirement", box == null ? null : box.getTemperatureRequirement());
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

    /**
     * Boxes record the acting user as an Integer; EQA carries it as a String. A
     * session whose user id is not numeric is a server fault, not something the
     * operator can fix by editing the form.
     */
    private Integer userId(String sysUserId) {
        try {
            return Integer.valueOf(sysUserId);
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Cannot attribute this shipment to an authenticated user", e);
        }
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
