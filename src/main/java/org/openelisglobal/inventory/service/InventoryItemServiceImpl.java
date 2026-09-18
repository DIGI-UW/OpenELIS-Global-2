package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.CodeGenerator;
import org.openelisglobal.inventory.dao.InventoryItemCodeSequenceDAO;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, Long>
        implements InventoryItemService {

    // inventory_item.code is VARCHAR(64) — see 070-inventory-item-code.xml
    private static final int CODE_MAX_LENGTH = 64;

    // Each miss burns a counter value; a legacy or typed code on the next slot is
    // rare.
    private static final int MAX_GENERATE_ATTEMPTS = 100;
    // inventory_item_tag.tag is VARCHAR(255)
    private static final int TAG_MAX_LENGTH = 255;

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    @Autowired
    private InventoryItemCodeSequenceDAO codeSequenceDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    public InventoryItemServiceImpl() {
        super(InventoryItem.class);
    }

    @Override
    protected InventoryItemDAO getBaseObjectDAO() {
        return inventoryItemDAO;
    }

    @Override
    @Transactional
    public Long insert(InventoryItem item) {
        item.setCode(resolveCode(item));
        item.setTags(canonicalizeTags(item.getTags()));
        return super.insert(item);
    }

    @Override
    @Transactional
    public InventoryItem save(InventoryItem item) {
        item.setTags(canonicalizeTags(item.getTags()));
        return super.save(item);
    }

    @Override
    @Transactional
    public InventoryItem update(InventoryItem item) {
        // The item editor's PUT calls update(), not save().
        item.setTags(canonicalizeTags(item.getTags()));
        return super.update(item);
    }

    /** Adopts a stored spelling that differs only by case or spacing. */
    private Set<String> canonicalizeTags(Set<String> supplied) {
        Set<String> result = new LinkedHashSet<>();
        if (supplied == null || supplied.isEmpty()) {
            return result;
        }
        List<String> tidied = new ArrayList<>();
        Set<String> wanted = new LinkedHashSet<>();
        for (String tag : supplied) {
            String tidy = tidy(tag);
            if (tidy != null) {
                tidied.add(tidy);
                wanted.add(tagKey(tidy));
            }
        }
        Map<String, String> existingByKey = new HashMap<>();
        for (String known : inventoryItemDAO.getTagsMatching(wanted)) {
            existingByKey.putIfAbsent(tagKey(known), known);
        }
        for (String trimmed : tidied) {
            String canonical = existingByKey.get(tagKey(trimmed));
            String chosen = canonical == null ? trimmed : canonical;
            // Two spellings of one tag would collide on the (item_id, tag) primary key.
            if (result.stream().noneMatch(kept -> tagKey(kept).equals(tagKey(chosen)))) {
                result.add(chosen);
            }
            existingByKey.putIfAbsent(tagKey(chosen), chosen);
        }
        return result;
    }

    private static String tagKey(String tag) {
        return tag.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** A tag as it would be stored, or null when there is nothing left of it. */
    private static String tidy(String tag) {
        if (tag == null) {
            return null;
        }
        String trimmed = tag.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > TAG_MAX_LENGTH ? trimmed.substring(0, TAG_MAX_LENGTH) : trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return inventoryItemDAO.getAllTags();
    }

    private String resolveCode(InventoryItem item) {
        String supplied = item.getCode();
        if (supplied == null || supplied.trim().isEmpty()) {
            return generateCode(item.getName());
        }
        String code = CodeGenerator.normalize(supplied, CODE_MAX_LENGTH);
        if (codeExists(code)) {
            throw new LocalizedValidationException("inventory.item.error.duplicateCode",
                    "Inventory item code already exists: " + code, Map.of("code", code));
        }
        return code;
    }

    /**
     * Prefix + zero-padded per-prefix counter (PAR-500MG-001), skipping slots a
     * stored code already holds.
     */
    private String generateCode(String name) {
        String prefix = CodeGenerator.prefixFor(name);
        for (int attempt = 0; attempt < MAX_GENERATE_ATTEMPTS; attempt++) {
            String code = prefix + "-" + String.format("%03d", codeSequenceDAO.nextValue(prefix));
            if (!codeExists(code)) {
                return code;
            }
        }
        throw new LocalizedValidationException("inventory.item.error.codeGenerationExhausted",
                "Could not find a free inventory item code for prefix " + prefix, Map.of("prefix", prefix));
    }

    private boolean codeExists(String code) {
        return inventoryItemDAO.getByCode(code) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllActive() {
        return inventoryItemDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getByCategory(String category) {
        return inventoryItemDAO.getByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> searchByName(String searchTerm) {
        return inventoryItemDAO.searchByName(searchTerm);
    }

    /**
     * Strictly below threshold on {@link InventoryLot#countsAsAvailableStock()}
     * stock, the rule the Low Stock report shares; one lot query per active item.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() {
        return inventoryItemDAO.getAllActive().stream().filter(item -> item.getLowStockThreshold() != null)
                .filter(item -> availableQuantity(item.getId()) < item.getLowStockThreshold())
                .collect(Collectors.toList());
    }

    private double availableQuantity(Long itemId) {
        return inventoryLotDAO.getByInventoryItemId(itemId).stream().filter(InventoryLot::countsAsAvailableStock)
                .mapToDouble(InventoryLot::getCurrentQuantity).sum();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByFhirUuid(String fhirUuid) {
        return inventoryItemDAO.getByFhirUuid(fhirUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalCurrentStock(Long itemId) {
        Integer total = inventoryLotDAO.getTotalCurrentQuantity(itemId);
        return total != null ? total.doubleValue() : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInStock(Long itemId) {
        List<org.openelisglobal.inventory.valueholder.InventoryLot> availableLots = inventoryLotDAO
                .getAvailableLotsByItemFEFO(itemId);
        return availableLots != null && !availableLots.isEmpty();
    }

    @Override
    @Transactional
    public void deactivateItem(Long itemId, String sysUserId) {
        InventoryItem item = get(itemId);
        if (item != null) {
            item.setIsActive("N");
            item.setSysUserId(sysUserId);
            item.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(item);
        }
    }

    @Override
    @Transactional
    public void activateItem(Long itemId, String sysUserId) {
        InventoryItem item = get(itemId);
        if (item != null) {
            item.setIsActive("Y");
            item.setSysUserId(sysUserId);
            item.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(item);
        }
    }

    @Override
    @Transactional
    public int markOrdered(List<Long> itemIds, String note, LocalDate expectedDate, String sysUserId) {
        if (itemIds == null || itemIds.isEmpty()) {
            return 0;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        int changed = 0;
        for (Long itemId : itemIds) {
            InventoryItem item = get(itemId);
            if (item == null) {
                continue;
            }
            // Keep the first stamp on a re-mark: it anchors the order-to-receipt lead time.
            if (item.getOrderedAt() == null) {
                item.setOrderedAt(now);
            }
            item.setOrderNote(note);
            item.setOrderExpectedDate(expectedDate);
            item.setSysUserId(sysUserId);
            update(item);
            changed++;
        }
        return changed;
    }

    @Override
    @Transactional
    public int clearOrdered(List<Long> itemIds, String sysUserId) {
        if (itemIds == null || itemIds.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (Long itemId : itemIds) {
            InventoryItem item = get(itemId);
            if (item == null || item.getOrderedAt() == null) {
                continue;
            }
            item.setOrderedAt(null);
            item.setOrderNote(null);
            item.setOrderExpectedDate(null);
            item.setSysUserId(sysUserId);
            update(item);
            changed++;
        }
        return changed;
    }
}
