package org.openelisglobal.validationpolicy.form;

import java.util.List;

/**
 * A lab unit override as the configuration page lists it.
 *
 * @param domain the lab unit's domain: CLINICAL, ENVIRONMENTAL or VECTOR
 * @param active whether the override decides for the unit; a deactivated one
 *               hands the unit back to the lab-wide default
 */
public record LabUnitPolicyView(Long id, String testSectionId, String labUnitName, String domain, String trigger,
        int levels, List<ValidationPolicyView.Level> levelRoles, boolean active) {
}
