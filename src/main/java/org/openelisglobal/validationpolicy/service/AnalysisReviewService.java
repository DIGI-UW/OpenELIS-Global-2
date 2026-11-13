package org.openelisglobal.validationpolicy.service;

import java.util.Collection;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.validationpolicy.form.ReviewState;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewSignOff.SupersededReason;

/**
 * OGC-1364 v2: sequential sign-off. With useMultiLevelValidation on, a result
 * awaiting validation needs one signature per review level of its lab unit's
 * policy, each by a different person holding Validation rights for the unit and
 * the level's role. Only the last level releases it. Modifying, retesting or
 * rejecting it supersedes the signatures and review restarts at level 1.
 */
public interface AnalysisReviewService {

    /** Whether sequential sign-off applies: useMultiLevelValidation is on. */
    boolean isEnabled();

    /** Each analysis's review state for the user, by analysis id. */
    Map<String, ReviewState> statesFor(Collection<Analysis> analyses, String sysUserId);

    ReviewState stateOf(Analysis analysis, String sysUserId);

    /**
     * Signs the next level of an analysis whose next level is not its last. The
     * analysis stays awaiting validation.
     *
     * @throws ReviewSignRefusedException when the user cannot sign it, or the next
     *                                    level is the last (release it instead)
     */
    ReviewState signEarlierLevel(Analysis analysis, String sysUserId, Long signatureId);

    /**
     * Checks the user may sign the last level, then returns the step that records
     * that signature inside the release transaction.
     *
     * @throws ReviewSignRefusedException when the user cannot sign it, or levels
     *                                    before it are still unsigned
     */
    IResultUpdate finalLevelRecorder(Analysis analysis, String sysUserId, Long signatureId);

    /**
     * The step that supersedes the analysis's current signatures inside the
     * transaction that modifies, retests or rejects it.
     */
    IResultUpdate restartRecorder(Analysis analysis, SupersededReason reason, String sysUserId);

    /**
     * Supersedes the analysis's current signatures because one of its result values
     * changed, wherever the change was saved (the Results page, an analyzer import,
     * a correction). Review restarts at level 1. Does nothing while the flag is
     * off.
     */
    void resultValueChanged(String analysisId, String sysUserId);

    /**
     * How many active users could sign a level with this role: Validation rights
     * for the lab unit and the role. Without a lab unit (the lab-wide default),
     * Validation rights for any lab unit count.
     */
    int signerCount(String labUnitId, Integer roleId);
}
