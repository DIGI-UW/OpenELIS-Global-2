package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;

public interface InventoryLotDAO extends BaseDAO<InventoryLot, Long> {

    /**
     * Get all lots for a specific inventory item
     */
    List<InventoryLot> getByInventoryItemId(Long itemId) throws LIMSRuntimeException;

    /**
     * Get available lots by item ID, sorted by expiration date (FEFO) Only includes
     * lots with status ACTIVE/IN_USE, QC PASSED, and currentQuantity > 0
     */
    List<InventoryLot> getAvailableLotsByItemFEFO(Long itemId) throws LIMSRuntimeException;

    /**
     * Get lots expiring within specified days
     */
    List<InventoryLot> getExpiringLots(int daysAhead) throws LIMSRuntimeException;

    /**
     * Get expired active lots (expiration date < today but status still ACTIVE)
     */
    List<InventoryLot> getExpiredActiveLots() throws LIMSRuntimeException;

    /**
     * Get lot by lot number
     */
    InventoryLot getByLotNumber(String lotNumber) throws LIMSRuntimeException;

    /**
     * Get lot by barcode
     */
    InventoryLot getByBarcode(String barcode) throws LIMSRuntimeException;

    /**
     * Get lots by storage location
     */
    List<InventoryLot> getByStorageLocationId(Long locationId) throws LIMSRuntimeException;

    /**
     * Get lots by QC status
     */
    List<InventoryLot> getByQCStatus(QCStatus qcStatus) throws LIMSRuntimeException;

    /**
     * Get lots by lot status
     */
    List<InventoryLot> getByStatus(LotStatus status) throws LIMSRuntimeException;

    /**
     * Get total current quantity for an inventory item across all lots
     */
    Integer getTotalCurrentQuantity(Long itemId) throws LIMSRuntimeException;

    /**
     * Get lot by FHIR UUID
     */
    InventoryLot getByFhirUuid(String fhirUuid) throws LIMSRuntimeException;
}
