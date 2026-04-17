package org.openelisglobal.shipment.service;

import java.util.List;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ShipmentService {

    /**
     * Get shipment by ID
     *
     * @param id Shipment ID
     * @return Shipment or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    Shipment getShipmentById(Integer id);

    /**
     * Get shipment by shipping box ID
     *
     * @param shippingBoxId Shipping box ID
     * @return Shipment or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    Shipment getShipmentByShippingBoxId(Integer shippingBoxId);

    /**
     * Get shipment by tracking number
     *
     * @param trackingNumber Tracking number
     * @return Shipment or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    Shipment getShipmentByTrackingNumber(String trackingNumber);

    /**
     * Get shipments by status
     *
     * @param status Shipment status
     * @return List of shipments
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<Shipment> getShipmentsByStatus(ShipmentStatus status);

    /**
     * Get shipments by courier
     *
     * @param courier Courier name
     * @return List of shipments
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<Shipment> getShipmentsByCourier(String courier);

    /**
     * Create a new shipment
     *
     * @param shipment Shipment to create
     * @return Created Shipment with ID
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    Shipment createShipment(Shipment shipment);

    /**
     * Update existing shipment
     *
     * @param shipment Shipment to update
     * @return Updated Shipment
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    Shipment updateShipment(Shipment shipment);

    /**
     * Update shipment status
     *
     * @param id        Shipment ID
     * @param newStatus New status
     * @return Updated Shipment
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    Shipment updateShipmentStatus(Integer id, ShipmentStatus newStatus);

    /**
     * Delete shipment
     *
     * @param id Shipment ID
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    void deleteShipment(Integer id);
}
