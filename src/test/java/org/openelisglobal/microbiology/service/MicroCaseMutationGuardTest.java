package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

public class MicroCaseMutationGuardTest {

    @Test
    public void finalCaseRemainsLockedWithoutAmendment() {
        MicroCase microCase = new MicroCase();
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());

        try {
            MicroCaseMutationGuard.requireMutable(microCase);
            fail("Expected final case to remain locked");
        } catch (MicroCaseLockedException expected) {
            assertEquals("FINAL_CASE_LOCKED", expected.getMessage());
        }
    }

    @Test
    public void activeAmendmentAllowsControlledMutation() {
        MicroCase microCase = new MicroCase();
        microCase.setStage(MicroCaseStage.AMENDED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());

        MicroCaseMutationGuard.requireMutable(microCase);
    }
}
