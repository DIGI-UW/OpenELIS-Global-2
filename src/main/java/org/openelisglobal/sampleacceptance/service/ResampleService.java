package org.openelisglobal.sampleacceptance.service;

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
     * cannot be re-collected. The {@code reason} is accepted for API symmetry and
     * the (deferred) rejection notification; it is not persisted on the item (there
     * is no free-text reject-reason column).
     */
    void reject(String sampleItemId, String reason, Integer userId);

    /**
     * Reject an entire vector pool: cascade the plain reject (see {@link #reject})
     * to every live member {@code sample_item} of the pool, all-or-nothing. No
     * replacement order. Voided/already-rejected members are skipped.
     */
    void rejectPool(Integer vectorPoolId, String reason, Integer userId);
}
