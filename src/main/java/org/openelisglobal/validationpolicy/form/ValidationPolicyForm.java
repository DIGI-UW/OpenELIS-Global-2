package org.openelisglobal.validationpolicy.form;

import java.util.List;

/**
 * A policy as the configuration page sends it: the review trigger (ALL,
 * NEEDS_REVIEW or NONE), the number of reviews and the role id for each level
 * in order. Roles may be left out; each level then takes the Validation role.
 */
public record ValidationPolicyForm(String trigger, Integer levels, List<Integer> roleIds) {
}
