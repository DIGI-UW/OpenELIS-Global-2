package org.openelisglobal.systemuser.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SystemUserService extends BaseObjectService<SystemUser, String> {

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
    void getData(SystemUser systemUser);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
    List<SystemUser> getPageOfSystemUsers(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
    List<SystemUser> getPagesOfSearchedUsers(int startRecNo, String searchString);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_USER_MANAGE')")
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
