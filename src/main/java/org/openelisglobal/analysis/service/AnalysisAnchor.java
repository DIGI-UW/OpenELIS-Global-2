package org.openelisglobal.analysis.service;

import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * The Sample + representative SampleItem owning an Analysis.
 *
 * <p>
 * Sample-item-anchored analyses ({@code sampitem_id} set): {@code sampleItem}
 * is the direct sample_item, {@code sample} is its parent Sample.
 *
 * <p>
 * Vector pool-anchored analyses ({@code vector_pool_id} set,
 * {@code sampitem_id} null per {@code ck_analysis_pool_or_item}):
 * {@code sample} comes from {@code vector_pool.sample_id}; {@code sampleItem}
 * is the first non-voided pool member as a representative for display fields
 * (sort order, sample type) — all organisms in a pool share those fields.
 */
public final class AnalysisAnchor {

    private final Sample sample;
    private final SampleItem sampleItem;

    public AnalysisAnchor(Sample sample, SampleItem sampleItem) {
        this.sample = sample;
        this.sampleItem = sampleItem;
    }

    public Sample getSample() {
        return sample;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }
}
