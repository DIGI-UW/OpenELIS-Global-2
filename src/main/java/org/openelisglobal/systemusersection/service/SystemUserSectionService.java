package org.openelisglobal.systemusersection.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.systemusersection.valueholder.SystemUserSection;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "result/workplan filtering (all authenticated users) and admin (PRIV_USER_ROLE_MANAGE) — getAllSystemUserSectionsBySystemUserId is public; admin methods are guarded per-method")
public interface SystemUserSectionService extends BaseObjectService<SystemUserSection, String> {

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    void getData(SystemUserSection systemUserSection);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    List<SystemUserSection> getAllSystemUserSections();

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    List<SystemUserSection> getPageOfSystemUserSections(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    Integer getTotalSystemUserSectionCount();

    // Called during result/workplan filtering to determine which sections a user
    // can see
    List<SystemUserSection> getAllSystemUserSectionsBySystemUserId(int systemUserId);
}
