package org.openelisglobal.vector.deconvolution.service;

import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionOutcome;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionPreview;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;

public interface VectorDeconvolutionService {

    /**
     * @throws IllegalArgumentException malformed request
     * @throws IllegalStateException    BR gate failure (non-vector, qty&le;1,
     *                                  already IN_PROGRESS)
     */
    DeconvolutionResult initiate(DeconvolutionInitiateRequest request, String sysUserId);

    /**
     * Reflex preview. Read-only: returns the test orders that {@link #initiate}
     * would create on each sub-pool — straight copies of the parent pool's analyses
     * plus any reflex-driven analyses the active rules would emit. Does not persist
     * anything; safe to call repeatedly as the user adjusts the form.
     */
    DeconvolutionPreview previewReflexes(Long vectorPoolId);

    /**
     * Per-pool result-watcher. Called whenever any result lands on a pool-level
     * analysis. Flags the pool as PENDING (result available, awaiting tech
     * decision) regardless of whether the result is positive or negative — the
     * technician decides what to do next. Self-gates on VECTOR-domain and pools
     * with >1 member. Idempotent. Returns the new pool status when changed, null
     * otherwise.
     */
    String evaluateResultEntered(Long vectorPoolId, String sysUserId);

    /**
     * Confirm that the pool result applies to every individual vector in the pool.
     * Copies the pool-level analysis results down to a new SampleItem-anchored
     * analysis row for each pool member, marks those analyses Finalized, and sets
     * the pool's deconvolutionStatus to COMPLETE. Use this when deconvolution is
     * not needed.
     */
    void confirmResultForAllMembers(Long vectorPoolId, String sysUserId);

    /**
     * Walks up to the intake pool reachable from {@code anyPoolId} and, when every
     * sub-pool analysis is finalized, advances that intake pool to COMPLETE with
     * the positive-rate outcome. Sample-level rollup status is refreshed as part of
     * the same transaction. Self-gates on VECTOR-domain + intake-pool currently
     * IN_PROGRESS; safe to invoke on any analysis.
     */
    DeconvolutionOutcome evaluateChildResultsForCompletion(Long anyPoolId, String sysUserId);

    /**
     * Supervisor override: mark a specific intake pool's deconvolution as COMPLETE
     * without requiring every sub-pool result to be final. Validates VECTOR domain
     * and that the pool has a non-trivial deconvolution status (rejects
     * {@code NOT_APPLICABLE}). Writes via the service path so audit hooks fire
     * consistently.
     *
     * @throws IllegalArgumentException pool missing / non-vector domain / nothing
     *                                  to complete
     */
    void forceComplete(Long vectorPoolId, String sysUserId);
}
