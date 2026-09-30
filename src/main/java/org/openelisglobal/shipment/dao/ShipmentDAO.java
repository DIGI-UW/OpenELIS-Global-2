package org.openelisglobal.shipment.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;

public interface ShipmentDAO extends BaseDAO<Shipment, Integer> {

    /**
     * Find shipment by shipping box ID
     *
     * @param shippingBoxId Shipping box ID
     * @return Shipment or null if not found
     */
    Shipment findByShippingBoxId(Integer shippingBoxId);

    /**
     * Find shipments by tracking number
     *
     * @param trackingNumber Tracking number
     * @return Shipment or null if not found
     */
    Shipment findByTrackingNumber(String trackingNumber);

    /**
     * Find shipments by status
     *
     * @param status Shipment status
     * @return List of shipments
     */
    List<Shipment> findByStatus(ShipmentStatus status);

    /**
     * Find shipments by courier
     *
     * @param courier Courier name
     * @return List of shipments
     */
    List<Shipment> findByCourier(String courier);

    /**
     * Newest shipped date per EQA cycle, one aggregate row per cycle that has
     * shipped at least once
     *
     * @param eqaCycleIds EQA cycle ids
     * @return rows of (eqaCycleId, max shippedDate)
     */
    List<Object[]> findLatestShippedDatesByEqaCycleIds(Collection<Long> eqaCycleIds);
}
