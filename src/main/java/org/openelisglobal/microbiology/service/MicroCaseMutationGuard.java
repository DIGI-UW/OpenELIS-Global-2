package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

final class MicroCaseMutationGuard {

    private MicroCaseMutationGuard() {
    }

    static void requireMutable(MicroCase microCase) {
        if (MicroCaseStage.AMENDED.name().equals(microCase.getStage())
                && MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name().equals(microCase.getFinalReleaseState())) {
            return;
        }
        if (MicroCaseStage.FINAL_RELEASED.name().equals(microCase.getStage())
                || MicroCaseFinalReleaseState.FINAL_RELEASED.name().equals(microCase.getFinalReleaseState())) {
            throw new MicroCaseLockedException("FINAL_CASE_LOCKED");
        }
    }
}
