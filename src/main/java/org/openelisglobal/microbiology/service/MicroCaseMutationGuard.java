package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

/**
 * Keeps the MVP immutable after final release until amendment history exists.
 */
final class MicroCaseMutationGuard {

    private MicroCaseMutationGuard() {
    }

    static void requireMutable(MicroCase microCase) {
        if (MicroCaseStage.FINAL_RELEASED.name().equals(microCase.getStage())
                || MicroCaseFinalReleaseState.FINAL_RELEASED.name().equals(microCase.getFinalReleaseState())) {
            throw new MicroCaseLockedException(
                    "Final-released microbiology cases cannot be changed until amendment history is available");
        }
    }
}
