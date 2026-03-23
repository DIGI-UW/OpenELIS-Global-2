package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
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
    public void extractLabelQuantities_readsOrderAndSpecimenFromAllSamples() {
        String sampleXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><samples>"
                + "<sample sampleID='2' date='01/01/2026' time='00:00' collector='test' quantity='1' uom='1' "
                + "tests='1' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' rejectReasonId='' "
                + "initialConditionIds='' numOrderLabels='4' numSpecimenLabels='3'/>"
                + "<sample sampleID='3' date='01/01/2026' time='00:00' collector='test' quantity='1' uom='1' "
                + "tests='2' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' rejectReasonId='' "
                + "initialConditionIds='' numOrderLabels='4' numSpecimenLabels='5'/>" + "</samples>";

        SamplePatientEntryRestController.ParsedLabelQuantities quantities = controller
                .extractLabelQuantities(sampleXml);

        assertEquals(4, quantities.orderQuantity);
        assertEquals(List.of(3, 5), quantities.specimenQuantities);
    }

    @Test
    public void populateWorkflowPrintModels_setsResponseModelsForSubmittedLabels() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleXML("<?xml version=\"1.0\" encoding=\"utf-8\"?><samples>"
                + "<sample sampleID='2' tests='1' panels='' date='01/01/2026' time='00:00' collector='test' "
                + "quantity='1' uom='1' rejected='false' rejectReasonId='' initialConditionIds='' "
                + "numOrderLabels='5' numSpecimenLabels='2'/>"
                + "<sample sampleID='3' tests='2' panels='' date='01/01/2026' time='00:00' collector='test' "
                + "quantity='1' uom='1' rejected='false' rejectReasonId='' initialConditionIds='' "
                + "numOrderLabels='5' numSpecimenLabels='3'/>" + "</samples>");

        LabelsSectionForm labelsSection = new LabelsSectionForm();
        PostSavePrintDialogForm postSavePrintDialog = new PostSavePrintDialogForm();
        when(barcodeWorkflowPrintService.buildLabelsSection(5, List.of(2, 3))).thenReturn(labelsSection);
        when(barcodeWorkflowPrintService.buildPostSavePrintDialog("LAB-001", labelsSection))
                .thenReturn(postSavePrintDialog);

        controller.populateWorkflowPrintModels(form, "LAB-001");

        assertSame(labelsSection, form.getLabelsSection());
        assertSame(postSavePrintDialog, form.getPostSavePrintDialog());
        verify(barcodeWorkflowPrintService).buildLabelsSection(5, List.of(2, 3));
        verify(barcodeWorkflowPrintService).buildPostSavePrintDialog("LAB-001", labelsSection);
    }
}
