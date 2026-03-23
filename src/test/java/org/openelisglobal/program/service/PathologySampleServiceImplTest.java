package org.openelisglobal.program.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.form.LabelRowForm;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.barcode.service.BarcodeWorkflowPrintService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class PathologySampleServiceImplTest {

    @Mock
    private BarcodeInfoService barcodeInfoService;

    @Mock
    private BarcodeWorkflowPrintService barcodeWorkflowPrintService;

    private PathologySampleServiceImpl pathologySampleService;
    private PathologySample existingPathologySample;

    @Before
    public void setUp() {
        pathologySampleService = org.mockito.Mockito.spy(new PathologySampleServiceImpl());
        ReflectionTestUtils.setField(pathologySampleService, "barcodeInfoService", barcodeInfoService);
        ReflectionTestUtils.setField(pathologySampleService, "barcodeWorkflowPrintService",
                barcodeWorkflowPrintService);

        existingPathologySample = new PathologySample();
        existingPathologySample.setId(2);
        existingPathologySample.setBlocks(new ArrayList<>());
        existingPathologySample.setSlides(new ArrayList<>());
        existingPathologySample.setRequests(new ArrayList<>());
        existingPathologySample.setTechniques(new ArrayList<>());
        existingPathologySample.setConclusions(new ArrayList<>());
        existingPathologySample.setReports(new ArrayList<>());

        Sample sample = new Sample();
        sample.setId("1");
        existingPathologySample.setSample(sample);

        doReturn(existingPathologySample).when(pathologySampleService).get(eq(2));
        doReturn(existingPathologySample).when(pathologySampleService).update(any(PathologySample.class));
    }

    @Test
    public void updateWithFormValues_persistsPathologyBarcodeCountsWhenSupplied() {
        PathologySampleForm form = baseForm();
        form.setNumOrderLabels(2);
        form.setNumSpecimenLabels(3);
        form.setNumBlockLabels(4);
        form.setNumSlideLabels(5);
        form.setNumFreezerLabels(6);
        LabelsSectionForm labelsSection = createLabelsSectionForm();
        PostSavePrintDialogForm postSavePrintDialog = new PostSavePrintDialogForm();
        when(barcodeWorkflowPrintService.buildLabelsSection(2, java.util.List.of(3))).thenReturn(labelsSection);
        when(barcodeWorkflowPrintService.buildPostSavePrintDialog(any(), eq(labelsSection)))
                .thenReturn(postSavePrintDialog);

        pathologySampleService.updateWithFormValues(2, form);

        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItemsPathology(existingPathologySample.getSample(),
                2, 3, 4, 5, 6);
        verify(barcodeWorkflowPrintService).buildLabelsSection(2, java.util.List.of(3));
        verify(barcodeWorkflowPrintService).buildPostSavePrintDialog(any(), eq(labelsSection));
    }

    @Test
    public void updateWithFormValues_doesNotPersistPathologyBarcodeCountsWhenMissing() {
        PathologySampleForm form = baseForm();
        LabelsSectionForm labelsSection = createLabelsSectionForm();
        PostSavePrintDialogForm postSavePrintDialog = new PostSavePrintDialogForm();
        when(barcodeWorkflowPrintService.buildLabelsSection(1, java.util.List.of(1))).thenReturn(labelsSection);
        when(barcodeWorkflowPrintService.buildPostSavePrintDialog(any(), eq(labelsSection)))
                .thenReturn(postSavePrintDialog);

        pathologySampleService.updateWithFormValues(2, form);

        verify(barcodeInfoService, never()).saveBarcodeInfoForSampleAndSampleItemsPathology(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
        verify(barcodeWorkflowPrintService).buildLabelsSection(1, java.util.List.of(1));
        verify(barcodeWorkflowPrintService).buildPostSavePrintDialog(any(), eq(labelsSection));
    }

    @Test
    public void updateWithFormValues_setsWorkflowModelsOnFormResponse() {
        PathologySampleForm form = baseForm();
        form.setNumOrderLabels(2);
        form.setNumSpecimenLabels(2);
        form.setNumBlockLabels(1);
        form.setNumSlideLabels(1);
        form.setNumFreezerLabels(1);
        LabelsSectionForm labelsSection = createLabelsSectionForm();
        PostSavePrintDialogForm postSavePrintDialog = new PostSavePrintDialogForm();
        when(barcodeWorkflowPrintService.buildLabelsSection(2, java.util.List.of(2))).thenReturn(labelsSection);
        when(barcodeWorkflowPrintService.buildPostSavePrintDialog(any(), eq(labelsSection)))
                .thenReturn(postSavePrintDialog);

        pathologySampleService.updateWithFormValues(2, form);

        org.junit.Assert.assertSame(labelsSection, form.getLabelsSection());
        org.junit.Assert.assertSame(postSavePrintDialog, form.getPostSavePrintDialog());
        org.junit.Assert.assertEquals(Integer.valueOf(1), labelsSection.getOrderRow().getQuantities().get("block"));
        org.junit.Assert.assertEquals(Integer.valueOf(1), labelsSection.getOrderRow().getQuantities().get("slide"));
        org.junit.Assert.assertEquals(Integer.valueOf(1), labelsSection.getOrderRow().getQuantities().get("freezer"));
    }

    private PathologySampleForm baseForm() {
        PathologySampleForm form = new PathologySampleForm();
        form.setSystemUserId("2");
        form.setStatus(PathologySample.PathologyStatus.GROSSING);
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());
        return form;
    }

    private LabelsSectionForm createLabelsSectionForm() {
        LabelsSectionForm labelsSection = new LabelsSectionForm();
        LabelRowForm orderRow = new LabelRowForm();
        orderRow.setQuantities(new java.util.HashMap<>(java.util.Map.of("order", 1)));
        labelsSection.setOrderRow(orderRow);
        labelsSection.setSampleRows(new ArrayList<>());
        return labelsSection;
    }
}
