package org.openelisglobal.microbiology.service;

/**
 * Ungated by design: this IS an authorization check, not a guarded operation.
 *
 * <p>
 * Both methods take the subject's own {@code systemUserId} and answer "may this
 * user see this case", so requiring a privilege to ask the question would be
 * circular — the caller would need permission before it could decide whether
 * the caller has permission. Same rationale as
 * {@code UserRoleService#userInRole} and
 * {@code UserService#getUserTestSections}, which are ungated for the same
 * reason and asserted by {@code SelfIdentityMethodsUngatedTest}.
 *
 * <p>
 * The endpoint that consults it (MicroCaseRestController) carries the real
 * privilege gate; this only narrows what that endpoint then returns.
 */
public interface MicrobiologyCaseAccessService {

    boolean canAccessCase(String caseId, String systemUserId, boolean administrator);

    boolean canAccessSampleItem(String sampleItemId, String systemUserId, boolean administrator);
}
