package org.openelisglobal.inventory.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCountServiceImpl implements InventoryCountService {

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryTransactionService transactionService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public CountResult recordCount(List<CountEntry> entries, String sysUserId) {
        if (entries == null || entries.isEmpty()) {
            return new CountResult(null, 0, 0);
        }

        Timestamp countedAt = new Timestamp(System.currentTimeMillis());
        Set<Long> itemsCounted = new HashSet<>();
        Long sessionReference = null;
        int adjusted = 0;
        int confirmed = 0;

        for (CountEntry entry : entries) {
            if (entry == null || entry.getLotId() == null || entry.getCountedQuantity() == null) {
                continue;
            }
            if (entry.getCountedQuantity() < 0) {
                throw new IllegalArgumentException("A counted quantity cannot be negative");
            }

            InventoryLot lot = findLot(entry.getLotId());
            if (lot == null) {
                throw new IllegalArgumentException("Lot not found: " + entry.getLotId());
            }

            if (lot.getInventoryItem() != null) {
                itemsCounted.add(lot.getInventoryItem().getId());
            }

            double recorded = lot.getCurrentQuantity() == null ? 0d : lot.getCurrentQuantity();
            double counted = entry.getCountedQuantity();
            if (counted == recorded) {
                confirmed++;
                continue;
            }

            if (sessionReference == null) {
                sessionReference = nextSessionReference();
            }

            lot.setCurrentQuantity(counted);
            reconcileStatus(lot, counted);
            lot.setSysUserId(sysUserId);
            lot.setLastupdated(countedAt);
            inventoryLotService.update(lot);

            transactionService.recordTransaction(lot.getId(), TransactionType.ADJUSTMENT, counted - recorded, counted,
                    sessionReference, ReferenceType.ADJUSTMENT.name(), "Physical count", sysUserId);
            adjusted++;
        }

        stampCounted(itemsCounted, countedAt, sysUserId);
        return new CountResult(sessionReference, adjusted, confirmed);
    }

    private void reconcileStatus(InventoryLot lot, double counted) {
        if (counted <= 0) {
            if (lot.getStatus() == LotStatus.ACTIVE || lot.getStatus() == LotStatus.IN_USE) {
                lot.setStatus(LotStatus.CONSUMED);
            }
        } else if (lot.getStatus() == LotStatus.CONSUMED) {
            lot.setStatus(LotStatus.ACTIVE);
        }
    }

    private void stampCounted(Set<Long> itemIds, Timestamp countedAt, String sysUserId) {
        for (Long itemId : itemIds) {
            InventoryItem item = inventoryItemService.get(itemId);
            item.setLastCountedAt(countedAt);
            item.setSysUserId(sysUserId);
            inventoryItemService.update(item);
        }
    }

    // Null for a missing id, where get() throws ObjectNotFoundException.
    private InventoryLot findLot(Long lotId) {
        List<InventoryLot> matches = inventoryLotService.getAllMatching("id", lotId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private Long nextSessionReference() {
        Object value = entityManager.createNativeQuery("SELECT nextval('clinlims.inventory_count_session_seq')")
                .getSingleResult();
        return ((Number) value).longValue();
    }
}
