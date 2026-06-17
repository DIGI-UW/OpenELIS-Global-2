package org.openelisglobal.barcode.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.barcode.labeltype.BlockLabel;
import org.openelisglobal.barcode.labeltype.FreezerLabel;
import org.openelisglobal.barcode.labeltype.OrderLabel;
import org.openelisglobal.barcode.labeltype.SlideLabel;
import org.openelisglobal.barcode.labeltype.SpecimenLabel;
import org.openelisglobal.barcode.valueholder.SampleBarcodeInfo;
import org.openelisglobal.barcode.valueholder.SampleItemBarcodeInfo;
import org.openelisglobal.sample.service.SampleService;
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

    @Autowired
    private SampleService sampleService;

    @Override
    public void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels, int numSpecimenLabels) {
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        Map<SampleItem, Integer> specimenLabelQuantities = new LinkedHashMap<>();
        for (SampleItem sampleItem : sampleItems) {
            specimenLabelQuantities.put(sampleItem, numSpecimenLabels);
        }
        saveBarcodeInfoForSampleAndSampleItems(sample, numOrderLabels, specimenLabelQuantities);
    }

    @Override
    public void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels,
            Map<SampleItem, Integer> specimenLabelQuantities) {
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

        if (specimenLabelQuantities == null || specimenLabelQuantities.isEmpty()) {
            return;
        }

        for (Map.Entry<SampleItem, Integer> entry : specimenLabelQuantities.entrySet()) {
            SampleItem sampleItem = entry.getKey();
            if (sampleItem == null) {
                continue;
            }
            List<SampleItemBarcodeInfo> existingItemInfo = sampleItemBarcodeInfoService.getAllMatching("sampleItem",
                    sampleItem);
            SampleItemBarcodeInfo itemInfo;
            int normalizedSpecimenLabels = normalizeConfiguredLabelCount(entry.getValue());
            if (!existingItemInfo.isEmpty()) {
                itemInfo = existingItemInfo.get(0);
                itemInfo.setPrintSpecimenNum(normalizedSpecimenLabels);
                sampleItemBarcodeInfoService.update(itemInfo);
            } else {
                itemInfo = new SampleItemBarcodeInfo();
                itemInfo.setSampleItem(sampleItem);
                itemInfo.setPrintSpecimenNum(normalizedSpecimenLabels);
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

    @Override
    public void recordPrintedCounts(String labNo, List<org.openelisglobal.barcode.labeltype.Label> labels) {
        if (labels == null || labels.isEmpty()) {
            return;
        }
        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        if (sample == null) {
            return;
        }
        for (org.openelisglobal.barcode.labeltype.Label label : labels) {
            int count = label.getNumLabels();
            if (count <= 0) {
                continue;
            }
            if (label instanceof OrderLabel) {
                incrementPrintedOrderCount(sample, count);
            } else if (label instanceof SpecimenLabel) {
                SampleItem item = ((SpecimenLabel) label).getSampleItem();
                if (item != null) {
                    incrementPrintedSpecimenCount(item, count);
                }
            } else if (label instanceof BlockLabel) {
                incrementPrintedPathologyCount(sample, count, PathologyLabelType.BLOCK);
            } else if (label instanceof SlideLabel) {
                incrementPrintedPathologyCount(sample, count, PathologyLabelType.SLIDE);
            } else if (label instanceof FreezerLabel) {
                incrementPrintedPathologyCount(sample, count, PathologyLabelType.FREEZER);
            }
        }
    }

    private void incrementPrintedOrderCount(Sample sample, int count) {
        List<SampleBarcodeInfo> existing = sampleBarcodeInfoService.getAllMatching("sample", sample);
        SampleBarcodeInfo info;
        if (!existing.isEmpty()) {
            info = existing.get(0);
        } else {
            info = new SampleBarcodeInfo();
            info.setSample(sample);
            info.setPrintedOrderCount(0);
            info = sampleBarcodeInfoService.get(sampleBarcodeInfoService.insert(info));
        }
        int current = info.getPrintedOrderCount() != null ? info.getPrintedOrderCount() : 0;
        info.setPrintedOrderCount(current + count);
        sampleBarcodeInfoService.update(info);
    }

    private void incrementPrintedSpecimenCount(SampleItem sampleItem, int count) {
        List<SampleItemBarcodeInfo> existing = sampleItemBarcodeInfoService.getAllMatching("sampleItem", sampleItem);
        SampleItemBarcodeInfo info;
        if (!existing.isEmpty()) {
            info = existing.get(0);
        } else {
            info = new SampleItemBarcodeInfo();
            info.setSampleItem(sampleItem);
            info.setPrintedSpecimenCount(0);
            info = sampleItemBarcodeInfoService.get(sampleItemBarcodeInfoService.insert(info));
        }
        int current = info.getPrintedSpecimenCount() != null ? info.getPrintedSpecimenCount() : 0;
        info.setPrintedSpecimenCount(current + count);
        sampleItemBarcodeInfoService.update(info);
    }

    private void incrementPrintedPathologyCount(Sample sample, int count, PathologyLabelType labelType) {
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (sampleItems == null || sampleItems.isEmpty()) {
            return;
        }
        for (SampleItem sampleItem : sampleItems) {
            incrementPrintedPathologyCount(sampleItem, count, labelType);
        }
    }

    private void incrementPrintedPathologyCount(SampleItem sampleItem, int count, PathologyLabelType labelType) {
        List<SampleItemBarcodeInfo> existing = sampleItemBarcodeInfoService.getAllMatching("sampleItem", sampleItem);
        SampleItemBarcodeInfo info;
        if (!existing.isEmpty()) {
            info = existing.get(0);
        } else {
            info = new SampleItemBarcodeInfo();
            info.setSampleItem(sampleItem);
            sampleItemBarcodeInfoService.insert(info);
        }
        if (labelType == PathologyLabelType.BLOCK) {
            int current = info.getPrintedBlockCount() != null ? info.getPrintedBlockCount() : 0;
            info.setPrintedBlockCount(current + count);
        } else if (labelType == PathologyLabelType.SLIDE) {
            int current = info.getPrintedSlideCount() != null ? info.getPrintedSlideCount() : 0;
            info.setPrintedSlideCount(current + count);
        } else if (labelType == PathologyLabelType.FREEZER) {
            int current = info.getPrintedFreezerCount() != null ? info.getPrintedFreezerCount() : 0;
            info.setPrintedFreezerCount(current + count);
        }
        sampleItemBarcodeInfoService.update(info);
    }

    private enum PathologyLabelType {
        BLOCK, SLIDE, FREEZER
    }

    private int normalizeConfiguredLabelCount(Integer count) {
        return count != null && count > 0 ? count : 1;
    }

}
