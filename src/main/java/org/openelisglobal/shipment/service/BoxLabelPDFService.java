package org.openelisglobal.shipment.service;

import java.io.ByteArrayOutputStream;

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
    ByteArrayOutputStream generateBoxLabelPDF(String boxId);
}
