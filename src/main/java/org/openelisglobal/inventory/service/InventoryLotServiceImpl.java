package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.CodeGenerator;
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

    // Matches the inventory_lot.barcode column length.
    private static final int BARCODE_MAX_LENGTH = 100;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryTransactionService transactionService;

    public InventoryLotServiceImpl() {
        super(InventoryLot.class);
    }

    @Override
    protected InventoryLotDAO getBaseObjectDAO() {
        return inventoryLotDAO;
    }

    @Override
    @Transactional
    public Long insert(InventoryLot lot) {
        lot.setBarcode(resolveBarcode(lot));
        return super.insert(lot);
    }

    /**
     * The lot barcode is the lab's own scannable label, so the system mints one
     * when the user does not supply it. Mirrors InventoryItemServiceImpl's code
     * handling: generate from a readable seed when blank, normalize an explicit
     * value otherwise, and reject a duplicate rather than letting the UNIQUE index
     * surface a raw constraint error.
     */
    private String resolveBarcode(InventoryLot lot) {
        String supplied = lot.getBarcode();
        String barcode = (supplied == null || supplied.trim().isEmpty())
                ? CodeGenerator.generateFromName(barcodeSeed(lot), BARCODE_MAX_LENGTH, "LOT", this::barcodeExists)
                : CodeGenerator.normalize(supplied, BARCODE_MAX_LENGTH);
        if (barcodeExists(barcode)) {
            throw new LocalizedValidationException("inventory.lot.error.duplicateBarcode",
                    "Inventory lot barcode already exists: " + barcode, Map.of("barcode", barcode));
        }
        return barcode;
    }

    /**
     * Item code plus lot number, so a human can still identify the lot when the
     * printed barcode is damaged. Falls back to whichever half is available when
     * the other is missing.
     */
    private String barcodeSeed(InventoryLot lot) {
        String itemCode = lot.getInventoryItem() != null ? lot.getInventoryItem().getCode() : null;
        String lotNumber = lot.getLotNumber();
        if (itemCode == null || itemCode.trim().isEmpty()) {
            return lotNumber;
        }
        if (lotNumber == null || lotNumber.trim().isEmpty()) {
            return itemCode;
        }
        // An auto-generated lot number is already "{itemCode}-{yyyyMMdd}" (see
        // InventoryManagementServiceImpl.generateLotNumber), so prefixing the item
        // code again repeats it: FOR_TESTING_FOR_TESTING_20260810.
        if (toComparable(lotNumber).startsWith(toComparable(itemCode))) {
            return lotNumber;
        }
        return itemCode + "_" + lotNumber;
    }

    /** Same shape CodeGenerator applies, so the prefix check sees final form. */
    private static String toComparable(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private boolean barcodeExists(String barcode) {
        return inventoryLotDAO.getByBarcode(barcode) != null;
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
    public InventoryLot getByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return null;
        }
        return inventoryLotDAO.getByBarcode(barcode.trim());
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

        // Update status to IN_USE
        lot.setStatus(LotStatus.IN_USE);
        lot.setDateOpened(openedDate);

        // Calculate expiry after opening for reagents
        InventoryItem item = lot.getInventoryItem();
        if (item != null && item.isReagent() && item.getStabilityAfterOpening() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(openedDate);
            cal.add(Calendar.DAY_OF_MONTH, item.getStabilityAfterOpening());
            lot.setCalculatedExpiryAfterOpening(new Timestamp(cal.getTimeInMillis()));
        }

        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        update(lot);

        // Record transaction
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
        lot.setQcStatus(qcStatus);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        update(lot);

        // Build transaction notes
        String transactionNotes = buildQCStatusNotes(oldStatus, qcStatus, notes);

        // Record transaction
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

        lot.setStatus(status);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
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

        lot.setCurrentQuantity(newQuantity);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));

        // Update status based on quantity
        if (newQuantity == 0 && lot.getStatus() != LotStatus.DISPOSED) {
            lot.setStatus(LotStatus.CONSUMED);
        }

        update(lot);

        // Record transaction
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
        lot.setCurrentQuantity(0.0);
        lot.setStatus(LotStatus.DISPOSED);
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        update(lot);

        // Record transaction with reason and notes
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
            lot.setStatus(LotStatus.EXPIRED);
            lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(lot);
            count++;

            // Record transaction
            transactionService.recordTransaction(lot.getId(), TransactionType.MANUAL, 0.0, lot.getCurrentQuantity(),
                    null, null, "Automatically marked as expired", "SYSTEM");
        }

        return count;
    }
}
