package org.openelisglobal.barcode.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface BarcodeInfoService {

    void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels, int numSpecimenLabels);

    void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels,
            Map<SampleItem, Integer> specimenLabelQuantities);

    void saveBarcodeInfoForSampleAndSampleItemsPathology(Sample sample, int numOrderLabels, int numSpecimenLabels,
            int numBlockLabels, int numSlideLabels, int numFreezerLabels);

    /**
     * FR-012a: increment cumulative printed counts for labels that were just
     * printed.
     */
    void recordPrintedCounts(String labNo, List<Label> labels);

}
