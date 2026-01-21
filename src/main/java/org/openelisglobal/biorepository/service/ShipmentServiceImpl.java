package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.biorepository.dao.ShipmentDAO;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.DocumentationStatus;
import org.openelisglobal.biorepository.valueholder.Shipment.ShipmentStatus;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Shipment entity operations.
 */
@Service
public class ShipmentServiceImpl extends AuditableBaseObjectServiceImpl<Shipment, Integer> implements ShipmentService {

    @Autowired
    protected ShipmentDAO baseObjectDAO;

    ShipmentServiceImpl() {
        super(Shipment.class);
    }

    @Override
    protected ShipmentDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getByDeliveryReference(String deliveryReference) {
        return baseObjectDAO.getByDeliveryReference(deliveryReference);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> getByStatus(ShipmentStatus status) {
        return baseObjectDAO.getByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> getByDateRange(Timestamp startDate, Timestamp endDate) {
        return baseObjectDAO.getByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> getByReceiverId(Integer receiverUserId) {
        return baseObjectDAO.getByReceiverId(receiverUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> getRecentShipments(int offset, int limit) {
        return baseObjectDAO.getRecentShipments(offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(ShipmentStatus status) {
        return baseObjectDAO.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> search(String searchTerm, int limit) {
        return baseObjectDAO.search(searchTerm, limit);
    }

    @Override
    @Transactional
    public Shipment receiveShipment(Shipment shipment) {
        if (shipment.getDeliveryReference() != null && existsByDeliveryReference(shipment.getDeliveryReference())) {
            throw new LIMSDuplicateRecordException(
                    "Shipment with delivery reference already exists: " + shipment.getDeliveryReference());
        }

        shipment.setStatus(ShipmentStatus.RECEIVED);
        if (shipment.getReceptionTimestamp() == null) {
            shipment.setReceptionTimestamp(new Timestamp(System.currentTimeMillis()));
        }

        return save(shipment);
    }

    @Override
    @Transactional
    public Shipment completeShipment(Integer shipmentId) {
        Shipment shipment = get(shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment not found: " + shipmentId);
        }

        shipment.setStatus(ShipmentStatus.COMPLETED);
        return update(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByDeliveryReference(String deliveryReference) {
        return getByDeliveryReference(deliveryReference) != null;
    }

    @Override
    public Integer insert(Shipment shipment) {
        if (shipment.getDeliveryReference() != null && existsByDeliveryReference(shipment.getDeliveryReference())) {
            throw new LIMSDuplicateRecordException(
                    "Shipment with delivery reference already exists: " + shipment.getDeliveryReference());
        }
        return super.insert(shipment);
    }

    @Override
    public Shipment save(Shipment shipment) {
        if (shipment.getDeliveryReference() != null && shipment.getId() == null
                && existsByDeliveryReference(shipment.getDeliveryReference())) {
            throw new LIMSDuplicateRecordException(
                    "Shipment with delivery reference already exists: " + shipment.getDeliveryReference());
        }
        return super.save(shipment);
    }

    @Override
    @Transactional
    public Shipment updateDocumentationStatus(Integer shipmentId, DocumentationStatus status) {
        Shipment shipment = get(shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment not found: " + shipmentId);
        }

        shipment.setDocumentationStatus(status);
        return update(shipment);
    }
}
