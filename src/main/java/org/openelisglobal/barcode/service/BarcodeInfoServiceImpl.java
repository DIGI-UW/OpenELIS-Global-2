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
                SampleBarcodeInfo sampleBarcodeInfo = new SampleBarcodeInfo();
                sampleBarcodeInfo.setSample(sample);
                sampleBarcodeInfo.setPrintOrderNum(numOrderLabels);

                List<SampleItemBarcodeInfo> sampleItemBarcodeInfos = new ArrayList<>();
                List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                for (SampleItem sampleItem : sampleItems) {
                        sampleBarcodeInfo
                }
                
        }

        @Override
        public void saveBarcodeInfoForSampleAndSampleItemsPathology(Sample sample, int numOrderLabels,
                        int numSpecimenLabels, int numBlockLabels, int numSlideLabels, int numFreezerLabels) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException(
                                "Unimplemented method 'saveBarcodeInfoForSampleAndSampleItemsPathology'");
        }

}
