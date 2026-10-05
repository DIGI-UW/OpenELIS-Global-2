package org.openelisglobal.systemuser.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    void updateLoginUser(LoginUser loginUser, boolean loginUserNew, SystemUser systemUser, boolean systemUserNew,
            List<String> selectedRoles, String loggedOnUserId);

    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    void saveUserLabUnitRoles(SystemUser systemUser, Map<String, Set<String>> selectedLabUnitRolesMap,
            String loggedOnUserId);

    // Returns another user's role and lab-unit assignments —
    // identity/administration
    // data, not result data. Must match the delegate
    // (UserRoleService.getUserLabUnitRoles,
    // PRIV_USER_ROLE_VIEW) so routing through UserService cannot downgrade the
    // required
    // privilege and let a result-viewer enumerate any user's RBAC assignments.
    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_VIEW')")
    UserLabUnitRoles getUserLabUnitRoles(String systemUserId);

    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    List<UserLabUnitRoles> getAllUserLabUnitRoles();

    /**
     * Ungated self-scoping identity read. EVERY caller passes the CALLER'S OWN id
     * ({@code getSysUserId(request)} or {@code usd.getSystemUserId()}) to scope
     * their own view — it answers "which lab units am I assigned to", which is
     * self-identity, not result data.
     *
     * <p>
     * It was gated on {@code PRIV_RESULT_VIEW}, which Reception does not hold (see
     * 012-004: result:view goes to Results/Validation/Pathologist/Cytopathologist/
     * Reports/EQA Coordinator only). {@code MenuController.resolveAllowedDomains}
     * calls this on {@code GET /rest/menu} — hit on every page load, with no
     * try/catch — so a Reception-only user got AccessDeniedException there. The
     * SecurityConfig AccessDeniedHandler only converts to 403 JSON for paths it
     * matches after the exception escapes the handler method, so in practice
     * /rest/menu returned 500, the frontend treated the failed bootstrap as a dead
     * session and bounced to /login. That is what took out 52 core E2E specs (run
     * 33568673124) while the Results and Validation personas passed.
     *
     * <p>
     * Callers that must restrict to a specific role still pass {@code roleId}; the
     * lab-unit filtering itself is unchanged. Cross-user assignment reads remain
     * gated ({@link #getAllUserLabUnitRoles()} at PRIV_USER_MANAGE).
     */
    List<IdValuePair> getUserTestSections(String systemUserId, String roleId);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    /**
     * OGC-189 (M2): the lab units to offer in a <em>viewer</em> control — a filter
     * over work the user is looking at (Results, Workplan, by-unit reports,
     * history), as opposed to a <em>chooser</em> ("assign this to a lab unit"),
     * which must keep using {@link #getUserTestSections}.
     *
     * <p>
     * Viewers show {@code isActive OR hasContent}: {@link #getUserTestSections}
     * returns active units only, so a unit deactivated with analyses still in
     * flight would drop off the worklists and strand them. This adds those units
     * back, and only those — a deactivated unit with nothing left in it stays
     * hidden, so the list cleans itself up as work completes.
     *
     * <p>
     * Authorization is unchanged: the user's own lab-unit roles still gate the
     * result, so this can never widen what someone may see.
     */
    List<IdValuePair> getUserViewerTestSections(String systemUserId, String roleId);

    List<IdValuePair> getUserSampleTypes(String systemUserId, String userRole);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getAllDisplayUserTestsByLabUnit(String SystemUserId, String roleName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<AnalysisItem> filterAnalysisResultsByLabUnitRoles(String SystemUserId, List<AnalysisItem> results,
            String roleName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    /**
     * Ids of every test in the lab units this user holds the given role for. The
     * predicate behind {@link #filterAnalysesByLabUnitRoles}, exposed separately so
     * a caller that pages or caps its query can push the same scope into SQL
     * instead of discarding rows after the fact.
     */
    List<String> getUserTestIdsForLabUnitRoles(String systemUserId, String roleName);

    List<Analysis> filterAnalysesByLabUnitRoles(String SystemUserId, List<Analysis> results, String roleName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestResultItem> filterResultsByLabUnitRoles(String SystemUserId, List<TestResultItem> results,
            String roleName);

    /**
     * The ids of the active tests in the lab units the user holds {@code roleName}
     * in: the single definition of which rows a lab-unit-restricted user may see,
     * shared by Results, Validation and Incoming Orders.
     */
    Set<String> getTestIdsInUserLabUnits(String systemUserId, String roleName);

    /**
     * Whether the user's lab units for {@code roleName} cover every active lab
     * unit: a global administrator or a user mapped to all lab units.
     */
    boolean hasAllLabUnits(String systemUserId, String roleName);

    /**
     * The programs this user may order under. Read from the cached display list,
     * which is rebuilt only when a program is saved: a program deleted straight
     * from the database therefore lingers there until the next restart, so an id
     * that no longer resolves to a record is dropped rather than dereferenced.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<IdValuePair> getUserPrograms(String systemUserId, String userRole);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<IdValuePair> getUserSampleTypes(String systemUserId, String roleName, String testSectionName);
}
