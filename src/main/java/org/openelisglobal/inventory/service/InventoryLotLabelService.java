package org.openelisglobal.inventory.service;

import java.io.ByteArrayOutputStream;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.security.access.prepost.PreAuthorize;

/** Generates printable barcode labels for inventory lots. */
public interface InventoryLotLabelService {

    /**
     * Render a PDF label carrying the lot's barcode, item name, lot number and
     * expiry.
     *
     * @param lot The lot to label; must already have a barcode
     * @return PDF as ByteArrayOutputStream
     * @throws IllegalArgumentException if the lot is null or has no barcode
     */
    @PreAuthorize("hasAuthority('PRIV_BARCODE_VIEW')")
    ByteArrayOutputStream generateLabel(InventoryLot lot);
}
