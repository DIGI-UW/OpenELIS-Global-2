package org.openelisglobal.barcode.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        orderRow.setQuantities(Map.of("order", normalizedOrderQty));
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
                sampleRow.setQuantities(Map.of("specimen", normalizedSpecimenQty));
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

        Map<String, Integer> printableTypes = new LinkedHashMap<>();
        if (labelsSection != null) {
            addPositiveTypes(printableTypes, labelsSection.getOrderRow());
            if (labelsSection.getSampleRows() != null) {
                for (LabelRowForm row : labelsSection.getSampleRows()) {
                    addPositiveTypes(printableTypes, row);
                }
            }
        }
        List<PrintableLabelOptionForm> printableOptions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : printableTypes.entrySet()) {
            PrintableLabelOptionForm option = new PrintableLabelOptionForm();
            option.setLabelType(entry.getKey());
            option.setQuantity(entry.getValue());
            option.setDimensionsMm("");
            option.setPrintUrl(buildPrintUrl(accessionNumber, entry.getKey()));
            printableOptions.add(option);
        }
        dialog.setPrintableLabelTypes(printableOptions);
        return dialog;
    }

    private int normalizeQuantity(Integer quantity) {
        return quantity == null || quantity < 0 ? 0 : quantity;
    }

    private void addPositiveTypes(Map<String, Integer> printableTypes, LabelRowForm row) {
        if (row == null || row.getQuantities() == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : row.getQuantities().entrySet()) {
            int normalizedQuantity = normalizeQuantity(entry.getValue());
            if (normalizedQuantity > 0) {
                printableTypes.put(entry.getKey(), printableTypes.getOrDefault(entry.getKey(), 0) + normalizedQuantity);
            }
        }
    }

    private String buildPrintUrl(String accessionNumber, String labelType) {
        String typeForUrl = labelType;
        if ("block".equals(labelType)) {
            typeForUrl = "blockOrder";
        } else if ("slide".equals(labelType)) {
            typeForUrl = "slideOrder";
        }
        return String.format("/LabelMakerServlet?labNo=%s&type=%s", accessionNumber, typeForUrl);
    }
}
