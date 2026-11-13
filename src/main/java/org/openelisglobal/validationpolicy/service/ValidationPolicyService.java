package org.openelisglobal.validationpolicy.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.validationpolicy.form.LabUnitPoliciesView;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyForm;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyView;
import org.openelisglobal.validationpolicy.form.LevelRoleOption;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.openelisglobal.validationpolicy.valueholder.ReviewTrigger;
import org.openelisglobal.validationpolicy.valueholder.ValidationPolicy;

/**
 * OGC-1364: the validation policy. A lab-wide default and lab unit overrides,
 * each with up to five sequential reviews and the role that signs each level.
 */
public interface ValidationPolicyService {

    int MAX_LEVELS = 5;

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

    /**
     * The policy a lab unit follows when useMultiLevelValidation is on: its active
     * override, otherwise the lab-wide default. Empty when the flag is off.
     */
    Optional<ValidationPolicy> effectivePolicy(String testSectionId);

    /**
     * The roles a review level can ask for: active roles that are not groupings,
     * Validation first, each with how many active users could sign it for the lab
     * unit (any unit when the id is null).
     */
    List<LevelRoleOption> levelRoleOptions(String labUnitId);
}
