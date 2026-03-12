package org.openelisglobal.barcode.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.barcode.form.LabelRowForm;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;

public class BarcodeWorkflowPrintServiceTest {

    private final BarcodeWorkflowPrintService service = new BarcodeWorkflowPrintServiceImpl();

    @Test
    public void buildLabelsSection_createsOrderAndSampleRows_andRunningTotal() {
        LabelsSectionForm section = service.buildLabelsSection(2, Arrays.asList(1, 3));

        assertNotNull(section);
        assertNotNull(section.getOrderRow());
        assertEquals("order", section.getOrderRow().getRowType());
        assertEquals(2, section.getOrderRow().getRowTotal());
        assertEquals(2, section.getSampleRows().size());
        assertEquals(6, section.getRunningTotal());
    }

    @Test
    public void buildPostSavePrintDialog_includesOnlyPositiveApplicableTypes() {
        LabelsSectionForm section = service.buildLabelsSection(2, Arrays.asList(0, 1));
        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-1", section);

        assertEquals("ACC-1", dialog.getAccessionNumber());
        assertTrue(dialog.getPrintableLabelTypes().contains("order"));
        assertTrue(dialog.getPrintableLabelTypes().contains("specimen"));
        assertEquals(2, dialog.getPrintableLabelTypes().size());
        assertTrue(dialog.isAllowSkipPrintLater());
        assertEquals("ACC-1", dialog.getReprintContextToken());
    }

    @Test
    public void buildLabelsSection_handlesNullSampleInput() {
        LabelsSectionForm section = service.buildLabelsSection(1, null);

        assertEquals(1, section.getRunningTotal());
        assertEquals(Collections.emptyList(), section.getSampleRows());
    }
}
