package org.openelisglobal.validationpolicy.service;

import java.util.Optional;
import org.openelisglobal.validationpolicy.form.LabUnitPoliciesView;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyForm;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyView;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.openelisglobal.validationpolicy.valueholder.ReviewTrigger;

/**
 * OGC-1364: the validation policy. This version has the lab-wide default and
 * lab unit overrides, with at most one review signed by the Validation role.
 */
public interface ValidationPolicyService {

    int MAX_LEVELS = 1;

    String LEVEL_ROLE_NAME = "Validation";

    ValidationPolicyView getLabWideDefault();

    /**
     * Saves the lab-wide default and writes "validate all results" to match, so
     * turning useMultiLevelValidation off keeps the same behaviour.
     *
     * @throws ValidationPolicyRefusedException naming the field the policy breaks
     */
    ValidationPolicyView saveLabWideDefault(ValidationPolicyForm form, String sysUserId);

    /**
     * The lab unit overrides, deactivated ones included, and the active lab units.
     */
    LabUnitPoliciesView getLabUnitPolicies();

    /**
     * Adds an override for an active lab unit that has none, active or deactivated.
     *
     * @throws ValidationPolicyRefusedException naming the field the override breaks
     */
    LabUnitPolicyView addLabUnitOverride(LabUnitPolicyForm form, String sysUserId);

    /**
     * Changes an active override's policy.
     *
     * @throws ValidationPolicyRefusedException when the override is unknown or
     *                                          deactivated, or the policy breaks a
     *                                          rule
     */
    LabUnitPolicyView saveLabUnitOverride(Long id, ValidationPolicyForm form, String sysUserId);

    /**
     * Deactivates an override, handing its unit back to the lab-wide default, or
     * reactivates it with the policy it had.
     */
    LabUnitPolicyView setLabUnitOverrideActive(Long id, boolean active, String sysUserId);

    /**
     * Which results of the lab unit wait for review at entry when
     * useMultiLevelValidation is on: the unit's active override, otherwise the
     * lab-wide default. Empty when the flag is off and "validate all results"
     * decides.
     */
    Optional<ReviewTrigger> entryReviewTrigger(String testSectionId);
}
