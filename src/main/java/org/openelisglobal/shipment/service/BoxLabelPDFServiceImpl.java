package org.openelisglobal.shipment.service;

import java.io.ByteArrayOutputStream;
import org.openelisglobal.barcode.BarcodeLabelMaker;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.shipment.barcode.ShippingBoxLabel;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of BoxLabelPDFService for generating shipping box label PDFs
 */
@Service
public class BoxLabelPDFServiceImpl implements BoxLabelPDFService {

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateBoxLabelPDF(String boxId) {
        // Parse boxId as Integer (from the REST endpoint it will be a String)
        Integer id;
        try {
            id = Integer.parseInt(boxId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid box ID format: " + boxId);
        }

        ShippingBox box = shippingBoxService.getBoxById(id);

        if (box == null) {
            throw new IllegalArgumentException("Shipping box not found: " + boxId);
        }

        // Create label with box information
        ShippingBoxLabel label = new ShippingBoxLabel(box.getBoxId(),
                box.getDestinationFacility() != null ? box.getDestinationFacility().getOrganizationName() : "",
                box.getTemperatureRequirement() != null ? box.getTemperatureRequirement().toString() : "AMBIENT",
                box.getSampleCount(), box.getCreatedDate());

        return generatePDF(label);
    }

    /**
     * Generate PDF from shipping box label using BarcodeLabelMaker
     */
    private ByteArrayOutputStream generatePDF(ShippingBoxLabel label) {
        try {
            // Link barcode label info (for print tracking)
            label.linkBarcodeLabelInfo();

            // Create BarcodeLabelMaker - use single-label constructor
            BarcodeLabelMaker labelMaker = new BarcodeLabelMaker(label);

            // Set number of labels to print (default 1)
            label.setNumLabels(1);

            // Generate PDF stream
            ByteArrayOutputStream stream = labelMaker.createLabelsAsStream();

            if (stream == null || stream.size() == 0) {
                LogEvent.logError("BoxLabelPDFServiceImpl", "generatePDF", "PDF stream is null or empty!");
            }

            return stream;
        } catch (Exception e) {
            LogEvent.logError("BoxLabelPDFServiceImpl", "generatePDF",
                    "Exception during PDF generation: " + e.getClass().getName() + " - " + e.getMessage());
            LogEvent.logError(e);
            if (e.getCause() != null) {
                LogEvent.logError("BoxLabelPDFServiceImpl", "generatePDF",
                        "Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to generate box label PDF", e);
        }
    }
}
