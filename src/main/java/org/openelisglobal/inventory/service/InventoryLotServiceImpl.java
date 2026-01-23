package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryLotServiceImpl extends AuditableBaseObjectServiceImpl<InventoryLot, Long>
        implements InventoryLotService {

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryTransactionService transactionService;

    public InventoryLotServiceImpl() {
        super(InventoryLot.class);
        this.auditTrailLog = true; // Enable generic audit trail
    }

    @Override
    protected InventoryLotDAO getBaseObjectDAO() {
        return inventoryLotDAO;
    }

    @Override
    @Transactional
    public Long insert(InventoryLot lot) {
        // Ensure UUID is set before insert
        if (lot.getFhirUuid() == null) {
            lot.setFhirUuid(UUID.randomUUID());
        }

        // Audit logging is automatic via auditTrailLog = true in constructor
        return super.insert(lot);
    }

    @Override
    @Transactional
    public InventoryLot update(InventoryLot lot) {
        // Audit logging is automatic via auditTrailLog = true in constructor
        return super.update(lot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getAvailableLotsByItemFEFO(Long itemId) {
        return inventoryLotDAO.getAvailableLotsByItemFEFO(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getByInventoryItemId(Long itemId) {
        return inventoryLotDAO.getByInventoryItemId(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getExpiringLots(int daysFromNow) {
        return inventoryLotDAO.getExpiringLots(daysFromNow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getExpiredActiveLots() {
        return inventoryLotDAO.getExpiredActiveLots();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLot getByLotNumber(String lotNumber) {
        return inventoryLotDAO.getByLotNumber(lotNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLot getByFhirUuid(String fhirUuid) {
        return inventoryLotDAO.getByFhirUuid(fhirUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalCurrentQuantity(Long itemId) {
        Integer total = inventoryLotDAO.getTotalCurrentQuantity(itemId);
        return total != null ? total.doubleValue() : 0.0;
    }

    @Override
    @Transactional
    public InventoryLot openLot(Long lotId, Timestamp openedDate, String sysUserId) {
        InventoryLot lot = get(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Lot not found: " + lotId);
        }

        if (lot.getStatus() != LotStatus.ACTIVE) {
            throw new IllegalStateException("Can only open lots with ACTIVE status");
        }

        // Detach from session so audit can compare properly
        inventoryLotDAO.evict(lot);

        lot.setStatus(LotStatus.IN_USE);
        lot.setDateOpened(openedDate);

        InventoryItem item = lot.getInventoryItem();
        if (item != null && item.isReagent() && item.getStabilityAfterOpening() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(openedDate);
            cal.add(Calendar.DAY_OF_MONTH, item.getStabilityAfterOpening());
            lot.setCalculatedExpiryAfterOpening(new Timestamp(cal.getTimeInMillis()));
        }

        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        // Audit logging is automatic via update() -> auditTrailLog
        update(lot);

        transactionService.recordTransaction(lotId, TransactionType.OPENING, 0.0, // No quantity change
                lot.getCurrentQuantity(), null, null, "Lot opened", sysUserId);

        return lot;
    }

    @Override
    @Transactional
    public InventoryLot updateQCStatus(Long lotId, QCStatus qcStatus, String notes, String sysUserId) {
        InventoryLot lot = get(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Lot not found: " + lotId);
        }

        QCStatus oldStatus = lot.getQcStatus();

        // Detach from session so audit can compare properly
        inventoryLotDAO.evict(lot);

        lot.setQcStatus(qcStatus);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        // Audit logging is automatic via update() -> auditTrailLog
        update(lot);

        String transactionNotes = buildQCStatusNotes(oldStatus, qcStatus, notes);

        transactionService.recordTransaction(lotId, TransactionType.QC_TEST, 0.0, // No quantity change
                lot.getCurrentQuantity(), null, null, transactionNotes, sysUserId);

        return lot;
    }

    @Override
    @Transactional
    public InventoryLot updateLotStatus(Long lotId, LotStatus status, String sysUserId) {
        InventoryLot lot = get(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Lot not found: " + lotId);
        }

        // Detach from session so audit can compare properly
        inventoryLotDAO.evict(lot);

        lot.setStatus(status);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        // Audit logging is automatic via update() -> auditTrailLog
        update(lot);

        return lot;
    }

    @Override
    @Transactional
    public InventoryLot adjustLotQuantity(Long lotId, Double newQuantity, String reason, String sysUserId) {
        InventoryLot lot = get(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Lot not found: " + lotId);
        }

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        Double oldQuantity = lot.getCurrentQuantity();
        Double quantityChange = newQuantity - oldQuantity;

        // Detach from session so audit can compare properly
        inventoryLotDAO.evict(lot);

        lot.setCurrentQuantity(newQuantity);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));

        if (newQuantity == 0 && lot.getStatus() != LotStatus.DISPOSED) {
            lot.setStatus(LotStatus.CONSUMED);
        }

        // Audit logging is automatic via update() -> auditTrailLog
        update(lot);

        transactionService.recordTransaction(lotId, TransactionType.ADJUSTMENT, quantityChange, newQuantity, null, null,
                reason != null ? reason : "Manual quantity adjustment", sysUserId);

        return lot;
    }

    @Override
    @Transactional
    public InventoryLot disposeLot(Long lotId, String reason, String notes, String sysUserId) {
        InventoryLot lot = get(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Lot not found: " + lotId);
        }

        Double quantityDisposed = lot.getCurrentQuantity();

        // Detach from session so audit can compare properly
        inventoryLotDAO.evict(lot);

        lot.setCurrentQuantity(0.0);
        lot.setStatus(LotStatus.DISPOSED);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        // Audit logging is automatic via update() -> auditTrailLog
        update(lot);

        String transactionNotes = buildDisposalNotes(reason, notes);
        transactionService.recordTransaction(lotId, TransactionType.DISPOSAL, -quantityDisposed, 0.0, null, null,
                transactionNotes, sysUserId);

        return lot;
    }

    private String buildDisposalNotes(String reason, String notes) {
        StringBuilder sb = new StringBuilder();
        if (reason != null && !reason.trim().isEmpty()) {
            sb.append("Reason: ").append(reason);
        }
        if (notes != null && !notes.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            sb.append("Notes: ").append(notes);
        }
        return sb.length() > 0 ? sb.toString() : "Lot disposed";
    }

    private String buildQCStatusNotes(QCStatus oldStatus, QCStatus newStatus, String notes) {
        StringBuilder sb = new StringBuilder();
        sb.append("QC status changed from ").append(oldStatus).append(" to ").append(newStatus);
        if (notes != null && !notes.trim().isEmpty()) {
            sb.append(". Notes: ").append(notes);
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLotExpired(Long lotId) {
        InventoryLot lot = get(lotId);
        return lot != null && lot.isExpired();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLotAvailable(Long lotId) {
        InventoryLot lot = get(lotId);
        return lot != null && lot.isAvailableForUse();
    }

    @Override
    @Transactional
    public int processExpiredLots() {
        List<InventoryLot> expiredLots = getExpiredActiveLots();
        int count = 0;

        for (InventoryLot lot : expiredLots) {
            // Detach from session so audit can compare properly
            inventoryLotDAO.evict(lot);

            lot.setStatus(LotStatus.EXPIRED);
            lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(lot);
            count++;

            transactionService.recordTransaction(lot.getId(), TransactionType.MANUAL, 0.0, lot.getCurrentQuantity(),
                    null, null, "Automatically marked as expired", "SYSTEM");
        }

        return count;
    }

    /**
     * Creates a shallow clone of the lot for audit "before" state. We create a new
     * instance to avoid Hibernate session issues when comparing before/after
     * states.
     */
    private InventoryLot cloneLot(InventoryLot lot) {
        InventoryLot clone = new InventoryLot();
        clone.setId(lot.getId());
        clone.setLotNumber(lot.getLotNumber());
        clone.setInventoryItem(lot.getInventoryItem());
        // Unified storage location fields
        clone.setLocationId(lot.getLocationId());
        clone.setLocationType(lot.getLocationType());
        clone.setPositionCoordinate(lot.getPositionCoordinate());
        clone.setStoragePath(lot.getStoragePath());
        clone.setInitialQuantity(lot.getInitialQuantity());
        clone.setCurrentQuantity(lot.getCurrentQuantity());
        clone.setStatus(lot.getStatus());
        clone.setQcStatus(lot.getQcStatus());
        clone.setReceiptDate(lot.getReceiptDate());
        clone.setExpirationDate(lot.getExpirationDate());
        clone.setDateOpened(lot.getDateOpened());
        clone.setCalculatedExpiryAfterOpening(lot.getCalculatedExpiryAfterOpening());
        clone.setUnitSize(lot.getUnitSize());
        clone.setSysUserId(lot.getSysUserId());
        clone.setLastupdated(lot.getLastupdated());
        return clone;
    }

    @Override
    @Transactional
    public InventoryLot updateStorageLocation(Long lotId, Integer locationId, String locationType,
            String positionCoordinate, String storagePath, String sysUserId) {
        InventoryLot lot = get(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Lot not found: " + lotId);
        }

        // Validate location type
        if (locationType != null && !locationType.isEmpty()) {
            if (!locationType.equals("room") && !locationType.equals("device") && !locationType.equals("shelf")
                    && !locationType.equals("rack") && !locationType.equals("box") && !locationType.equals("general")) {
                throw new IllegalArgumentException("Invalid location type: " + locationType
                        + ". Must be one of: 'room', 'device', 'shelf', 'rack', 'box', 'general'");
            }
        }

        // Detach from session so audit can compare properly
        inventoryLotDAO.evict(lot);

        // Update unified storage location fields
        lot.setLocationId(locationId);
        lot.setLocationType(locationType);
        lot.setPositionCoordinate(positionCoordinate);
        lot.setStoragePath(storagePath);

        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));

        // Audit logging is automatic via update() -> auditTrailLog
        update(lot);

        transactionService.recordTransaction(lotId, TransactionType.MANUAL, 0.0, lot.getCurrentQuantity(), null, null,
                "Storage location updated to: " + (storagePath != null ? storagePath : locationType + " " + locationId),
                sysUserId);

        return lot;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getByUnifiedLocation(Integer locationId, String locationType) {
        return inventoryLotDAO.getByUnifiedLocation(locationId, locationType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getPagedLots(int limit, int offset, String sortBy, String sortOrder, String itemType,
            LotStatus status, String searchTerm) {
        // Validate and constrain limit to prevent performance issues
        if (limit > 1000) {
            limit = 1000;
        }
        if (limit < 1) {
            limit = 20; // Default page size
        }

        // Ensure offset is non-negative
        if (offset < 0) {
            offset = 0;
        }

        return inventoryLotDAO.getPagedLots(limit, offset, sortBy, sortOrder, itemType, status, searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPagedLotsCount(String itemType, LotStatus status, String searchTerm) {
        return inventoryLotDAO.getPagedLotsCount(itemType, status, searchTerm);
    }
}
