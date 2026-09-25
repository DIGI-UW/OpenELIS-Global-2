package org.openelisglobal.analysis.service;

import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Resolves the Sample (and a representative SampleItem) that owns an Analysis,
 * regardless of whether it's anchored via {@code sampitem_id} or
 * {@code vector_pool_id}. Centralises the navigation that would otherwise NPE
 * on pool-anchored rows.
 *
 * <p>
 * Cannot live on {@link Analysis} itself: Hibernate's HBM XML uses property
 * (getter) access for dirty-checking, so any lazy resolution inside
 * {@code getSampleItem()} would make Hibernate believe {@code sampitem_id}
 * changed from null to the representative member's id, and try to UPDATE the
 * row — violating {@code ck_analysis_pool_or_item}. So the resolution lives in
 * a separate service and callers opt in.
 */
public interface AnalysisAnchorService {

    /**
     * @return Sample + representative SampleItem, or null if neither anchor
     *         resolves. For pool-anchored analyses with no resolvable members,
     *         {@link AnalysisAnchor#getSampleItem()} may still be null even when
     *         {@link AnalysisAnchor#getSample()} is non-null.
     */
    AnalysisAnchor resolveAnchor(Analysis analysis);

    /**
     * Sample-only resolution — skips the pool-member lookup for callers that don't
     * need a SampleItem.
     */
    Sample resolveSample(Analysis analysis);
}
