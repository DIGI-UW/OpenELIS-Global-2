package org.openelisglobal.security.login;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userDetailsService")
@Primary
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    LoginUserService loginService;

    @Autowired
    UserRoleService userRoleService;

    @Autowired
    RoleService roleService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginName) {
        LoginUser user = loginService.getMatch("loginName", loginName).orElseThrow(() -> new UsernameNotFoundException(
                "Unique Username not found, could be duplicates in database or it doesn't" + " exist"));

        boolean disabled = user.getAccountDisabled().equalsIgnoreCase(IActionConstants.YES);
        boolean locked = user.getAccountLocked().equalsIgnoreCase(IActionConstants.YES);
        boolean credentialsExpired = user.getPasswordExpiredDayNo() <= 0;
        return new org.springframework.security.core.userdetails.User(user.getLoginName(), user.getPassword(),
                !disabled, true, !credentialsExpired, !locked, getGrantedAuthorities(user));
    }

    private List<GrantedAuthority> getGrantedAuthorities(LoginUser user) {
        Set<String> authorityNames = new LinkedHashSet<>();

        if (user != null && user.getSystemUserId() > 0) {
            List<String> roleIds = userRoleService.getRoleIdsForUser(String.valueOf(user.getSystemUserId()));
            if (roleIds != null) {
                for (String roleId : roleIds) {
                    if (roleId == null || roleId.trim().isEmpty()) {
                        continue;
                    }
                    Role role = roleService.getRoleById(roleId.trim());
                    if (role != null && role.getName() != null && !role.getName().trim().isEmpty()) {
                        authorityNames.add(toRoleAuthority(role.getName()));
                        if (Constants.ROLE_GLOBAL_ADMIN.equalsIgnoreCase(role.getName())) {
                            authorityNames.add("ROLE_ADMIN");
                        }
                    }
                }
            }
        }

        if (user != null && IActionConstants.YES.equalsIgnoreCase(user.getIsAdmin())) {
            authorityNames.add(toRoleAuthority(Constants.ROLE_GLOBAL_ADMIN));
            authorityNames.add("ROLE_ADMIN");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String authorityName : authorityNames) {
            authorities.add(new SimpleGrantedAuthority(authorityName));
        }
        return authorities;
    }

    private String toRoleAuthority(String roleName) {
        if (Constants.ROLE_GLOBAL_ADMIN.equalsIgnoreCase(roleName)) {
            return "ROLE_GLOBAL_ADMIN";
        }
        if (Constants.ROLE_USER_ACCOUNT_ADMIN.equalsIgnoreCase(roleName)) {
            return "ROLE_USER_ACCOUNT_ADMIN";
        }
        String normalized = roleName.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }
        return "ROLE_" + normalized;
    }
}
