package org.openelisglobal.validationpolicy.form;

/**
 * A role a review level can ask for.
 *
 * @param labUnitRole whether it is held per lab unit, rather than lab-wide
 * @param signers     how many active users could sign a level with this role
 *                    for the lab unit asked about (any unit when none)
 */
public record LevelRoleOption(Integer id, String name, boolean labUnitRole, int signers) {
}
