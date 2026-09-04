package org.openelisglobal.barcode.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.security.access.prepost.PreAuthorize;

public interface BarcodeInfoService {

    @PreAuthorize("hasAuthority('PRIV_BARCODE_MANAGE')")
    void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels, int numSpecimenLabels);

    @PreAuthorize("hasAuthority('PRIV_BARCODE_MANAGE')")
    void saveBarcodeInfoForSampleAndSampleItems(Sample sample, int numOrderLabels,
            Map<SampleItem, Integer> specimenLabelQuantities);

    @PreAuthorize("hasAuthority('PRIV_BARCODE_MANAGE')")
    void saveBarcodeInfoForSampleAndSampleItemsPathology(Sample sample, int numOrderLabels, int numSpecimenLabels,
            int numBlockLabels, int numSlideLabels, int numFreezerLabels);

    /**
     * FR-012a: increment cumulative printed counts for labels that were just
     * printed.
     */
    @PreAuthorize("hasAuthority('PRIV_BARCODE_MANAGE')")
    void recordPrintedCounts(String labNo, List<Label> labels);

}
