package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.dao.ShipmentDAO;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for shipment tracking operations
 */
@Service
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentServiceImpl.class);

    @Autowired
    private ShipmentDAO shipmentDAO;

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentById(Integer id) {
        try {
            return shipmentDAO.get(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error getting shipment by ID", e);
            throw new LIMSRuntimeException("Error getting shipment by ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentByShippingBoxId(Integer shippingBoxId) {
        try {
            return shipmentDAO.findByShippingBoxId(shippingBoxId);
        } catch (Exception e) {
            logger.error("Error getting shipment by shipping box ID", e);
            throw new LIMSRuntimeException("Error getting shipment by shipping box ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentByTrackingNumber(String trackingNumber) {
        try {
            return shipmentDAO.findByTrackingNumber(trackingNumber);
        } catch (Exception e) {
            logger.error("Error getting shipment by tracking number", e);
            throw new LIMSRuntimeException("Error getting shipment by tracking number", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByStatus(ShipmentStatus status) {
        try {
            return shipmentDAO.findByStatus(status);
        } catch (Exception e) {
            logger.error("Error getting shipments by status", e);
            throw new LIMSRuntimeException("Error getting shipments by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByCourier(String courier) {
        try {
            return shipmentDAO.findByCourier(courier);
        } catch (Exception e) {
            logger.error("Error getting shipments by courier", e);
            throw new LIMSRuntimeException("Error getting shipments by courier", e);
        }
    }

    @Override
    public Shipment createShipment(Shipment shipment) {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            shipment.setLastupdated(now);
            if (shipment.getStatus() == null) {
                shipment.setStatus(ShipmentStatus.PENDING);
            }
            // Persist the audit user to the mapped DB column.
            // sysUserId (from BaseObject) is @Transient and never written by Hibernate.
            if (shipment.getSystemUserId() == null) {
                String sysUserId = shipment.getSysUserId();
                if (sysUserId == null || sysUserId.isBlank()) {
                    throw new LIMSRuntimeException("System user ID is required for audit but was not provided.");
                }
                try {
                    shipment.setSystemUserId(Integer.parseInt(sysUserId));
                } catch (NumberFormatException e) {
                    throw new LIMSRuntimeException("Malformed system user ID provided: " + sysUserId, e);
                }
            }

            Integer id = shipmentDAO.insert(shipment);
            logger.info("Created shipment with ID: {}", id);
            return shipmentDAO.get(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error creating shipment", e);
            throw new LIMSRuntimeException("Error creating shipment", e);
        }
    }

    @Override
    public Shipment updateShipment(Shipment shipment) {
        try {
            shipment.setLastupdated(new Timestamp(System.currentTimeMillis()));

            // Prevent writing NULL to the mapped sys_user_id column when a detached
            // entity is passed from the controller without it.
            if (shipment.getSystemUserId() == null && shipment.getId() != null) {
                Shipment existing = shipmentDAO.get(shipment.getId()).orElseThrow(
                        () -> new IllegalArgumentException("Shipment not found with ID: " + shipment.getId()));
                shipment.setSystemUserId(existing.getSystemUserId());
            }

            shipmentDAO.update(shipment);
            logger.info("Updated shipment with ID: {}", shipment.getId());
            return shipment;
        } catch (Exception e) {
            logger.error("Error updating shipment", e);
            throw new LIMSRuntimeException("Error updating shipment", e);
        }
    }

    @Override
    public Shipment updateShipmentStatus(Integer id, ShipmentStatus newStatus) {
        try {
            Shipment shipment = shipmentDAO.get(id)
                    .orElseThrow(() -> new IllegalArgumentException("Shipment not found with ID: " + id));

            shipment.setStatus(newStatus);
            shipment.setLastupdated(new Timestamp(System.currentTimeMillis()));

            // Update date fields based on status
            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (newStatus == ShipmentStatus.IN_TRANSIT && shipment.getShippedDate() == null) {
                shipment.setShippedDate(now);
            } else if (newStatus == ShipmentStatus.DELIVERED) {
                shipment.setActualDeliveryDate(now);
            }

            shipmentDAO.update(shipment);
            logger.info("Updated shipment {} status to {}", id, newStatus);
            return shipment;
        } catch (Exception e) {
            logger.error("Error updating shipment status", e);
            throw new LIMSRuntimeException("Error updating shipment status", e);
        }
    }

    @Override
    public void deleteShipment(Integer id) {
        try {
            Shipment shipment = shipmentDAO.get(id).orElse(null);
            if (shipment != null) {
                shipmentDAO.delete(shipment);
                logger.info("Deleted shipment with ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting shipment", e);
            throw new LIMSRuntimeException("Error deleting shipment", e);
        }
    }
}
