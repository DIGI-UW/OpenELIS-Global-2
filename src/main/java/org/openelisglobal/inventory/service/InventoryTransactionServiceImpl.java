package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.dao.InventoryTransactionDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryTransactionServiceImpl extends AuditableBaseObjectServiceImpl<InventoryTransaction, Long>
        implements InventoryTransactionService {

    @Autowired
    private InventoryTransactionDAO inventoryTransactionDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    public InventoryTransactionServiceImpl() {
        super(InventoryTransaction.class);
    }

    @Override
    protected InventoryTransactionDAO getBaseObjectDAO() {
        return inventoryTransactionDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByLotId(Long lotId) {
        return inventoryTransactionDAO.getByLotId(lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByTransactionType(TransactionType transactionType) {
        return inventoryTransactionDAO.getByTransactionType(transactionType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByDateRange(Timestamp startDate, Timestamp endDate) {
        return inventoryTransactionDAO.getByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByReference(Long referenceId, String referenceType) {
        return inventoryTransactionDAO.getByReference(referenceId, referenceType);
    }

    @Override
    @Transactional
    public InventoryTransaction recordTransaction(Long lotId, TransactionType transactionType, Double quantityChange,
            Double quantityAfter, Long referenceId, String referenceType, String notes, String sysUserId) {

        InventoryLot lot = inventoryLotDAO.get(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setLot(lot);
        transaction.setTransactionType(transactionType);
        transaction.setQuantityChange(quantityChange);
        transaction.setQuantityAfter(quantityAfter);
        transaction.setReferenceId(referenceId);
        // Convert String to ReferenceType enum if provided
        if (referenceType != null) {
            transaction.setReferenceType(ReferenceType.valueOf(referenceType));
        }
        transaction.setNotes(notes);
        transaction.setTransactionDate(new Timestamp(System.currentTimeMillis()));
        transaction.setSysUserId(sysUserId);
        // Set performedByUser from sysUserId
        transaction.setPerformedByUser(Integer.valueOf(sysUserId));

        Long id = insert(transaction);
        return get(id);
    }
}
