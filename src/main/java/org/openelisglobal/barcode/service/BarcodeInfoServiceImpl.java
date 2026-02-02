package org.openelisglobal.barcode.service;

import java.util.List;
import org.openelisglobal.barcode.valueholder.SampleBarcodeInfo;
import org.openelisglobal.barcode.valueholder.SampleItemBarcodeInfo;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BarcodeInfoServiceImpl implements BarcodeInfoService {

    @Autowired
    private SampleBarcodeInfoService sampleBarcodeInfoService;

    @Autowired
    private SampleItemBarcodeInfoService sampleItemBarcodeInfoService;

    @Autowired
    private SampleItemService sampleItemService;

    @Override
    public void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels, int numSpecimenLabels) {
        List<SampleBarcodeInfo> existingSampleInfo = sampleBarcodeInfoService.getAllMatching("sample", sample);
        SampleBarcodeInfo sampleBarcodeInfo;
        if (!existingSampleInfo.isEmpty()) {
            sampleBarcodeInfo = existingSampleInfo.get(0);
            sampleBarcodeInfo.setPrintOrderNum(numOrderLabels);
            sampleBarcodeInfoService.update(sampleBarcodeInfo);
        } else {
            sampleBarcodeInfo = new SampleBarcodeInfo();
            sampleBarcodeInfo.setSample(sample);
            sampleBarcodeInfo.setPrintOrderNum(numOrderLabels);
            sampleBarcodeInfoService.insert(sampleBarcodeInfo);
        }

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        for (SampleItem sampleItem : sampleItems) {
            List<SampleItemBarcodeInfo> existingItemInfo = sampleItemBarcodeInfoService.getAllMatching("sampleItem",
                    sampleItem);
            SampleItemBarcodeInfo itemInfo;
            if (!existingItemInfo.isEmpty()) {
                itemInfo = existingItemInfo.get(0);
                itemInfo.setPrintSpecimenNum(numSpecimenLabels);
                sampleItemBarcodeInfoService.update(itemInfo);
            } else {
                itemInfo = new SampleItemBarcodeInfo();
                itemInfo.setSampleItem(sampleItem);
                itemInfo.setPrintSpecimenNum(numSpecimenLabels);
                sampleItemBarcodeInfoService.insert(itemInfo);
            }
        }
    }

    @Override
    public void saveBarcodeInfoForSampleAndSampleItemsPathology(Sample sample, int numOrderLabels,
            int numSpecimenLabels, int numBlockLabels, int numSlideLabels, int numFreezerLabels) {
        List<SampleBarcodeInfo> existingSampleInfo = sampleBarcodeInfoService.getAllMatching("sample", sample);
        SampleBarcodeInfo sampleBarcodeInfo;
        if (!existingSampleInfo.isEmpty()) {
            sampleBarcodeInfo = existingSampleInfo.get(0);
            sampleBarcodeInfo.setPrintOrderNum(numOrderLabels);
            sampleBarcodeInfoService.update(sampleBarcodeInfo);
        } else {
            sampleBarcodeInfo = new SampleBarcodeInfo();
            sampleBarcodeInfo.setSample(sample);
            sampleBarcodeInfo.setPrintOrderNum(numOrderLabels);
            sampleBarcodeInfoService.insert(sampleBarcodeInfo);
        }

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        for (SampleItem sampleItem : sampleItems) {
            List<SampleItemBarcodeInfo> existingItemInfo = sampleItemBarcodeInfoService.getAllMatching("sampleItem",
                    sampleItem);
            SampleItemBarcodeInfo itemInfo;
            if (!existingItemInfo.isEmpty()) {
                itemInfo = existingItemInfo.get(0);
                itemInfo.setPrintSpecimenNum(numSpecimenLabels);
                itemInfo.setPrintBlockNum(numBlockLabels);
                itemInfo.setPrintSlideNum(numSlideLabels);
                itemInfo.setPrintFreezerNum(numFreezerLabels);
                sampleItemBarcodeInfoService.update(itemInfo);
            } else {
                itemInfo = new SampleItemBarcodeInfo();
                itemInfo.setSampleItem(sampleItem);
                itemInfo.setPrintSpecimenNum(numSpecimenLabels);
                itemInfo.setPrintBlockNum(numBlockLabels);
                itemInfo.setPrintSlideNum(numSlideLabels);
                itemInfo.setPrintFreezerNum(numFreezerLabels);
                sampleItemBarcodeInfoService.insert(itemInfo);
            }
        }
    }

}
