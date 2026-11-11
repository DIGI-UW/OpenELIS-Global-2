package org.openelisglobal.qc.service;

import java.util.Collection;
import java.util.Set;

/**
 * OGC-1147 — the QC-fail signal as Validation consumes it.
 */
public interface QcHoldService {

    /**
     * Of the given analyses, those covered by a still-open QC failure. Used to
     * annotate validation rows; always answered regardless of the blocking policy,
     * because a lab that only warns still needs to see which results are affected.
     */
    Set<String> heldAnalysisIds(Collection<String> analysisIds);

    /**
     * Of the given analyses, those that must not be released right now. Empty when
     * the lab has not opted into blocking. Fail-closed: an unresolvable hold
     * blocks.
     */
    Set<String> analysisIdsBlockedFromRelease(Collection<String> analysisIds);

    /**
     * Whether this lab blocks release on an open QC failure, or only warns.
     */
    boolean blocksRelease();
}
