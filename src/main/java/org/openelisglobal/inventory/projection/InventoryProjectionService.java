package org.openelisglobal.inventory.projection;

import java.util.List;

public interface InventoryProjectionService {

    /**
     * Builds one row per active inventory item, sorted by urgency and then by how
     * soon the item runs out, so the items needing attention are already at the
     * top.
     */
    List<InventoryProjection> getBoard();
}
