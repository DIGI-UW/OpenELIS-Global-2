package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Vector surveillance pool fan-out.
 *
 * <p>
 * When a vector order is submitted with {@code vecPoolCount > 1}, an
 * implementation:
 *
 * <ol>
 * <li>Creates {@code N} <strong>sibling</strong> {@link SampleItem}s (qty=1
 * each, no {@code parent_sample_item_id}) under the same {@code Sample} as the
 * original. Pool members are direct individual organisms, not aliquots of one
 * larger specimen.</li>
 * <li>Creates one {@link org.openelisglobal.vector.valueholder.VectorPool} row
 * and links each sibling to it through the {@code vector_pool_member} M:N join
 * table (see {@link org.openelisglobal.vector.valueholder.VectorPoolMember}).
 * The join-table model is forward-compatible with V-03 deconvolution, where the
 * same organism may appear in multiple pools across rounds.</li>
 * <li>Re-attaches the order's pool-level analyses to the new pool (sets
 * {@code analysis.vector_pool_id}, clears {@code analysis.sample_item}).
 * Pool-level tests (PCR on a homogenate of N organisms) are properties of the
 * pool, not any single member; analyses move down to a {@code SampleItem} only
 * when deconvolution narrows a pool to one organism.</li>
 * <li>Hard-deletes the original parent SampleItem (qty=N). Sample_item must
 * contain only individual organisms — the pool placeholder doesn't belong there
 * at all, voided or otherwise.</li>
 * </ol>
 */
public interface VectorPoolFanOutService {

    /**
     * Replace one parent SampleItem (qty=N) with N siblings (qty=1 each) and a
     * {@link org.openelisglobal.vector.valueholder.VectorPool} grouping them. No-op
     * when {@code poolCount <= 1}.
     *
     * @param original         the parent pool SampleItem (must already be
     *                         persisted)
     * @param originalAnalyses analyses currently attached to {@code original} that
     *                         should become pool-level analyses (re-FK'd to the new
     *                         pool). Pass an empty list if the order had no
     *                         analyses on this item.
     * @param poolCount        number of organisms in the pool; values <= 1 trigger
     *                         no-op
     * @param sysUserId        audit trail user id
     * @return the {@code N} persisted sibling SampleItems (empty list when no
     *         fan-out occurred). Order matches their pool position 1..N.
     */
    List<SampleItem> fanOut(SampleItem original, List<Analysis> originalAnalyses, int poolCount, String sysUserId);
}
