package org.openelisglobal.barcode.service;

import java.util.List;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;

/**
 * OGC-285 M5 (T148) — deletion DEFERRED (audit verdict: DEFER_DELETION).
 *
 * <p>
 * tasks.md T148 calls for deleting this service once
 * {@link org.openelisglobal.labelpreset.service.OrderEntryLabelRequestService}
 * is the authoritative aggregator. It is NOT deleted in M5a backend because:
 * <ul>
 * <li>The new aggregator returns the Order Entry column/cell model, NOT the
 * legacy {@link LabelsSectionForm} / {@link PostSavePrintDialogForm} shape this
 * service produces — there is no drop-in replacement yet.</li>
 * <li>Three live callers still depend on it:
 * {@code GenericSampleOrderServiceImpl},
 * {@code SamplePatientEntryRestController}, and
 * {@code PathologySampleServiceImpl} (the pathology path also relies on its
 * {@code mapLabelTypeForUrl} block/slide/freezer URL types). A fourth coupling
 * is the {@code LabelMakerServlet} URL-type contract.</li>
 * <li>Deleting it now would break compilation and the post-save print dialog
 * for all three workflows.</li>
 * </ul>
 * The migration of these callers to the new model lands with the M5b Order
 * Entry frontend rewrite (tasks.md T143–T145). Follow-up ticket: M5b — migrate
 * BarcodeWorkflowPrint callers to OrderEntryLabelRequest model, then delete
 * this service (and satisfy grep gates T148a / T148b).
 */
public interface BarcodeWorkflowPrintService {

    LabelsSectionForm buildLabelsSection(int orderQuantity, List<Integer> specimenQuantities);

    PostSavePrintDialogForm buildPostSavePrintDialog(String accessionNumber, LabelsSectionForm labelsSection);
}
