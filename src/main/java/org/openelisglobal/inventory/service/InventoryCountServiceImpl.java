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
            // Abandoning a count is not an event. Nothing is written, including the stamp:
            // an item nobody counted has not been counted.
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
                // Counted and correct. Worth recording that it was checked, not worth a
                // transaction saying nothing moved.
                confirmed++;
                continue;
            }

            if (sessionReference == null) {
                // Minted on the first discrepancy rather than up front, so a count that
                // finds nothing wrong does not burn a session number.
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

    /**
     * Brings the lot's status back in line with what the count found.
     *
     * <p>
     * Writing the quantity alone left the count stranding the very stock it
     * discovered: a lot the books had run down to zero stays CONSUMED, which every
     * on-hand sum, FEFO draw and availability check excludes, so found stock stayed
     * invisible. The same in reverse, a lot counted down to nothing is consumed
     * whatever the books said. This is the rule adjustLotQuantity already applies
     * to a manual correction; a physical count is the same kind of correction.
     *
     * <p>
     * Only CONSUMED is reversed. Disposal is a decision and expiry is a date:
     * neither is undone by finding the box still on the shelf.
     */
    private void reconcileStatus(InventoryLot lot, double counted) {
        if (counted <= 0) {
            if (lot.getStatus() == LotStatus.ACTIVE || lot.getStatus() == LotStatus.IN_USE) {
                lot.setStatus(LotStatus.CONSUMED);
            }
        } else if (lot.getStatus() == LotStatus.CONSUMED) {
            lot.setStatus(LotStatus.ACTIVE);
        }
    }

    /**
     * Every item the session touched is stamped, whether or not its lots moved.
     * "Counted and correct" is the answer the stamp exists to record.
     */
    private void stampCounted(Set<Long> itemIds, Timestamp countedAt, String sysUserId) {
        for (Long itemId : itemIds) {
            InventoryItem item = inventoryItemService.get(itemId);
            if (item == null) {
                continue;
            }
            item.setLastCountedAt(countedAt);
            item.setSysUserId(sysUserId);
            inventoryItemService.update(item);
        }
    }

    /**
     * {@code get} throws Hibernate's own ObjectNotFoundException for an id that is
     * not there, which escapes this method as a 500 rather than as the bad request
     * it is — and, thrown mid-session, reports nothing about which entry was wrong.
     */
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
