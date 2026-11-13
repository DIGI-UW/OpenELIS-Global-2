package org.openelisglobal.validationpolicy.form;

import java.util.List;

/**
 * A lab unit override as the configuration page adds it: the lab unit (test
 * section id) and its policy, with the same fields as the lab-wide default.
 */
public record LabUnitPolicyForm(String testSectionId, String trigger, Integer levels, List<Integer> roleIds) {

    public ValidationPolicyForm policy() {
        return new ValidationPolicyForm(trigger, levels, roleIds);
    }
}
