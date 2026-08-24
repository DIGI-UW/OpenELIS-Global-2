package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQACycleParticipantDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAProgramEnrollmentDAO;
import org.openelisglobal.eqa.service.EQAPrepGate.PanelRequirement;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleParticipant;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
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

    /** FR-V2.5-14: the grace the monitor allows before a shipment reads overdue. */
    private static final int OVERDUE_GRACE_BUSINESS_DAYS = 2;

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
    private EQACycleParticipantDAO eqaCycleParticipantDAO;

    @Autowired
    private EQAPanelReceiptDAO eqaPanelReceiptDAO;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Autowired
    private ShipmentService shipmentService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProviderSchemes() {
        List<Object[]> schemeRows = eqaProgramEnrollmentDAO.findProviderSchemeRows();
        if (schemeRows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<EQACycle>> cyclesByScheme = new LinkedHashMap<>();
        List<Long> schemeIds = new ArrayList<>();
        for (Object[] row : schemeRows) {
            schemeIds.add(((Number) row[0]).longValue());
        }
        for (EQACycle cycle : eqaCycleDAO.findBySchemeIds(schemeIds)) {
            cyclesByScheme.computeIfAbsent(cycle.getScheme().getId(), id -> new ArrayList<>()).add(cycle);
        }

        List<Long> cycleIds = new ArrayList<>();
        for (List<EQACycle> cycles : cyclesByScheme.values()) {
            for (EQACycle cycle : cycles) {
                cycleIds.add(cycle.getId());
            }
        }
        Map<Long, Integer> panelCounts = new LinkedHashMap<>();
        for (Object[] row : eqaPanelDAO.countByCycleIds(cycleIds)) {
            panelCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        Map<Long, Integer> rosterCounts = new LinkedHashMap<>();
        for (EQACycleParticipant participant : eqaCycleParticipantDAO.findActiveByCycleIds(cycleIds)) {
            rosterCounts.merge(participant.getCycle().getId(), 1, Integer::sum);
        }

        List<Map<String, Object>> schemes = new ArrayList<>();
        for (Object[] row : schemeRows) {
            Long schemeId = ((Number) row[0]).longValue();
            int enrolled = ((Number) row[4]).intValue();

            List<Map<String, Object>> cycleDtos = new ArrayList<>();
            for (EQACycle cycle : cyclesByScheme.getOrDefault(schemeId, List.of())) {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", cycle.getId());
                dto.put("cycleNumber", cycle.getCycleNumber());
                dto.put("cycleName", cycle.getCycleName());
                dto.put("status", cycle.getStatus() == null ? null : cycle.getStatus().name());
                // A cycle predating qa/032 has no roster, and was sized by the scheme's
                // enrollments — the same fallback participantOrganizationIds applies, so
                // the list cannot show a count the prep gate disagrees with.
                dto.put("participantCount", rosterCounts.getOrDefault(cycle.getId(), enrolled));
                dto.put("panelCount", panelCounts.getOrDefault(cycle.getId(), 0));
                dto.put("distributionMethod",
                        cycle.getDistributionMethod() == null ? null : cycle.getDistributionMethod().name());
                cycleDtos.add(dto);
            }

            Map<String, Object> scheme = new LinkedHashMap<>();
            scheme.put("id", schemeId);
            scheme.put("name", row[1]);
            scheme.put("provider", row[2]);
            scheme.put("schemeType", row[3] == null ? null : ((EQASchemeType) row[3]).name());
            scheme.put("enrolledParticipantCount", enrolled);
            scheme.put("cycles", cycleDtos);
            schemes.add(scheme);
        }
        return schemes;
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
        // FR-V2.5-02 step 4, read where the cycle is worked on: the operator packing a
        // panel needs to know whether scores go back over FHIR or as a file.
        status.put("distributionMethod",
                cycle.getDistributionMethod() == null ? null : cycle.getDistributionMethod().name());
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
        for (Long organizationId : eqaCycleService.participantOrganizationIds(cycle)) {
            ShippingBox box = boxes.get(boxCode(cycleId, organizationId));
            rows.add(toShipmentRow(organizationId, box, box == null ? null : box.getShipment()));
        }
        return rows;
    }

    @Override
    public Map<String, Object> saveShipmentDetails(Long cycleId, Long organizationId, String courier,
            String trackingNumber, Date estimatedDeliveryDate, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        requireParticipant(eqaCycleService.participantOrganizationIds(cycle), organizationId);

        ShippingBox box = shippingBoxService.getBoxByBoxId(boxCode(cycleId, organizationId));
        if (box == null) {
            box = createBox(cycle, organizationId, boxCode(cycleId, organizationId), sysUserId);
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
        return toShipmentRow(organizationId, box, shipment);
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
        List<Long> participants = eqaCycleService.participantOrganizationIds(cycle);
        Map<String, ShippingBox> boxes = boxesByCode(cycleId);
        List<Long> dispatchTo = new ArrayList<>();
        List<ShippingBox> dispatching = new ArrayList<>();
        for (Long organizationId : targets) {
            requireParticipant(participants, organizationId);
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
            dispatchTo.add(organizationId);
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

    // ---- FR-V2.5-14 receipt monitoring ----

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReceiptRows(Long cycleId) {
        EQACycle cycle = cycle(cycleId);
        Map<Long, ShippingBox> latest = latestBoxes(cycleId);
        Map<Integer, EQAPanelReceipt> receipts = receiptsByShipment(cycleId);
        List<Map<String, Object>> rows = new ArrayList<>();
        // The cycle's roster (T-24), not the scheme's enrollments: a lab enrolled
        // after this cycle was created is not one of its participants.
        for (Long organizationId : eqaCycleService.participantOrganizationIds(cycle)) {
            ShippingBox box = latest.get(organizationId);
            rows.add(toReceiptRow(organizationId, box, box == null ? null : box.getShipment(), receipts));
        }
        return rows;
    }

    @Override
    public Map<String, Object> markDelivered(Long cycleId, Long organizationId, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        requireParticipant(eqaCycleService.participantOrganizationIds(cycle), organizationId);
        ShippingBox box = latestBoxes(cycleId).get(organizationId);
        if (box == null || box.getShipment() == null) {
            throw new IllegalArgumentException(
                    "No shipment has been dispatched to organization " + organizationId + " for this cycle");
        }
        Shipment shipment = box.getShipment();
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            // Already receipted, by the participant (T-15) or by an earlier click.
            return toReceiptRow(organizationId, box, shipment, receiptsByShipment(cycleId));
        }
        if (box.getState() != BoxState.SENT && box.getState() != BoxState.IN_TRANSIT) {
            throw new IllegalStateException(
                    "Box " + box.getBoxId() + " is " + box.getState() + ", so it cannot be recorded as delivered");
        }

        box = shippingBoxService.changeBoxState(box.getId(), BoxState.RECEIVED, userId(sysUserId));
        // updateShipmentStatus stamps the actual delivery date with the status, as it
        // does the shipped date on dispatch — one owner for "this shipment arrived".
        shipment = shipmentService.updateShipmentStatus(shipment.getId(), ShipmentStatus.DELIVERED);
        shipment.setSysUserId(sysUserId);
        shipment.setSystemUserId(userId(sysUserId));

        openSubmissionsIfAllDelivered(cycle, sysUserId);
        return toReceiptRow(organizationId, box, shipment, receiptsByShipment(cycleId));
    }

    /**
     * AC-V2.5-13: once every active participant holds its panel there is nothing
     * left to wait for, so the cycle walks shipped → delivered → submissions_open
     * on its own. Each edge keeps its own audit row, as the machine requires.
     */
    private void openSubmissionsIfAllDelivered(EQACycle cycle, String sysUserId) {
        if (cycle.getStatus() != EQACycleStatus.SHIPPED) {
            return;
        }
        Map<Long, ShippingBox> latest = latestBoxes(cycle.getId());
        List<Long> participants = eqaCycleService.participantOrganizationIds(cycle);
        if (participants.isEmpty()) {
            return;
        }
        for (Long organizationId : participants) {
            ShippingBox box = latest.get(organizationId);
            if (box == null || box.getShipment() == null || box.getShipment().getStatus() != ShipmentStatus.DELIVERED) {
                return;
            }
        }
        for (EQACycleStatus next : List.of(EQACycleStatus.DELIVERED, EQACycleStatus.SUBMISSIONS_OPEN)) {
            eqaCycleService.transition(cycle.getId(), next, EQAStateMachine.PROVIDER, EQATriggerType.AUTO,
                    EQATriggerEvent.ALL_SHIPMENTS_DELIVERED, null, "Every participant panel is delivered", sysUserId);
        }
    }

    // ---- FR-V2.5-15 reprovisioning ----

    @Override
    public Map<String, Object> sendRepeat(Long cycleId, Long organizationId, String overrideNote, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        requireParticipant(eqaCycleService.participantOrganizationIds(cycle), organizationId);
        Map<Long, ShippingBox> latest = latestBoxes(cycleId);
        ShippingBox original = latest.get(organizationId);
        if (original == null || original.getShipment() == null || original.getShipment().getShippedDate() == null) {
            throw new IllegalArgumentException("Nothing has been dispatched to organization " + organizationId
                    + " yet, so there is no shipment to repeat");
        }

        // Validate the whole repeat against every panel before writing anything: a
        // refused reprovision must leave the inventory exactly as it was.
        List<EQAPanel> panels = panelsOf(cycleId);
        if (panels.isEmpty()) {
            throw new IllegalStateException("This cycle has no panel, so there is no material to repeat");
        }
        Map<Long, Integer> fromReserve = new LinkedHashMap<>();
        Map<Long, Integer> needed = new LinkedHashMap<>();
        for (EQAPanel panel : panels) {
            int samples = sampleCount(panel.getId());
            int reserved = zeroIfNull(panel.getAliquotsReserved());
            int takeFromReserve = Math.min(reserved, samples);
            int beyondReserve = samples - takeFromReserve;
            if (beyondReserve > 0) {
                // FR-V2.5-15: an empty reserve is a hard warning, not a refusal — but it
                // takes a written justification, and the material still has to exist.
                if (GenericValidator.isBlankOrNull(overrideNote)) {
                    throw new IllegalStateException(
                            "Panel " + panel.getPanelName() + " holds only " + reserved + " reserved aliquots of the "
                                    + samples + " a repeat needs;" + " record an override note to send it anyway");
                }
                int headroom = zeroIfNull(panel.getAliquotsProduced()) - reserved
                        - zeroIfNull(panel.getAliquotsShipped());
                if (beyondReserve > headroom) {
                    throw new IllegalStateException("Panel " + panel.getPanelName() + " has no aliquots left for a"
                            + " repeat; produce more before reprovisioning");
                }
            }
            fromReserve.put(panel.getId(), takeFromReserve);
            needed.put(panel.getId(), samples);
        }

        Shipment previous = original.getShipment();
        ShippingBox repeat = createBox(cycle, organizationId, nextRepeatCode(cycleId, organizationId, original),
                sysUserId);
        Shipment shipment = new Shipment();
        shipment.setShippingBox(repeat);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setCourier(previous.getCourier());
        shipment.setRepeatOfShipmentId(previous.getId());
        shipment.setSysUserId(sysUserId);
        shipment.setSystemUserId(userId(sysUserId));
        if (!GenericValidator.isBlankOrNull(overrideNote)) {
            repeat.setNotes(repeat.getNotes() + " — repeat, override: " + overrideNote);
        }
        shipment = shipmentService.createShipment(shipment);

        // A repeat is dispatched by the act of sending it: there is no second decision
        // to make, and the courier details come from the shipment it replaces.
        repeat = shippingBoxService.changeBoxState(repeat.getId(), BoxState.READY_TO_SEND, userId(sysUserId));
        repeat = shippingBoxService.changeBoxState(repeat.getId(), BoxState.SENT, userId(sysUserId));
        shipment = shipmentService.updateShipmentStatus(shipment.getId(), ShipmentStatus.IN_TRANSIT);
        shipment.setSysUserId(sysUserId);
        shipment.setSystemUserId(userId(sysUserId));

        // Repeat material comes out of the reserve first (FR-V2.5-15); whatever the
        // reserve could not cover was justified above and comes out of production.
        for (EQAPanel panel : panels) {
            panel.setAliquotsReserved(zeroIfNull(panel.getAliquotsReserved()) - fromReserve.get(panel.getId()));
            panel.setAliquotsShipped(zeroIfNull(panel.getAliquotsShipped()) + needed.get(panel.getId()));
            panel.setSysUserId(sysUserId);
            eqaPanelDAO.update(panel);
        }
        return toReceiptRow(organizationId, repeat, shipment, receiptsByShipment(cycleId));
    }

    // ---- helpers ----

    private EQACycle cycle(Long cycleId) {
        return eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new ObjectNotFoundException(cycleId, EQACycle.class.getName()));
    }

    /**
     * Shipping to a laboratory that is not on this cycle's roster would consume
     * aliquots the prep gate never counted, so it is refused rather than tolerated.
     */
    private void requireParticipant(List<Long> participants, Long organizationId) {
        if (!participants.contains(organizationId)) {
            throw new IllegalArgumentException(
                    "Organization " + organizationId + " is not a participant of this cycle");
        }
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
     * The box that currently represents each participant: its original, or the
     * newest repeat once one has been sent (FR-V2.5-15). Repeats are suffixed
     * {@code -R1}, {@code -R2}, … on the participant's base code — matched on that
     * exact suffix rather than a bare prefix, because one organization id can be
     * the prefix of another.
     */
    private Map<Long, ShippingBox> latestBoxes(Long cycleId) {
        Map<Long, ShippingBox> latest = new LinkedHashMap<>();
        Map<Long, Integer> generation = new LinkedHashMap<>();
        for (ShippingBox box : shippingBoxService.getBoxesByEqaCycle(cycleId)) {
            Long organizationId = box.getDestinationFacility() == null ? null
                    : Long.valueOf(box.getDestinationFacility().getId());
            if (organizationId == null) {
                continue;
            }
            int repeat = repeatNumber(box.getBoxId(), boxCode(cycleId, organizationId));
            if (repeat >= 0 && repeat >= generation.getOrDefault(organizationId, -1)) {
                generation.put(organizationId, repeat);
                latest.put(organizationId, box);
            }
        }
        return latest;
    }

    /**
     * 0 for the original, n for {@code -Rn}, -1 when the code is not this org's.
     */
    private static int repeatNumber(String boxId, String baseCode) {
        if (baseCode.equals(boxId)) {
            return 0;
        }
        if (boxId != null && boxId.startsWith(baseCode + "-R")) {
            try {
                return Integer.parseInt(boxId.substring(baseCode.length() + 2));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private String nextRepeatCode(Long cycleId, Long organizationId, ShippingBox current) {
        String base = boxCode(cycleId, organizationId);
        return base + "-R" + (repeatNumber(current.getBoxId(), base) + 1);
    }

    /**
     * The participant-recorded receipts of this cycle, keyed by the shipment each
     * one confirms. That FK is the only join back to an organization: a receipt's
     * lab_enrollment_id belongs to the participant's own enrollment table, which
     * carries no organization at all.
     */
    private Map<Integer, EQAPanelReceipt> receiptsByShipment(Long cycleId) {
        Map<Integer, EQAPanelReceipt> receipts = new LinkedHashMap<>();
        for (EQAPanelReceipt receipt : eqaPanelReceiptDAO.getAllMatching("cycle.id", cycleId)) {
            if (receipt.getShipmentId() != null) {
                receipts.put(receipt.getShipmentId(), receipt);
            }
        }
        return receipts;
    }

    /**
     * A shipment row plus what receiving adds: when it arrived, the participant's
     * cold-chain and integrity findings when they recorded a receipt, the shipment
     * this one repeats, and the single status the monitor renders.
     */
    private Map<String, Object> toReceiptRow(Long organizationId, ShippingBox box, Shipment shipment,
            Map<Integer, EQAPanelReceipt> receipts) {
        Map<String, Object> row = toShipmentRow(organizationId, box, shipment);
        EQAPanelReceipt receipt = shipment == null ? null : receipts.get(shipment.getId());
        boolean delivered = shipment != null && shipment.getStatus() == ShipmentStatus.DELIVERED;
        boolean overdue = !delivered && shipment != null && isOverdue(shipment.getEstimatedDeliveryDate());

        row.put("shipmentId", shipment == null ? null : shipment.getId());
        row.put("receivedDate", shipment == null || shipment.getActualDeliveryDate() == null ? null
                : shipment.getActualDeliveryDate().toString());
        row.put("repeatOfShipmentId", shipment == null ? null : shipment.getRepeatOfShipmentId());
        row.put("receivedTempC", receipt == null ? null : receipt.getReceivedTempC());
        row.put("integrityOk", receipt == null ? null : receipt.getIntegrityOk());
        row.put("integrityNotes", receipt == null ? null : receipt.getIntegrityNotes());
        row.put("overdue", overdue);
        row.put("receiptStatus", receiptStatus(shipment, receipt, delivered, overdue));
        return row;
    }

    private String receiptStatus(Shipment shipment, EQAPanelReceipt receipt, boolean delivered, boolean overdue) {
        if (delivered) {
            return receipt != null && Boolean.FALSE.equals(receipt.getIntegrityOk()) ? "EXCEPTION" : "DELIVERED";
        }
        if (shipment == null || shipment.getShippedDate() == null) {
            return "NOT_SHIPPED";
        }
        return overdue ? "OVERDUE" : "IN_TRANSIT";
    }

    /**
     * FR-V2.5-14: overdue is two <em>business</em> days past the expected delivery
     * — a Friday delivery is not chased on Sunday. A shipment with no expected date
     * is never overdue: nothing was promised to be late against.
     */
    private static boolean isOverdue(Timestamp estimatedDelivery) {
        if (estimatedDelivery == null) {
            return false;
        }
        LocalDate due = estimatedDelivery.toLocalDateTime().toLocalDate();
        for (int added = 0; added < OVERDUE_GRACE_BUSINESS_DAYS;) {
            due = due.plusDays(1);
            if (due.getDayOfWeek() != DayOfWeek.SATURDAY && due.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return LocalDate.now().isAfter(due);
    }

    /**
     * Creating the box publishes it to the FHIR store as a SupplyDelivery, as any
     * other shipping box would be (failures are logged, not fatal). Nothing on it
     * carries a target value.
     */
    private ShippingBox createBox(EQACycle cycle, Long organizationId, String boxCode, String sysUserId) {
        Organization participant = organizationService.getOrganizationById(String.valueOf(organizationId));
        if (participant == null) {
            throw new IllegalArgumentException("Organization " + organizationId + " does not exist");
        }
        ShippingBox box = new ShippingBox();
        box.setBoxId(boxCode);
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

    private Map<String, Object> toShipmentRow(Long organizationId, ShippingBox box, Shipment shipment) {
        Organization participant = organizationService.getOrganizationById(String.valueOf(organizationId));

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("organizationId", organizationId);
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
