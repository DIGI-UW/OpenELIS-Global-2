package org.openelisglobal.shipment.service;

import java.util.List;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;

public interface ShipmentService {

    /**
     * Get shipment by ID
     *
     * @param id Shipment ID
     * @return Shipment or null if not found
     */
    Shipment getShipmentById(Integer id);

    /**
     * Get shipment by shipping box ID
     *
     * @param shippingBoxId Shipping box ID
     * @return Shipment or null if not found
     */
    Shipment getShipmentByShippingBoxId(Integer shippingBoxId);

    /**
     * Get shipment by tracking number
     *
     * @param trackingNumber Tracking number
     * @return Shipment or null if not found
     */
    Shipment getShipmentByTrackingNumber(String trackingNumber);

    /**
     * Get shipments by status
     *
     * @param status Shipment status
     * @return List of shipments
     */
    List<Shipment> getShipmentsByStatus(ShipmentStatus status);

    /**
     * Get shipments by courier
     *
     * @param courier Courier name
     * @return List of shipments
     */
    List<Shipment> getShipmentsByCourier(String courier);

    /**
     * Create a new shipment
     *
     * @param shipment Shipment to create
     * @return Created Shipment with ID
     */
    Shipment createShipment(Shipment shipment);

    /**
     * Update existing shipment
     *
     * @param shipment Shipment to update
     * @return Updated Shipment
     */
    Shipment updateShipment(Shipment shipment);

    /**
     * Update shipment status
     *
     * @param id        Shipment ID
     * @param newStatus New status
     * @return Updated Shipment
     */
    Shipment updateShipmentStatus(Integer id, ShipmentStatus newStatus);

    /**
     * Delete shipment
     *
     * @param id Shipment ID
     */
    void deleteShipment(Integer id);
}
