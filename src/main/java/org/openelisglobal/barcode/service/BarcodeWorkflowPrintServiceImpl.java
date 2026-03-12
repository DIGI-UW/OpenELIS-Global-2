package org.openelisglobal.barcode.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.barcode.form.LabelRowForm;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
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

        Set<String> printableTypes = new LinkedHashSet<>();
        if (labelsSection != null) {
            addPositiveTypes(printableTypes, labelsSection.getOrderRow());
            if (labelsSection.getSampleRows() != null) {
                for (LabelRowForm row : labelsSection.getSampleRows()) {
                    addPositiveTypes(printableTypes, row);
                }
            }
        }
        dialog.setPrintableLabelTypes(new ArrayList<>(printableTypes));
        return dialog;
    }

    private int normalizeQuantity(Integer quantity) {
        return quantity == null || quantity < 0 ? 0 : quantity;
    }

    private void addPositiveTypes(Set<String> printableTypes, LabelRowForm row) {
        if (row == null || row.getQuantities() == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : row.getQuantities().entrySet()) {
            if (normalizeQuantity(entry.getValue()) > 0) {
                printableTypes.add(entry.getKey());
            }
        }
    }
}
