package org.openelisglobal.barcode.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
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
    public void buildLabelsSection_returnsMutableQuantityMaps() {
        LabelsSectionForm section = service.buildLabelsSection(1, Arrays.asList(1));

        section.getOrderRow().getQuantities().put("block", 2);
        section.getSampleRows().get(0).getQuantities().put("freezer", 3);

        assertEquals(Integer.valueOf(2), section.getOrderRow().getQuantities().get("block"));
        assertEquals(Integer.valueOf(3), section.getSampleRows().get(0).getQuantities().get("freezer"));
    }

    @Test
    public void buildPostSavePrintDialog_includesOnlyPositiveApplicableTypes() {
        LabelsSectionForm section = service.buildLabelsSection(2, Arrays.asList(0, 1));
        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-1", section);

        assertEquals("ACC-1", dialog.getAccessionNumber());
        assertEquals("order", dialog.getPrintableLabelTypes().get(0).getLabelType());
        assertEquals(2, dialog.getPrintableLabelTypes().get(0).getQuantity());
        assertEquals("/LabelMakerServlet?labNo=ACC-1&type=order", dialog.getPrintableLabelTypes().get(0).getPrintUrl());
        assertEquals("specimen", dialog.getPrintableLabelTypes().get(1).getLabelType());
        assertEquals(1, dialog.getPrintableLabelTypes().get(1).getQuantity());
        assertEquals("/LabelMakerServlet?labNo=ACC-1&type=specimen",
                dialog.getPrintableLabelTypes().get(1).getPrintUrl());
        assertEquals(2, dialog.getPrintableLabelTypes().size());
        assertTrue(dialog.isAllowSkipPrintLater());
        assertEquals("ACC-1", dialog.getReprintContextToken());
    }

    @Test
    public void buildPostSavePrintDialog_encodesAccessionNumberInPrintUrl() {
        LabelsSectionForm section = service.buildLabelsSection(1, Arrays.asList(1));

        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC 1/2", section);

        assertEquals("/LabelMakerServlet?labNo=ACC+1%2F2&type=order",
                dialog.getPrintableLabelTypes().get(0).getPrintUrl());
    }

    @Test
    public void buildLabelsSection_handlesNullSampleInput() {
        LabelsSectionForm section = service.buildLabelsSection(1, null);

        assertEquals(1, section.getRunningTotal());
        assertEquals(Collections.emptyList(), section.getSampleRows());
    }

    @Test
    public void buildPostSavePrintDialog_mapsPathologyTypesToServletDispatchParams() {
        LabelsSectionForm section = new LabelsSectionForm();
        LabelRowForm orderRow = new LabelRowForm();
        orderRow.setQuantities(java.util.Map.of("block", 1, "slide", 2, "freezer", 1));
        section.setOrderRow(orderRow);
        section.setSampleRows(Collections.emptyList());

        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-2", section);

        Map<String, String> printUrlsByType = dialog.getPrintableLabelTypes().stream()
                .collect(Collectors.toMap(option -> option.getLabelType(), option -> option.getPrintUrl()));

        assertEquals("/LabelMakerServlet?labNo=ACC-2&type=blockOrder", printUrlsByType.get("block"));
        assertEquals("/LabelMakerServlet?labNo=ACC-2&type=slideOrder", printUrlsByType.get("slide"));
        assertEquals("/LabelMakerServlet?labNo=ACC-2&type=freezer", printUrlsByType.get("freezer"));
    }
}
