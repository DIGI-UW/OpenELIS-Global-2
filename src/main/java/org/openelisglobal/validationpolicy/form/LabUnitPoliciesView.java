package org.openelisglobal.validationpolicy.form;

import java.util.List;

/**
 * The lab unit overrides, deactivated ones included, and the active lab units,
 * which the page offers for a new override and lists in the effective policy.
 */
public record LabUnitPoliciesView(List<LabUnit> labUnits, List<LabUnitPolicyView> overrides) {

    public record LabUnit(String id, String name, String domain) {
    }
}
