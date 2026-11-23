package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.stereotype.Component;

/**
 * Who may cancel a test that has left Not started (R-34): an administrator, or
 * a user holding a validation role. Modify Order and the order screens apply
 * the same rule.
 */
@Component
public class TestCancelPermission {

    public static final List<String> ROLE_NAMES = List.of("Validator", "Validation", "Biologist");

    private final UserModuleService userModuleService;
    private final UserRoleService userRoleService;

    public TestCancelPermission(UserModuleService userModuleService, UserRoleService userRoleService) {
        this.userModuleService = userModuleService;
        this.userRoleService = userRoleService;
    }

    public boolean mayCancelStartedTests(HttpServletRequest request, String sysUserId) {
        return userModuleService.isUserAdmin(request) || userRoleService.userInRole(sysUserId, ROLE_NAMES);
    }
}
