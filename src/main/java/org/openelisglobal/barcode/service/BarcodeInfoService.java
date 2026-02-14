package org.openelisglobal.barcode.service;

import org.openelisglobal.sample.valueholder.Sample;

public interface BarcodeInfoService {

    void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels, int numSpecimenLabels);

    void saveBarcodeInfoForSampleAndSampleItemsPathology(Sample sample, int numOrderLabels, int numSpecimenLabels,
            int numBlockLabels, int numSlideLabels, int numFreezerLabels);

}
