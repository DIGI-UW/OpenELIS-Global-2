package org.openelisglobal.inventory.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryTagDAO;
import org.openelisglobal.inventory.valueholder.InventoryTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryTagServiceImpl extends AuditableBaseObjectServiceImpl<InventoryTag, Long>
        implements InventoryTagService {

    @Autowired
    private InventoryTagDAO inventoryTagDAO;

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    public InventoryTagServiceImpl() {
        super(InventoryTag.class);
    }

    @Override
    protected InventoryTagDAO getBaseObjectDAO() {
        return inventoryTagDAO;
    }

    /** In-use tags lack a row until deactivated; pre-created tags lack items. */
    @Override
    @Transactional(readOnly = true)
    public List<TagSummary> getDirectory() {
        Map<String, Long> counts = inventoryTagDAO.countItemsPerTag();
        Map<String, InventoryTag> entries = entriesByKey();

        Set<String> names = new LinkedHashSet<>(counts.keySet());
        for (InventoryTag entry : entries.values()) {
            // A row whose spelling matches a tag in use must not appear twice.
            if (names.stream().noneMatch(inUse -> key(inUse).equals(key(entry.getName())))) {
                names.add(entry.getName());
            }
        }

        List<TagSummary> directory = new ArrayList<>();
        for (String name : names) {
            InventoryTag entry = entries.get(key(name));
            directory.add(new TagSummary(name, counts.getOrDefault(name, 0L), entry == null || entry.isActive()));
        }
        directory.sort(Comparator.comparing(TagSummary::getName, String.CASE_INSENSITIVE_ORDER));
        return directory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveTagNames() {
        List<String> active = new ArrayList<>();
        for (TagSummary tag : getDirectory()) {
            if (tag.isActive()) {
                active.add(tag.getName());
            }
        }
        return active;
    }

    @Override
    @Transactional
    public TagSummary createTag(String name, String sysUserId) {
        String trimmed = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            throw new LocalizedValidationException("inventory.tags.error.blank", "A tag needs a name", Map.of());
        }

        // A tag already in use needs no row
        for (String inUse : inventoryItemDAO.getAllTags()) {
            if (key(inUse).equals(key(trimmed))) {
                InventoryTag existing = inventoryTagDAO.getByName(trimmed);
                if (existing != null && !existing.isActive()) {
                    setActive(trimmed, true, sysUserId);
                }
                return new TagSummary(inUse, 0L, true);
            }
        }

        InventoryTag entry = inventoryTagDAO.getByName(trimmed);
        if (entry != null) {
            entry.setIsActive("Y");
            entry.setSysUserId(sysUserId);
            update(entry);
            return new TagSummary(entry.getName(), 0L, true);
        }

        InventoryTag created = new InventoryTag();
        created.setName(trimmed);
        created.setIsActive("Y");
        created.setSysUserId(sysUserId);
        insert(created);
        return new TagSummary(trimmed, 0L, true);
    }

    @Override
    @Transactional
    public void setActive(String name, boolean active, String sysUserId) {
        InventoryTag entry = inventoryTagDAO.getByName(name);
        if (entry == null) {
            // A tag in use has no row until its state first leaves the default
            entry = new InventoryTag();
            entry.setName(name == null ? "" : name.trim().replaceAll("\\s+", " "));
            entry.setIsActive(active ? "Y" : "N");
            entry.setSysUserId(sysUserId);
            insert(entry);
            return;
        }
        entry.setIsActive(active ? "Y" : "N");
        entry.setSysUserId(sysUserId);
        update(entry);
    }

    private Map<String, InventoryTag> entriesByKey() {
        Map<String, InventoryTag> entries = new HashMap<>();
        for (InventoryTag entry : inventoryTagDAO.getAllTags()) {
            entries.putIfAbsent(key(entry.getName()), entry);
        }
        return entries;
    }

    private static String key(String tag) {
        return tag == null ? "" : tag.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
