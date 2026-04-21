package org.openelisglobal.shipment.service;

import java.io.ByteArrayOutputStream;

/**
 * Service for generating PDF manifests for shipping boxes Contains detailed
 * list of all samples in a box
 */
public interface ManifestPDFService {

    /**
     * Generate a PDF manifest for a shipping box
     *
     * @param boxId The ID of the shipping box
     * @return ByteArrayOutputStream containing the PDF
     * @throws IllegalArgumentException if box not found
     */
    ByteArrayOutputStream generateManifestPDF(String boxId);
}
