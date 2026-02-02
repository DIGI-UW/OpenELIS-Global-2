package org.openelisglobal.barcode.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.valueholder.SampleBarcodeInfo;
import org.openelisglobal.barcode.valueholder.SampleItemBarcodeInfo;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@RunWith(MockitoJUnitRunner.class)
public class BarcodeInfoServiceImplTest {

    @Mock
    private SampleBarcodeInfoService sampleBarcodeInfoService;

    @Mock
    private SampleItemBarcodeInfoService sampleItemBarcodeInfoService;

    @Mock
    private SampleItemService sampleItemService;

    @InjectMocks
    private BarcodeInfoServiceImpl barcodeInfoService;

    private Sample sample;
    private List<SampleItem> sampleItems;

    @Before
    public void setUp() {
        sample = new Sample();
        sample.setId("sample-1");
        sample.setAccessionNumber("2025-00001");

        SampleItem item1 = new SampleItem();
        item1.setId("item-1");
        item1.setSortOrder("1");
        SampleItem item2 = new SampleItem();
        item2.setId("item-2");
        item2.setSortOrder("2");

        sampleItems = new ArrayList<>();
        sampleItems.add(item1);
        sampleItems.add(item2);

        when(sampleBarcodeInfoService.getAllMatching(eq("sample"), eq(sample))).thenReturn(Collections.emptyList());
        when(sampleItemBarcodeInfoService.getAllMatching(eq("sampleItem"), any(SampleItem.class)))
                .thenReturn(Collections.emptyList());
        when(sampleItemService.getSampleItemsBySampleId(sample.getId())).thenReturn(sampleItems);
    }

    @Test
    public void saveBarcodeInfoForSampleAndSampleItems_insertsWhenNoExisting() {
        barcodeInfoService.saveBarcodeInfoForSampleAndSampleItems(sample, 2, 3);

        ArgumentCaptor<SampleBarcodeInfo> sampleInfoCaptor = ArgumentCaptor.forClass(SampleBarcodeInfo.class);
        verify(sampleBarcodeInfoService).insert(sampleInfoCaptor.capture());
        SampleBarcodeInfo savedSampleInfo = sampleInfoCaptor.getValue();
        assertNotNull(savedSampleInfo);
        assertEquals(sample, savedSampleInfo.getSample());
        assertEquals(Integer.valueOf(2), savedSampleInfo.getPrintOrderNum());

        ArgumentCaptor<SampleItemBarcodeInfo> itemInfoCaptor = ArgumentCaptor.forClass(SampleItemBarcodeInfo.class);
        verify(sampleItemBarcodeInfoService, org.mockito.Mockito.times(2)).insert(itemInfoCaptor.capture());
        List<SampleItemBarcodeInfo> savedItemInfos = itemInfoCaptor.getAllValues();
        assertEquals(2, savedItemInfos.size());
        assertEquals(sampleItems.get(0), savedItemInfos.get(0).getSampleItem());
        assertEquals(Integer.valueOf(3), savedItemInfos.get(0).getPrintSpecimenNum());
        assertEquals(sampleItems.get(1), savedItemInfos.get(1).getSampleItem());
        assertEquals(Integer.valueOf(3), savedItemInfos.get(1).getPrintSpecimenNum());
    }

    @Test
    public void saveBarcodeInfoForSampleAndSampleItems_updatesWhenExisting() {
        SampleBarcodeInfo existingSampleInfo = new SampleBarcodeInfo();
        existingSampleInfo.setId(1);
        existingSampleInfo.setSample(sample);
        existingSampleInfo.setPrintOrderNum(1);
        when(sampleBarcodeInfoService.getAllMatching(eq("sample"), eq(sample)))
                .thenReturn(Collections.singletonList(existingSampleInfo));

        SampleItemBarcodeInfo existingItemInfo = new SampleItemBarcodeInfo();
        existingItemInfo.setId(10);
        existingItemInfo.setSampleItem(sampleItems.get(0));
        when(sampleItemBarcodeInfoService.getAllMatching(eq("sampleItem"), eq(sampleItems.get(0))))
                .thenReturn(Collections.singletonList(existingItemInfo));
        when(sampleItemBarcodeInfoService.getAllMatching(eq("sampleItem"), eq(sampleItems.get(1))))
                .thenReturn(Collections.emptyList());

        barcodeInfoService.saveBarcodeInfoForSampleAndSampleItems(sample, 5, 4);

        verify(sampleBarcodeInfoService).update(existingSampleInfo);
        assertEquals(Integer.valueOf(5), existingSampleInfo.getPrintOrderNum());

        verify(sampleItemBarcodeInfoService).update(existingItemInfo);
        assertEquals(Integer.valueOf(4), existingItemInfo.getPrintSpecimenNum());
        verify(sampleItemBarcodeInfoService).insert(any(SampleItemBarcodeInfo.class));
    }

    @Test
    public void saveBarcodeInfoForSampleAndSampleItemsPathology_setsAllPerItemFields() {
        // Arrange: one existing sample record, first item updates, second inserts
        SampleBarcodeInfo existingSampleInfo = new SampleBarcodeInfo();
        existingSampleInfo.setId(1);
        existingSampleInfo.setSample(sample);
        when(sampleBarcodeInfoService.getAllMatching(eq("sample"), eq(sample)))
                .thenReturn(Collections.singletonList(existingSampleInfo));

        SampleItemBarcodeInfo existingItemInfo = new SampleItemBarcodeInfo();
        existingItemInfo.setId(10);
        existingItemInfo.setSampleItem(sampleItems.get(0));
        when(sampleItemBarcodeInfoService.getAllMatching(eq("sampleItem"), eq(sampleItems.get(0))))
                .thenReturn(Collections.singletonList(existingItemInfo));
        when(sampleItemBarcodeInfoService.getAllMatching(eq("sampleItem"), eq(sampleItems.get(1))))
                .thenReturn(Collections.emptyList());

        // Act
        barcodeInfoService.saveBarcodeInfoForSampleAndSampleItemsPathology(sample, 2, 3, 4, 5, 6);

        // Assert: sample updated
        verify(sampleBarcodeInfoService).update(existingSampleInfo);
        assertEquals(Integer.valueOf(2), existingSampleInfo.getPrintOrderNum());

        // Assert: first item updated with all fields
        verify(sampleItemBarcodeInfoService).update(existingItemInfo);
        assertEquals(Integer.valueOf(3), existingItemInfo.getPrintSpecimenNum());
        assertEquals(Integer.valueOf(4), existingItemInfo.getPrintBlockNum());
        assertEquals(Integer.valueOf(5), existingItemInfo.getPrintSlideNum());
        assertEquals(Integer.valueOf(6), existingItemInfo.getPrintFreezerNum());

        // Assert: second item inserted
        verify(sampleItemBarcodeInfoService).insert(any(SampleItemBarcodeInfo.class));
    }
}
