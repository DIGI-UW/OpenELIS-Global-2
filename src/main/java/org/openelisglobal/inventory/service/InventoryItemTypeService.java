package org.openelisglobal.inventory.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryItemType;

public interface InventoryItemTypeService extends BaseObjectService<InventoryItemType, Long> {

    /** All types (any status), sorted by sort_order — for the admin list. */
    List<InventoryItemType> getAllOrderedBySortOrder();

    /** Active-only types, sorted by sort_order — for the Item Add dropdown. */
    List<InventoryItemType> getAllActiveOrderedBySortOrder();

    /** Lookup by code, or {@code null} if none exists. */
    InventoryItemType getByCode(String code);

    /**
     * Create a new item type. If {@code code} is blank, one is generated from
     * {@code nameInLocale} (UPPER_SNAKE, collision-suffixed with _2, _3, ...).
     *
     * @param code         optional explicit code (uppercased, validated for
     *                     uniqueness if provided)
     * @param nameInLocale the display name, saved as the translation for
     *                     {@code locale}
     * @param locale       the locale the name is being entered in
     * @param sortOrder    display order
     * @param isActive     initial status; defaults to {@code true} when null
     * @param sysUserId    acting user, for audit
     */
    InventoryItemType create(String code, String nameInLocale, String locale, Integer sortOrder, Boolean isActive,
            String sysUserId);

    /**
     * Update the translation for {@code locale} and the sort order. The code is
     * immutable after creation.
     */
    InventoryItemType updateNameAndSortOrder(Long id, String locale, String nameInLocale, Integer sortOrder,
            String sysUserId);

    /** Deactivate a type — existing inventory items keep their assignment. */
    InventoryItemType deactivate(Long id, String sysUserId);

    /**
     * Create-or-update by code, setting every locale present in
     * {@code localizedNames} in one call. Used by the {@code inventory-item-types}
     * domain-config CSV loader ({@code InventoryItemTypeConfigurationHandler}) to
     * seed/refresh a starter set independently of the single-locale admin-page
     * {@link #create} flow. Rows created this way are marked seeded
     * ({@code isSeeded=true}).
     */
    InventoryItemType upsertSeeded(String code, Integer sortOrder, boolean isActive, Map<String, String> localizedNames,
            String sysUserId);
}
