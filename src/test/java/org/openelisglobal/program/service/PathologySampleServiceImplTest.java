package org.openelisglobal.program.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class PathologySampleServiceImplTest {

    @Mock
    private BarcodeInfoService barcodeInfoService;

    private PathologySampleServiceImpl pathologySampleService;
    private PathologySample existingPathologySample;

    @Before
    public void setUp() {
        pathologySampleService = org.mockito.Mockito.spy(new PathologySampleServiceImpl());
        ReflectionTestUtils.setField(pathologySampleService, "barcodeInfoService", barcodeInfoService);

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

        pathologySampleService.updateWithFormValues(2, form);

        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItemsPathology(existingPathologySample.getSample(),
                2, 3, 4, 5, 6);
    }

    @Test
    public void updateWithFormValues_doesNotPersistPathologyBarcodeCountsWhenMissing() {
        PathologySampleForm form = baseForm();

        pathologySampleService.updateWithFormValues(2, form);

        verify(barcodeInfoService, never()).saveBarcodeInfoForSampleAndSampleItemsPathology(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
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
}
