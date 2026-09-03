package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.shipment.service.ShipmentService;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panel receipt intake and its transactional side-effects (OGC-609,
 * FR-V2.1-20).
 */
@Service
@Transactional
public class EQAPanelReceiptServiceImpl extends BaseObjectServiceImpl<EQAPanelReceipt, Long>
        implements EQAPanelReceiptService {

    @Autowired
    private EQAPanelReceiptDAO eqaPanelReceiptDAO;

    @Autowired
    private EQACycleService eqaCycleService;

    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private ShippingBoxService shippingBoxService;

    public EQAPanelReceiptServiceImpl() {
        super(EQAPanelReceipt.class);
    }

    @Override
    protected EQAPanelReceiptDAO getBaseObjectDAO() {
        return eqaPanelReceiptDAO;
    }

    @Override
    public EQAPanelReceipt recordReceipt(Long cycleId, Long labEnrollmentId, Integer shipmentId,
            BigDecimal receivedTempC, Boolean integrityOk, String integrityNotes, Long receivedBy, String sysUserId) {
        return recordReceipt(cycleId, labEnrollmentId, shipmentId, null, receivedTempC, integrityOk, integrityNotes,
                receivedBy, sysUserId);
    }

    @Override
    public EQAPanelReceipt recordReceipt(Long cycleId, Long labEnrollmentId, Integer shipmentId, Integer shippingBoxId,
            BigDecimal receivedTempC, Boolean integrityOk, String integrityNotes, Long receivedBy, String sysUserId) {
        if (receivedBy == null) {
            throw new IllegalArgumentException("A receipt requires the receiving user");
        }

        // Idempotency: the DB unique constraint is the backstop; this read is what
        // makes the double-call read-only instead of a constraint error.
        List<EQAPanelReceipt> existing = eqaPanelReceiptDAO
                .getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId));
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        // Resolve everything the receipt references before writing anything, so a
        // bad reference is a clean 4xx instead of a flush-time constraint error
        // (the FK to shipment stays as the backstop).
        EQACycle cycle = eqaCycleService.get(cycleId);
        Shipment shipment = null;
        if (shipmentId != null) {
            shipment = shipmentService.getShipmentById(shipmentId);
            if (shipment == null) {
                throw new IllegalArgumentException("Receipt names shipment " + shipmentId + ", which does not exist");
            }
        }

        ShippingBox box = null;
        if (shippingBoxId != null) {
            box = shippingBoxService.getBoxById(shippingBoxId);
            if (box == null) {
                throw new IllegalArgumentException(
                        "Receipt names consignment " + shippingBoxId + ", which does not exist");
            }
        }

        EQAPanelReceipt receipt = new EQAPanelReceipt();
        receipt.setCycle(cycle);
        receipt.setLabEnrollmentId(labEnrollmentId);
        receipt.setShipmentId(shipmentId);
        receipt.setShippingBoxId(shippingBoxId);
        receipt.setReceivedDate(new Date(System.currentTimeMillis()));
        receipt.setReceivedBy(receivedBy);
        receipt.setReceivedTempC(receivedTempC);
        receipt.setIntegrityOk(integrityOk == null || integrityOk);
        receipt.setIntegrityNotes(integrityNotes);
        receipt.setSysUserId(sysUserId);
        receipt.setId(eqaPanelReceiptDAO.insert(receipt));

        if (shipment != null) {
            shipment.setActualDeliveryDate(new Timestamp(System.currentTimeMillis()));
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipment.setSysUserId(sysUserId);
            shipmentService.updateShipment(shipment);
        }

        // FR-V2.2-12: taking the panel in IS receiving the consignment. Walking the
        // imported box to RECEIVED here is what completes the provider's
        // SupplyDelivery (the delivery backflow), so the sender's monitor turns
        // DELIVERED without a second act on the Reception screen. A box someone
        // already received there is left alone.
        if (box != null && box.getState() != null && box.getState().canTransitionTo(BoxState.RECEIVED)) {
            shippingBoxService.changeBoxState(box.getId(), BoxState.RECEIVED,
                    receivedBy == null ? null : receivedBy.intValue());
        }

        // Receipt is the participant machine's planned -> panel_received trigger.
        // A cycle already past PLANNED keeps its state — the receipt is late
        // paperwork then, not a regression.
        if (cycle.getStatus() == EQACycleStatus.PLANNED) {
            eqaCycleService.transition(cycleId, EQACycleStatus.PANEL_RECEIVED, EQAStateMachine.PARTICIPANT,
                    EQATriggerType.AUTO, EQATriggerEvent.PANEL_RECEIPT, null, "Panel receipt recorded", sysUserId);
        }

        return receipt;
    }
}
