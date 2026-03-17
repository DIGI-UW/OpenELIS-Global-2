package org.openelisglobal.barcode.service;

import java.util.List;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;

public interface BarcodeWorkflowPrintService {

    LabelsSectionForm buildLabelsSection(int orderQuantity, List<Integer> specimenQuantities);

    PostSavePrintDialogForm buildPostSavePrintDialog(String accessionNumber, LabelsSectionForm labelsSection);
}
