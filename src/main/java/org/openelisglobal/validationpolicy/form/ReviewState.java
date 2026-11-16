package org.openelisglobal.validationpolicy.form;

import java.util.List;

/**
 * OGC-1364 v2: where one analysis stands in its sequential review, as seen by
 * one user.
 *
 * @param levels        the reviews the analysis needs
 * @param nextLevel     the level the next signature signs, from 1 to levels
 * @param nextRoleId    the role that signs it
 * @param nextRoleName  that role's name
 * @param signOffs      the signatures of the current pass, by level
 * @param superseded    signatures a modification, retest or rejection
 *                      superseded
 * @param blockedReason why this user cannot sign the next level, or null:
 *                      alreadySigned, roleRequired, validationRightsRequired,
 *                      selfValidationBlocked
 */
public record ReviewState(int levels, int nextLevel, Integer nextRoleId, String nextRoleName, List<SignOff> signOffs,
        List<SignOff> superseded, String blockedReason) {

    public boolean finalLevel() {
        return nextLevel >= levels;
    }

    public record SignOff(int level, int levels, String signerName, String roleName, String signedAt,
            String supersededReason, String supersededAt) {
    }
}
