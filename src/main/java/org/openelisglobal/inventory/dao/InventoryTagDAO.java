package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryTag;

public interface InventoryTagDAO extends BaseDAO<InventoryTag, Long> {

    List<InventoryTag> getAllTags() throws LIMSRuntimeException;

    /** Ignores case and inner spacing, as the item write path does. */
    InventoryTag getByName(String name) throws LIMSRuntimeException;

    /** How many items carry each tag, keyed by the tag as it is stored. */
    java.util.Map<String, Long> countItemsPerTag() throws LIMSRuntimeException;
}
