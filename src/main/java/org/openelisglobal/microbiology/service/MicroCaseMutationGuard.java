package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCaseStatus;

final class MicroCaseMutationGuard {

    private MicroCaseMutationGuard() {
    }

    static void requireMutable(MicroCase microCase) {
        if (microCase.getStatus() == MicroCaseStatus.CANCELLED || microCase.getStatus() == MicroCaseStatus.REJECTED
                || MicroCaseStage.REJECTED.name().equals(microCase.getStage())) {
            throw new MicroCaseLockedException("TERMINAL_CASE_LOCKED");
        }
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
