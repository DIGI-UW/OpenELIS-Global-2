package org.openelisglobal.barcode.service;

import java.util.List;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface BarcodeWorkflowPrintService {

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    LabelsSectionForm buildLabelsSection(int orderQuantity, List<Integer> specimenQuantities);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    PostSavePrintDialogForm buildPostSavePrintDialog(String accessionNumber, LabelsSectionForm labelsSection);
}
