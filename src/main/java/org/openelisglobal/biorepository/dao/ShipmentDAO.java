package org.openelisglobal.biorepository.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.ShipmentStatus;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for Shipment entity operations.
 */
public interface ShipmentDAO extends BaseDAO<Shipment, Integer> {

    /**
     * Find shipment by delivery reference.
     * 
     * @param deliveryReference the tracking/reference number
     * @return shipment or null if not found
     */
    Shipment getByDeliveryReference(String deliveryReference);

    /**
     * Find all shipments with a given status.
     * 
     * @param status the shipment status
     * @return list of shipments ordered by reception timestamp descending
     */
    List<Shipment> getByStatus(ShipmentStatus status);

    /**
     * Find shipments received within a date range.
     * 
     * @param startDate start of the date range
     * @param endDate   end of the date range
     * @return list of shipments ordered by reception timestamp descending
     */
    List<Shipment> getByDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Find shipments received by a specific user.
     * 
     * @param receiverUserId the receiving user's ID
     * @return list of shipments ordered by reception timestamp descending
     */
    List<Shipment> getByReceiverId(Integer receiverUserId);

    /**
     * Find recent shipments with pagination.
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
     * Search shipments by sender name or delivery reference.
     * 
     * @param searchTerm the search term
     * @param limit      maximum results
     * @return list of matching shipments
     */
    List<Shipment> search(String searchTerm, int limit);
}
