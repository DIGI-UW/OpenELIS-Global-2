package org.openelisglobal.sampleacceptance.service;

import org.openelisglobal.sample.valueholder.Sample;

/**
 * Resample action (S-09 / OGC-580, FR-10), per failed specimen. In one atomic
 * transaction: record an NCE against the specimen, reject just that
 * {@code sample_item} (canceling its analyses; the order's accepted specimens
 * are untouched and proceed), and create a draft replacement order cloned from
 * the parent order carrying only this specimen (same site/program, sample type,
 * tests, customer, requester, GPS, and questionnaire/observation answers; fresh
 * lab number), with bidirectional {@code resampled_from}/{@code resampled_to}
 * links between the parent order and the replacement. These steps are
 * all-or-nothing — any failure rolls everything back and leaves the original
 * untouched. The requester notification (FR-12) is then fired
 * <em>post-commit</em> through the existing Notification Trigger system, so a
 * send failure follows the retry policy rather than rolling back the resample.
 */
public interface ResampleService {

    /**
     * Commit a resample for one failed specimen.
     *
     * @param sampleItemId the rejected specimen's {@code sample_item} id
     * @param reason       the NCE reason (typically pre-filled from failed
     *                     acceptance-checklist items)
     * @param userId       the acting system user id
     * @return identifiers of the parent order, the new draft order, and the NCE
     */
    ResampleResult resample(String sampleItemId, String reason, Integer userId);

    /**
     * Plainly reject a single specimen — mark its {@code sample_item} rejected and
     * cancel its analyses, with <em>no</em> replacement order (unlike
     * {@link #resample}). Used for vector specimens, where a one-time field catch
     * cannot be re-collected. The {@code reason} is kept as the specimen's
     * rejection note.
     */
    void reject(String sampleItemId, String reason, Integer userId);

    /**
     * Reject a single specimen for one of the lab's rejection reasons, with an
     * optional comment. The reason id is stored on the {@code sample_item} and the
     * reason (with the comment) as its rejection note. An EQA sample cannot be
     * rejected without a reason (OGC-1488).
     *
     * @throws IllegalArgumentException when the reason is unknown, or missing for
     *                                  an EQA sample
     */
    void reject(String sampleItemId, String rejectReasonId, String reason, Integer userId);

    /** Whether the order is an EQA / proficiency-testing sample. */
    boolean isEqa(Sample sample);

    /**
     * Reject an entire vector pool: cascade the plain reject (see {@link #reject})
     * to every live member {@code sample_item} of the pool, all-or-nothing. No
     * replacement order. Voided/already-rejected members are skipped. The
     * {@code reason} is kept as each member's rejection note.
     */
    void rejectPool(Integer vectorPoolId, String reason, Integer userId);
}
