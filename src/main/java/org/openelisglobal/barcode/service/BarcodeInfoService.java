package org.openelisglobal.barcode.service;

import java.util.List;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.sample.valueholder.Sample;

public interface BarcodeInfoService {

    void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels, int numSpecimenLabels);

    void saveBarcodeInfoForSampleAndSampleItemsPathology(Sample sample, int numOrderLabels, int numSpecimenLabels,
            int numBlockLabels, int numSlideLabels, int numFreezerLabels);

    /**
     * FR-012a: increment cumulative printed counts for labels that were just
     * printed.
     */
    void recordPrintedCounts(String labNo, List<Label> labels);

}
