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
        // Sample row 1 has quantity 0 and is filtered out; sample row 2 (sortOrder 2)
        // is emitted as a per-sample specimen option with labNo=ACC-1.2.
        assertEquals(2, dialog.getPrintableLabelTypes().size());
        assertEquals("order", dialog.getPrintableLabelTypes().get(0).getLabelType());
        assertEquals(2, dialog.getPrintableLabelTypes().get(0).getQuantity());
        assertEquals("/LabelMakerServlet?labNo=ACC-1&type=order&quantity=2",
                dialog.getPrintableLabelTypes().get(0).getPrintUrl());
        assertEquals("specimen", dialog.getPrintableLabelTypes().get(1).getLabelType());
        assertEquals(Integer.valueOf(2), dialog.getPrintableLabelTypes().get(1).getSampleNumber());
        assertEquals(1, dialog.getPrintableLabelTypes().get(1).getQuantity());
        assertEquals("/LabelMakerServlet?labNo=ACC-1.2&type=specimen&quantity=1",
                dialog.getPrintableLabelTypes().get(1).getPrintUrl());
        assertTrue(dialog.isAllowSkipPrintLater());
        assertEquals("ACC-1", dialog.getReprintContextToken());
    }

    @Test
    public void buildPostSavePrintDialog_emitsOneSpecimenEntryPerSampleRow() {
        LabelsSectionForm section = service.buildLabelsSection(0, Arrays.asList(2, 3, 1));
        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-3", section);

        // Three sample rows, three independent specimen entries (no summing).
        assertEquals(3, dialog.getPrintableLabelTypes().size());

        assertEquals(Integer.valueOf(1), dialog.getPrintableLabelTypes().get(0).getSampleNumber());
        assertEquals(2, dialog.getPrintableLabelTypes().get(0).getQuantity());
        assertEquals("/LabelMakerServlet?labNo=ACC-3.1&type=specimen&quantity=2",
                dialog.getPrintableLabelTypes().get(0).getPrintUrl());

        assertEquals(Integer.valueOf(2), dialog.getPrintableLabelTypes().get(1).getSampleNumber());
        assertEquals(3, dialog.getPrintableLabelTypes().get(1).getQuantity());
        assertEquals("/LabelMakerServlet?labNo=ACC-3.2&type=specimen&quantity=3",
                dialog.getPrintableLabelTypes().get(1).getPrintUrl());

        assertEquals(Integer.valueOf(3), dialog.getPrintableLabelTypes().get(2).getSampleNumber());
        assertEquals(1, dialog.getPrintableLabelTypes().get(2).getQuantity());
        assertEquals("/LabelMakerServlet?labNo=ACC-3.3&type=specimen&quantity=1",
                dialog.getPrintableLabelTypes().get(2).getPrintUrl());
    }

    @Test
    public void buildPostSavePrintDialog_encodesAccessionNumberInPrintUrl() {
        LabelsSectionForm section = service.buildLabelsSection(1, Arrays.asList(1));

        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC 1/2", section);

        assertEquals("/LabelMakerServlet?labNo=ACC+1%2F2&type=order&quantity=1",
                dialog.getPrintableLabelTypes().get(0).getPrintUrl());
        assertEquals("/LabelMakerServlet?labNo=ACC+1%2F2.1&type=specimen&quantity=1",
                dialog.getPrintableLabelTypes().get(1).getPrintUrl());
    }

    @Test
    public void buildLabelsSection_handlesNullSampleInput() {
        LabelsSectionForm section = service.buildLabelsSection(1, null);

        assertEquals(1, section.getRunningTotal());
        assertEquals(Collections.emptyList(), section.getSampleRows());
    }

    @Test
    public void buildLabelsSection_clampsNullAndNegativeQuantitiesToZero() {
        LabelsSectionForm section = service.buildLabelsSection(-5, Arrays.asList(null, -2, 4));

        assertEquals(0, section.getOrderRow().getRowTotal());
        assertEquals(0, section.getSampleRows().get(0).getRowTotal());
        assertEquals(0, section.getSampleRows().get(1).getRowTotal());
        assertEquals(4, section.getSampleRows().get(2).getRowTotal());
        assertEquals(4, section.getRunningTotal());
    }

    @Test
    public void buildPostSavePrintDialog_emptyDialogWhenAllQuantitiesAreZero() {
        LabelsSectionForm section = service.buildLabelsSection(0, Arrays.asList(0, 0, 0));
        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-Z", section);

        // Caller must distinguish "no printable labels" from "missing dialog".
        assertEquals(0, dialog.getPrintableLabelTypes().size());
        assertEquals("ACC-Z", dialog.getAccessionNumber());
    }

    @Test
    public void buildPostSavePrintDialog_combinesOrderAndPerSampleEntries() {
        LabelsSectionForm section = service.buildLabelsSection(7, Arrays.asList(4, 4, 4));
        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-9", section);

        // 1 order entry + 3 specimen entries; specimen quantities don't sum.
        assertEquals(4, dialog.getPrintableLabelTypes().size());
        assertEquals("order", dialog.getPrintableLabelTypes().get(0).getLabelType());
        assertEquals(7, dialog.getPrintableLabelTypes().get(0).getQuantity());
        for (int i = 1; i <= 3; i++) {
            assertEquals("specimen", dialog.getPrintableLabelTypes().get(i).getLabelType());
            assertEquals(Integer.valueOf(i), dialog.getPrintableLabelTypes().get(i).getSampleNumber());
            assertEquals(4, dialog.getPrintableLabelTypes().get(i).getQuantity());
        }
    }

    @Test
    public void buildPostSavePrintDialog_passesThroughUnknownLabelType() {
        LabelsSectionForm section = new LabelsSectionForm();
        LabelRowForm orderRow = new LabelRowForm();
        orderRow.setQuantities(Map.of("custom", 2));
        section.setOrderRow(orderRow);
        section.setSampleRows(Collections.emptyList());

        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-U", section);

        assertEquals(1, dialog.getPrintableLabelTypes().size());
        assertEquals("custom", dialog.getPrintableLabelTypes().get(0).getLabelType());
        // Unknown types pass through untouched; servlet validation is the
        // gatekeeper for whether the type is actually printable.
        assertEquals("/LabelMakerServlet?labNo=ACC-U&type=custom&quantity=2",
                dialog.getPrintableLabelTypes().get(0).getPrintUrl());
    }

    @Test
    public void buildPostSavePrintDialog_mapsPathologyTypesToServletDispatchParams() {
        LabelsSectionForm section = new LabelsSectionForm();
        LabelRowForm orderRow = new LabelRowForm();
        orderRow.setQuantities(java.util.Map.of("block", 1, "slide", 2, "freezer", 3));
        section.setOrderRow(orderRow);
        section.setSampleRows(Collections.emptyList());

        PostSavePrintDialogForm dialog = service.buildPostSavePrintDialog("ACC-2", section);

        Map<String, String> printUrlsByType = dialog.getPrintableLabelTypes().stream()
                .collect(Collectors.toMap(option -> option.getLabelType(), option -> option.getPrintUrl()));

        // BarcodeLabelMaker only handles the *Order suffix variants — including
        // freezerOrder. The bare "freezer" type passes servlet validation but
        // produces an empty PDF, so the URL builder normalizes to freezerOrder.
        assertEquals("/LabelMakerServlet?labNo=ACC-2&type=blockOrder&quantity=1", printUrlsByType.get("block"));
        assertEquals("/LabelMakerServlet?labNo=ACC-2&type=slideOrder&quantity=2", printUrlsByType.get("slide"));
        assertEquals("/LabelMakerServlet?labNo=ACC-2&type=freezerOrder&quantity=3", printUrlsByType.get("freezer"));
    }
}
