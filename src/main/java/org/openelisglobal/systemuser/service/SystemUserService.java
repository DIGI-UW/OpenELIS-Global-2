package org.openelisglobal.systemuser.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SystemUserService extends BaseObjectService<SystemUser, String> {

    /**
     * Populates a SystemUser from its id - in practice to put an author's name on a
     * record.
     *
     * <p>
     * Every caller does exactly that: NoteServiceImpl#createSystemUser attaches the
     * note's author, ResultsLoadUtility names who entered a result, and
     * NonConformityUpdateWorker names who raised the event. Gated on
     * PRIV_SYSTEM_USER_MANAGE - granted to no role at all - saving a result with a
     * note answered 403 for the Results role, because writing the note resolves its
     * own author. Reading a user's display name is not user administration, so it
     * takes PRIV_SYSTEM_USER_VIEW; the administrative writes on this interface keep
     * PRIV_SYSTEM_USER_MANAGE.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_SYSTEM_USER_MANAGE','PRIV_SYSTEM_USER_VIEW')")
    void getData(SystemUser systemUser);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
    List<SystemUser> getPageOfSystemUsers(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
    List<SystemUser> getPagesOfSearchedUsers(int startRecNo, String searchString);

    // A name lookup, not user administration: its three callers populate the
    // NCE dashboard's "performed by" picker, the EQA analyst-competency list
    // and the QC alert recipient list, all reached by non-admin roles. Under
    // PRIV_SYSTEM_USER_MANAGE (held by no seeded role) every one of them 403s.
    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_VIEW')")
    List<SystemUser> getAllSystemUsers();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
    Integer getTotalSystemUserCount();

    // Called by Spring Security login handlers and result/order workflows — not
    // admin-only
    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_VIEW')")
    SystemUser getDataForLoginUser(String name);

    // Called from result/order workflows to resolve the current user
    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_VIEW')")
    SystemUser getUserById(String userId);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
    Integer getTotalSearchedUserCount(String searchString);
}
