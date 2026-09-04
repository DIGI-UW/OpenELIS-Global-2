package org.openelisglobal.shipment.dao;

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
     * Fetch only the sys_user_id scalar for the given shipment ID. Using a scalar
     * projection avoids materialising a second managed entity in the current
     * persistence context, preventing a duplicate-entry conflict when the caller
     * subsequently merges the detached shipment.
     *
     * @param shipmentId the shipment primary key
     * @return the stored sys_user_id, or {@code null} if no row exists
     */
    Integer findSystemUserIdById(Integer shipmentId);
}
