package org.openelisglobal.inventory.service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import org.openelisglobal.barcode.BarcodeLabelMaker;
import org.openelisglobal.barcode.labeltype.InventoryLotLabel;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryLotLabelServiceImpl implements InventoryLotLabelService {

    private static final String EXPIRY_FORMAT = "yyyy-MM-dd";

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(InventoryLot lot) {
        if (lot == null) {
            throw new IllegalArgumentException("Lot cannot be null");
        }
        String barcode = lot.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Lot barcode is required for label printing.");
        }

        String itemName = lot.getInventoryItem() != null ? lot.getInventoryItem().getName() : null;
        String expiry = lot.getExpirationDate() != null
                ? new SimpleDateFormat(EXPIRY_FORMAT).format(lot.getExpirationDate())
                : "";

        InventoryLotLabel label = new InventoryLotLabel(itemName, lot.getLotNumber(), expiry, barcode);
        return generatePDF(label);
    }

    private ByteArrayOutputStream generatePDF(InventoryLotLabel label) {
        try {
            label.linkBarcodeLabelInfo();
            BarcodeLabelMaker labelMaker = new BarcodeLabelMaker(label);
            label.setNumLabels(1);
            ByteArrayOutputStream stream = labelMaker.createLabelsAsStream();
            if (stream == null || stream.size() == 0) {
                LogEvent.logError("InventoryLotLabelServiceImpl", "generatePDF", "PDF stream is null or empty");
            }
            return stream;
        } catch (Exception e) {
            LogEvent.logError("InventoryLotLabelServiceImpl", "generatePDF",
                    "Exception during PDF generation: " + e.getClass().getName() + " - " + e.getMessage());
            LogEvent.logError(e);
            throw new IllegalStateException("Failed to generate lot label PDF", e);
        }
    }
}
