package org.openelisglobal.sample.service;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryServiceImplTest {

    @Mock
    private BarcodeInfoService barcodeInfoService;

    private SamplePatientEntryServiceImpl service;

    @Before
    public void setUp() {
        service = new SamplePatientEntryServiceImpl();
        ReflectionTestUtils.setField(service, "barcodeInfoService", barcodeInfoService);
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_persistsProvidedValues() {
        Sample sample = new Sample();
        sample.setId("sample-1");
        SampleItem firstItem = new SampleItem();
        firstItem.setId("item-1");
        SampleItem secondItem = new SampleItem();
        secondItem.setId("item-2");
        Map<SampleItem, Integer> specimenLabelQuantities = new LinkedHashMap<>();
        specimenLabelQuantities.put(firstItem, 4);
        specimenLabelQuantities.put(secondItem, 3);

        service.persistOrderSpecimenBarcodeCounts(sample, 4, specimenLabelQuantities);

        ArgumentCaptor<Map<SampleItem, Integer>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(eq(sample), eq(4), mapCaptor.capture());
        org.junit.Assert.assertEquals(Integer.valueOf(4), mapCaptor.getValue().get(firstItem));
        org.junit.Assert.assertEquals(Integer.valueOf(3), mapCaptor.getValue().get(secondItem));
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_defaultsInvalidValuesToOne() {
        Sample sample = new Sample();
        sample.setId("sample-1");
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("item-1");
        Map<SampleItem, Integer> specimenLabelQuantities = new LinkedHashMap<>();
        specimenLabelQuantities.put(sampleItem, -2);

        service.persistOrderSpecimenBarcodeCounts(sample, 0, specimenLabelQuantities);

        ArgumentCaptor<Map<SampleItem, Integer>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(eq(sample), eq(1), mapCaptor.capture());
        org.junit.Assert.assertEquals(Integer.valueOf(1), mapCaptor.getValue().get(sampleItem));
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_skipsNullSample() {
        service.persistOrderSpecimenBarcodeCounts(null, 2, new LinkedHashMap<>());

        verify(barcodeInfoService, never()).saveBarcodeInfoForSampleAndSampleItems(isNull(), anyInt(),
                org.mockito.ArgumentMatchers.anyMap());
    }
}
