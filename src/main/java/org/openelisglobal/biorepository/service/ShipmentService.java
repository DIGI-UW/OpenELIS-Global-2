package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.ShipmentStatus;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for Shipment entity operations.
 */
public interface ShipmentService extends BaseObjectService<Shipment, Integer> {

    /**
     * Find shipment by delivery reference.
     * 
     * @param deliveryReference the unique delivery reference
     * @return shipment or null if not found
     */
    Shipment getByDeliveryReference(String deliveryReference);

    /**
     * Find shipments by status.
     * 
     * @param status the shipment status
     * @return list of shipments ordered by reception timestamp descending
     */
    List<Shipment> getByStatus(ShipmentStatus status);

    /**
     * Find shipments within a date range.
     * 
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of shipments ordered by reception timestamp descending
     */
    List<Shipment> getByDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Find shipments by receiver user.
     * 
     * @param receiverUserId the receiver's user ID
     * @return list of shipments ordered by reception timestamp descending
     */
    List<Shipment> getByReceiverId(Integer receiverUserId);

    /**
     * Get recent shipments with pagination.
     * 
     * @param offset starting offset
     * @param limit  maximum results
     * @return list of shipments ordered by reception timestamp descending
     */
    List<Shipment> getRecentShipments(int offset, int limit);

    /**
     * Count shipments by status.
     * 
     * @param status the shipment status
     * @return count of matching shipments
     */
    long countByStatus(ShipmentStatus status);

    /**
     * Search shipments by sender name, organization, or delivery reference.
     * 
     * @param searchTerm the search term
     * @param limit      maximum results
     * @return list of matching shipments
     */
    List<Shipment> search(String searchTerm, int limit);

    /**
     * Receive a new shipment and create a record.
     * 
     * @param shipment the shipment to receive
     * @return the created shipment with ID
     */
    Shipment receiveShipment(Shipment shipment);

    /**
     * Complete shipment processing after all samples are registered.
     * 
     * @param shipmentId the shipment ID
     * @return the updated shipment
     */
    Shipment completeShipment(Integer shipmentId);

    /**
     * Check if delivery reference already exists.
     *
     * @param deliveryReference the delivery reference to check
     * @return true if exists
     */
    boolean existsByDeliveryReference(String deliveryReference);

    /**
     * Update the documentation status for a shipment.
     *
     * @param shipmentId the shipment ID
     * @param status     the new documentation status
     * @return the updated shipment
     */
    Shipment updateDocumentationStatus(Integer shipmentId,
            org.openelisglobal.biorepository.valueholder.Shipment.DocumentationStatus status);
}
