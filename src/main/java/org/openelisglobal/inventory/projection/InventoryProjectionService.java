package org.openelisglobal.inventory.projection;

import java.util.List;

public interface InventoryProjectionService {

    /**
     * Builds one row per active inventory item, sorted by urgency and then by how
     * soon the item runs out, so the items needing attention are already at the
     * top.
     */
    List<InventoryProjection> getBoard();

    /**
     * The board, optionally including items that have been deactivated. A
     * deactivated item is hidden rather than deleted, so there has to be somewhere
     * to see one — otherwise deactivating is indistinguishable from losing it.
     */
    List<InventoryProjection> getBoard(boolean includeInactive);
}
