package org.openelisglobal.validationpolicy.form;

import java.util.List;

/**
 * The lab-wide default as the configuration page shows it, with what the page
 * needs to keep the editor within what this version supports.
 *
 * @param enabled             whether useMultiLevelValidation is on, so the
 *                            policy decides instead of "validate all results"
 * @param maxLevels           the most reviews a policy may ask for in this
 *                            version
 * @param blockSelfValidation whether self-validation is blocked, which keeps
 *                            every result waiting for review
 */
public record ValidationPolicyView(String trigger, int levels, List<Level> levelRoles, boolean enabled, int maxLevels,
        boolean blockSelfValidation) {

    public record Level(int level, Integer roleId, String roleName) {
    }
}
