package org.openelisglobal.barcode.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.barcode.form.LabelRowForm;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.openelisglobal.barcode.form.PrintableLabelOptionForm;
import org.springframework.stereotype.Service;

@Service
public class BarcodeWorkflowPrintServiceImpl implements BarcodeWorkflowPrintService {

    @Override
    public LabelsSectionForm buildLabelsSection(int orderQuantity, List<Integer> specimenQuantities) {
        LabelsSectionForm section = new LabelsSectionForm();
        int runningTotal = 0;

        LabelRowForm orderRow = new LabelRowForm();
        orderRow.setRowType("order");
        orderRow.setRowId("order-row");
        orderRow.setApplicableLabelTypes(List.of("order"));
        int normalizedOrderQty = normalizeQuantity(orderQuantity);
        Map<String, Integer> orderQuantities = new HashMap<>();
        orderQuantities.put("order", normalizedOrderQty);
        orderRow.setQuantities(orderQuantities);
        orderRow.setRowTotal(normalizedOrderQty);
        section.setOrderRow(orderRow);
        runningTotal += normalizedOrderQty;

        List<LabelRowForm> sampleRows = new ArrayList<>();
        if (specimenQuantities != null) {
            for (int i = 0; i < specimenQuantities.size(); i++) {
                int normalizedSpecimenQty = normalizeQuantity(specimenQuantities.get(i));
                LabelRowForm sampleRow = new LabelRowForm();
                sampleRow.setRowType("sample");
                sampleRow.setRowId("sample-row-" + (i + 1));
                sampleRow.setSampleRef("sample-" + (i + 1));
                sampleRow.setApplicableLabelTypes(List.of("specimen"));
                Map<String, Integer> specimenQuantityMap = new HashMap<>();
                specimenQuantityMap.put("specimen", normalizedSpecimenQty);
                sampleRow.setQuantities(specimenQuantityMap);
                sampleRow.setRowTotal(normalizedSpecimenQty);
                sampleRows.add(sampleRow);
                runningTotal += normalizedSpecimenQty;
            }
        }
        section.setSampleRows(sampleRows);
        section.setRunningTotal(runningTotal);
        return section;
    }

    @Override
    public PostSavePrintDialogForm buildPostSavePrintDialog(String accessionNumber, LabelsSectionForm labelsSection) {
        PostSavePrintDialogForm dialog = new PostSavePrintDialogForm();
        dialog.setAccessionNumber(accessionNumber);
        dialog.setReprintContextToken(accessionNumber);
        dialog.setAllowSkipPrintLater(true);

        List<PrintableLabelOptionForm> printableOptions = new ArrayList<>();
        if (labelsSection != null) {
            // orderRow holds whole-accession entries (the order label and
            // pathology types — block/slide/freezer — that callers route here).
            addOrderOptions(printableOptions, accessionNumber, labelsSection.getOrderRow());
            // One specimen entry per sample row, tagged with the row's 1-based
            // position. Without that suffix, LabelMakerServlet treats
            // type=specimen as "every sample item" and multiplies the count.
            List<LabelRowForm> sampleRows = labelsSection.getSampleRows();
            if (sampleRows != null) {
                for (int i = 0; i < sampleRows.size(); i++) {
                    addSampleOptions(printableOptions, accessionNumber, i + 1, sampleRows.get(i));
                }
            }
        }
        dialog.setPrintableLabelTypes(printableOptions);
        return dialog;
    }

    private int normalizeQuantity(Integer quantity) {
        return quantity == null || quantity < 0 ? 0 : quantity;
    }

    private void addOrderOptions(List<PrintableLabelOptionForm> options, String accessionNumber, LabelRowForm row) {
        if (row == null || row.getQuantities() == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : row.getQuantities().entrySet()) {
            int qty = normalizeQuantity(entry.getValue());
            if (qty <= 0) {
                continue;
            }
            PrintableLabelOptionForm option = new PrintableLabelOptionForm();
            option.setLabelType(entry.getKey());
            option.setQuantity(qty);
            option.setDimensionsMm("");
            option.setPrintUrl(buildPrintUrl(accessionNumber, entry.getKey(), qty));
            options.add(option);
        }
    }

    private void addSampleOptions(List<PrintableLabelOptionForm> options, String accessionNumber, int sampleNumber,
            LabelRowForm row) {
        if (row == null || row.getQuantities() == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : row.getQuantities().entrySet()) {
            int qty = normalizeQuantity(entry.getValue());
            if (qty <= 0) {
                continue;
            }
            PrintableLabelOptionForm option = new PrintableLabelOptionForm();
            option.setLabelType(entry.getKey());
            option.setSampleNumber(sampleNumber);
            option.setQuantity(qty);
            option.setDimensionsMm("");
            option.setPrintUrl(buildSpecimenPrintUrl(accessionNumber, entry.getKey(), sampleNumber, qty));
            options.add(option);
        }
    }

    private String buildPrintUrl(String accessionNumber, String labelType, int quantity) {
        String typeForUrl = mapLabelTypeForUrl(labelType);
        String encodedAccession = encode(accessionNumber);
        String encodedType = encode(typeForUrl);
        return String.format("/LabelMakerServlet?labNo=%s&type=%s&quantity=%d", encodedAccession, encodedType,
                Math.max(quantity, 1));
    }

    private String buildSpecimenPrintUrl(String accessionNumber, String labelType, int sampleNumber, int quantity) {
        String typeForUrl = mapLabelTypeForUrl(labelType);
        String labNo = (accessionNumber == null ? "" : accessionNumber) + "." + sampleNumber;
        return String.format("/LabelMakerServlet?labNo=%s&type=%s&quantity=%d", encode(labNo), encode(typeForUrl),
                Math.max(quantity, 1));
    }

    private String mapLabelTypeForUrl(String labelType) {
        // BarcodeLabelMaker.generateLabels only branches on the *Order
        // variants — passing the bare type silently produces an empty PDF.
        if ("block".equals(labelType)) {
            return "blockOrder";
        }
        if ("slide".equals(labelType)) {
            return "slideOrder";
        }
        if ("freezer".equals(labelType)) {
            return "freezerOrder";
        }
        return labelType;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
