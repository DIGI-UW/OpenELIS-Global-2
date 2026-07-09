package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.CodeGenerator;
import org.openelisglobal.inventory.dao.InventoryItemTypeDAO;
import org.openelisglobal.inventory.valueholder.InventoryItemType;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryItemTypeServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItemType, Long>
        implements InventoryItemTypeService {

    // inventory_item_type.code is VARCHAR(50) — see 070-inventory-item-type.xml
    private static final int CODE_MAX_LENGTH = 50;

    @Autowired
    private InventoryItemTypeDAO inventoryItemTypeDAO;

    @Autowired
    private LocalizationService localizationService;

    public InventoryItemTypeServiceImpl() {
        super(InventoryItemType.class);
    }

    @Override
    protected InventoryItemTypeDAO getBaseObjectDAO() {
        return inventoryItemTypeDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemType> getAllOrderedBySortOrder() {
        return inventoryItemTypeDAO.getAllOrderedBySortOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemType> getAllActiveOrderedBySortOrder() {
        return inventoryItemTypeDAO.getAllActiveOrderedBySortOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemType getByCode(String code) {
        return inventoryItemTypeDAO.getByCode(code);
    }

    @Override
    @Transactional
    public InventoryItemType create(String code, String nameInLocale, String locale, Integer sortOrder,
            Boolean isActive, String sysUserId) {
        if (nameInLocale == null || nameInLocale.trim().isEmpty()) {
            throw new LIMSRuntimeException("Name is required");
        }
        String finalCode = (code == null || code.trim().isEmpty()) ? CodeGenerator.generateFromName(nameInLocale,
                CODE_MAX_LENGTH, "TYPE", inventoryItemTypeDAO::existsByCode)
                : CodeGenerator.normalize(code, CODE_MAX_LENGTH);
        if (inventoryItemTypeDAO.existsByCode(finalCode)) {
            throw new LIMSRuntimeException("Inventory item type code already exists: " + finalCode);
        }

        Localization localization = new Localization();
        localization.setDescription("inventory item type: " + finalCode);
        localization.setLocalizedValue(locale, nameInLocale.trim());
        localizationService.insert(localization);

        InventoryItemType type = new InventoryItemType();
        type.setCode(finalCode);
        type.setNameLocalization(localization);
        type.setSortOrder(sortOrder != null ? sortOrder : 0);
        type.setIsActive(isActive != null ? isActive : true);
        type.setIsSeeded(false);
        type.setSysUserId(sysUserId);
        type.setLastupdated(new Timestamp(System.currentTimeMillis()));
        insert(type);
        return type;
    }

    @Override
    @Transactional
    public InventoryItemType updateNameAndSortOrder(Long id, String locale, String nameInLocale, Integer sortOrder,
            String sysUserId) {
        InventoryItemType type = getOrThrow(id);
        if (nameInLocale != null && !nameInLocale.trim().isEmpty()) {
            Localization localization = type.getNameLocalization();
            localization.setLocalizedValue(locale, nameInLocale.trim());
            localizationService.update(localization);
        }
        if (sortOrder != null) {
            type.setSortOrder(sortOrder);
        }
        type.setSysUserId(sysUserId);
        type.setLastupdated(new Timestamp(System.currentTimeMillis()));
        return update(type);
    }

    @Override
    @Transactional
    public InventoryItemType deactivate(Long id, String sysUserId) {
        InventoryItemType type = getOrThrow(id);
        type.setIsActive(false);
        type.setSysUserId(sysUserId);
        type.setLastupdated(new Timestamp(System.currentTimeMillis()));
        return update(type);
    }

    @Override
    @Transactional
    public InventoryItemType upsertSeeded(String code, Integer sortOrder, boolean isActive,
            Map<String, String> localizedNames, String sysUserId) {
        String normalizedCode = CodeGenerator.normalize(code, CODE_MAX_LENGTH);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        InventoryItemType existing = inventoryItemTypeDAO.getByCode(normalizedCode);

        if (existing != null) {
            Localization localization = existing.getNameLocalization();
            localizedNames.forEach(localization::setLocalizedValue);
            localizationService.update(localization);
            existing.setSortOrder(sortOrder != null ? sortOrder : existing.getSortOrder());
            existing.setIsActive(isActive);
            existing.setSysUserId(sysUserId);
            existing.setLastupdated(now);
            return update(existing);
        }

        Localization localization = new Localization();
        localization.setDescription("inventory item type: " + normalizedCode);
        localizedNames.forEach(localization::setLocalizedValue);
        localizationService.insert(localization);

        InventoryItemType type = new InventoryItemType();
        type.setCode(normalizedCode);
        type.setNameLocalization(localization);
        type.setSortOrder(sortOrder != null ? sortOrder : 0);
        type.setIsActive(isActive);
        type.setIsSeeded(true);
        type.setSysUserId(sysUserId);
        type.setLastupdated(now);
        insert(type);
        return type;
    }

    private InventoryItemType getOrThrow(Long id) {
        try {
            return get(id);
        } catch (ObjectNotFoundException e) {
            throw new LIMSRuntimeException("Inventory item type not found: " + id);
        }
    }
}
