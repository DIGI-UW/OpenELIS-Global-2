package org.openelisglobal.inventory.dao;

import org.openelisglobal.common.exception.LIMSRuntimeException;

public interface InventoryItemCodeSequenceDAO {

    /**
     * Hands out the next counter value for {@code prefix}, starting at 1, holding
     * the row lock until the caller's transaction ends.
     */
    long nextValue(String prefix) throws LIMSRuntimeException;
}
