package org.openelisglobal.shipment.service;

import java.util.List;
import org.openelisglobal.shipment.dto.ExpectedSpecimenDTO;

/**
 * Reception-side resolution between a received shipment box and the referral
 * electronic orders the FHIR poller already imported. Bridges the box's
 * declared specimens (SupplyDelivery EXT_SPECIMEN) to their electronic orders
 * by the shared specimen UUID.
 */
public interface ShipmentReceptionService {

    /**
     * For a received box, list its declared specimens resolved to their electronic
     * orders, and link any whose order has already been accepted into a Sample to
     * the box (idempotent).
     *
     * @param shippingBoxId the local box id
     * @param systemUserId  actor for any created box-sample links
     * @return one entry per declared specimen, with resolution status
     */
    List<ExpectedSpecimenDTO> reconcileAndGetExpectedSpecimens(Integer shippingBoxId, Integer systemUserId);
}
