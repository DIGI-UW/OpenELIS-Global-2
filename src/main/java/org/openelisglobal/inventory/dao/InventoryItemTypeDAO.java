package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryItemType;

public interface InventoryItemTypeDAO extends BaseDAO<InventoryItemType, Long> {

    /** All types (any status), sorted by sort_order — for the admin list. */
    List<InventoryItemType> getAllOrderedBySortOrder() throws LIMSRuntimeException;

    /** Active-only types, sorted by sort_order — for the Item Add dropdown. */
    List<InventoryItemType> getAllActiveOrderedBySortOrder() throws LIMSRuntimeException;

    InventoryItemType getByCode(String code) throws LIMSRuntimeException;

    boolean existsByCode(String code) throws LIMSRuntimeException;
}
