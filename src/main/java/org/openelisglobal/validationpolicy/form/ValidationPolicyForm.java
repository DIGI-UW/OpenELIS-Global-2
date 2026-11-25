package org.openelisglobal.validationpolicy.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

/**
 * A policy as the configuration page sends it: the review trigger (ALL,
 * NEEDS_REVIEW or NONE), the number of reviews and the role id for each level
 * in order. Roles may be left out; each level then takes the Validation role.
 * {@code lastupdated} is the version the page loaded (OGC-1017); a save from a
 * page that loaded an older version is refused. Without it nothing is checked.
 */
public record ValidationPolicyForm(String trigger, Integer levels, List<Integer> roleIds, String lastupdated) {

    @JsonCreator
    public ValidationPolicyForm {
    }

    public ValidationPolicyForm(String trigger, Integer levels, List<Integer> roleIds) {
        this(trigger, levels, roleIds, null);
    }
}
