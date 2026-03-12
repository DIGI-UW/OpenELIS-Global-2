package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.openelisglobal.barcode.service.BarcodeWorkflowPrintService;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryLabelsIntegrationTest {

    @Mock
    private BarcodeWorkflowPrintService barcodeWorkflowPrintService;

    private SamplePatientEntryRestController controller;

    @Before
    public void setUp() {
        controller = new SamplePatientEntryRestController();
        ReflectionTestUtils.setField(controller, "barcodeWorkflowPrintService", barcodeWorkflowPrintService);
    }

    @Test
    public void extractFirstSampleLabelQuantities_readsOrderAndSpecimenFromSampleXml() {
        String sampleXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><samples>"
                + "<sample sampleID='2' date='01/01/2026' time='00:00' collector='test' quantity='1' uom='1' "
                + "tests='1' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' rejectReasonId='' "
                + "initialConditionIds='' numOrderLabels='4' numSpecimenLabels='3'/>" + "</samples>";

        int[] quantities = controller.extractFirstSampleLabelQuantities(sampleXml);

        assertArrayEquals(new int[] { 4, 3 }, quantities);
    }

    @Test
    public void populateWorkflowPrintModels_setsResponseModelsForSubmittedLabels() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleXML("<?xml version=\"1.0\" encoding=\"utf-8\"?><samples>"
                + "<sample sampleID='2' tests='1' panels='' date='01/01/2026' time='00:00' collector='test' "
                + "quantity='1' uom='1' rejected='false' rejectReasonId='' initialConditionIds='' "
                + "numOrderLabels='5' numSpecimenLabels='2'/>" + "</samples>");

        LabelsSectionForm labelsSection = new LabelsSectionForm();
        PostSavePrintDialogForm postSavePrintDialog = new PostSavePrintDialogForm();
        when(barcodeWorkflowPrintService.buildLabelsSection(5, List.of(2))).thenReturn(labelsSection);
        when(barcodeWorkflowPrintService.buildPostSavePrintDialog("LAB-001", labelsSection))
                .thenReturn(postSavePrintDialog);

        controller.populateWorkflowPrintModels(form, "LAB-001");

        assertSame(labelsSection, form.getLabelsSection());
        assertSame(postSavePrintDialog, form.getPostSavePrintDialog());
        verify(barcodeWorkflowPrintService).buildLabelsSection(5, List.of(2));
        verify(barcodeWorkflowPrintService).buildPostSavePrintDialog("LAB-001", labelsSection);
    }
}
