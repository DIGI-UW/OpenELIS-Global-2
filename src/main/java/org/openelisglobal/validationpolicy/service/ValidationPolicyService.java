package org.openelisglobal.validationpolicy.service;

import java.util.Optional;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.openelisglobal.validationpolicy.valueholder.ReviewTrigger;

/**
 * OGC-1364: the validation policy. This version has the lab-wide default only,
 * with at most one review signed by the Validation role.
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
     * Which results wait for review at entry when useMultiLevelValidation is on;
     * empty when it is off and "validate all results" decides.
     */
    Optional<ReviewTrigger> entryReviewTrigger();
}
