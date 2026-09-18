package org.openelisglobal.inventory.projection;

import java.util.List;

public interface InventoryProjectionService {

    /** One row per active item, most urgent first, then soonest run-out. */
    List<InventoryProjection> getBoard();

    List<InventoryProjection> getBoard(boolean includeInactive);
}
