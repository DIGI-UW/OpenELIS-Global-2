package org.openelisglobal.resultrun.form;

import java.util.Set;

/**
 * What one user may see of a run (D-043): the tests of the lab units they hold
 * Results rights in, and those lab units, for rows not yet matched to a test.
 */
public record RunViewer(Set<String> testIds, Set<String> labUnitIds) {
}
