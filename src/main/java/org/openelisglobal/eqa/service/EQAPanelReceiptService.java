package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;

public interface EQAPanelReceiptService extends BaseObjectService<EQAPanelReceipt, Long> {

    /**
     * Record that this lab received its panel for a cycle (FR-V2.1-20), atomically
     * with its side-effects: the matched shipment gets its actual delivery date and
     * DELIVERED status, and a PLANNED cycle advances to PANEL_RECEIVED on the
     * participant machine. Either all three rows change or none do.
     *
     * <p>
     * Idempotent per (cycle, lab enrollment): a second call returns the existing
     * receipt unchanged and performs no side-effects.
     */
    EQAPanelReceipt recordReceipt(Long cycleId, Long labEnrollmentId, Integer shipmentId, BigDecimal receivedTempC,
            Boolean integrityOk, String integrityNotes, Long receivedBy, String sysUserId);

    /**
     * As above, also taking delivery of the imported consignment
     * {@code shippingBoxId}: the box goes RECEIVED in the same transaction, which
     * completes the provider's SupplyDelivery so its monitor learns the panel
     * arrived, and the receipt records the box as its shipment reference. A box
     * already received stays as it is.
     */
    EQAPanelReceipt recordReceipt(Long cycleId, Long labEnrollmentId, Integer shipmentId, Integer shippingBoxId,
            BigDecimal receivedTempC, Boolean integrityOk, String integrityNotes, Long receivedBy, String sysUserId);
}
