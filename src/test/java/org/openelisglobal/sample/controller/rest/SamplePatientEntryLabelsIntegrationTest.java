package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryLabelsIntegrationTest {

    @Mock
    private BarcodeWorkflowPrintService barcodeWorkflowPrintService;

    @Mock
    private SamplePatientEntryService samplePatientService;

    private SamplePatientEntryRestController controller;

    @Before
    public void setUp() {
        controller = new SamplePatientEntryRestController();
        ReflectionTestUtils.setField(controller, "barcodeWorkflowPrintService", barcodeWorkflowPrintService);
        ReflectionTestUtils.setField(controller, "samplePatientService", samplePatientService);
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

    // ── OGC-285 M5b SAFETY guard: label persistence fires ONLY when the save
    // body carried a labelPersistRequest, so existing/legacy saves are untouched.

    @Test
    public void maybePersistLabelRequests_firesPersistence_whenPayloadPresent() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        form.setLabelPersistRequest(payload);
        // mock (not new) — SamplePatientUpdateData's constructor reaches into
        // SpringContext, and the helper only passes the value through to the
        // (mocked) service, never dereferencing it.
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);

        controller.maybePersistLabelRequests(form, updateData, "1");

        verify(samplePatientService).persistLabelRequests(updateData, payload, "1");
    }

    @Test
    public void maybePersistLabelRequests_isNoOp_whenPayloadNull() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        // no labelPersistRequest set — the legacy/decoupled save path
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);

        controller.maybePersistLabelRequests(form, updateData, "1");

        verify(samplePatientService, never()).persistLabelRequests(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
