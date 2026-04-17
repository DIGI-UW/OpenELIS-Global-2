package org.openelisglobal.storage.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for parsing storage location barcodes Supports 2-5 level hierarchical
 * barcodes per FR-023
 *
 * Barcode format: ROOM-DEVICE[-SHELF[-RACK[-POSITION]]] - Minimum 2 levels
 * (Room + Device) - Maximum 5 levels (Room + Device + Shelf + Rack + Position)
 * - Delimiter: hyphen (-) only
 */
public interface BarcodeParsingService {

    /**
     * Parse a barcode string into its hierarchical components
     *
     * @param barcode The barcode string to parse
     * @return ParsedBarcode object with extracted components
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    ParsedBarcode parseBarcode(String barcode);

    /**
     * Validate barcode format without parsing
     *
     * @param barcode The barcode string to validate
     * @return true if format is valid (2-5 levels, hyphen-delimited)
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean validateFormat(String barcode);

    /**
     * Extract components from a barcode string
     *
     * @param barcode The barcode string
     * @return List of string components (empty if invalid)
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    List<String> extractComponents(String barcode);
}
