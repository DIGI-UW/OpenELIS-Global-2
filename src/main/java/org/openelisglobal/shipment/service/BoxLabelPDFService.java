package org.openelisglobal.shipment.service;

import java.io.ByteArrayOutputStream;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for generating PDF labels for shipping boxes
 */
public interface BoxLabelPDFService {

    /**
     * Generate a PDF label for a shipping box
     *
     * @param boxId The ID of the shipping box
     * @return ByteArrayOutputStream containing the PDF
     * @throws IllegalArgumentException if box not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    ByteArrayOutputStream generateBoxLabelPDF(String boxId);
}
