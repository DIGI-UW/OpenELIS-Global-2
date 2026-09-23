package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryTag;

public interface InventoryTagDAO extends BaseDAO<InventoryTag, Long> {

    /**
     * Every directory entry. Most tags have none — a row exists only once a tag has
     * been deactivated or created ahead of use.
     */
    List<InventoryTag> getAllTags() throws LIMSRuntimeException;

    /**
     * The entry for one tag, matched the way the write path folds spellings: on
     * case and inner spacing, so {@code Glove} and {@code glove} are one tag.
     */
    InventoryTag getByName(String name) throws LIMSRuntimeException;

    /** How many items carry each tag, keyed by the tag as it is stored. */
    java.util.Map<String, Long> countItemsPerTag() throws LIMSRuntimeException;
}
