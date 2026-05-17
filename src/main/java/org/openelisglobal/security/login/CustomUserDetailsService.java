package org.openelisglobal.security.login;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.constants.Privileges;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.privilege.service.PrivilegeService;
import org.openelisglobal.privilege.valueholder.Privilege;
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

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");

    @Autowired
    LoginUserService loginService;

    @Autowired
    UserRoleService userRoleService;

    @Autowired
    RoleService roleService;

    @Autowired
    PrivilegeService privilegeService;

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
            List<Integer> roleIds = userRoleService.getRoleIdsForUser(String.valueOf(user.getSystemUserId()));
            if (roleIds != null) {
                for (Integer roleId : roleIds) {
                    if (roleId == null) {
                        continue;
                    }
                    Role role = roleService.getRoleById(roleId);
                    if (role != null && role.getName() != null && !role.getName().trim().isEmpty()) {
                        addAuthoritiesForRole(role.getName(), authorityNames);
                    }
                }
            }
        }

        if (user != null && IActionConstants.YES.equalsIgnoreCase(user.getIsAdmin())) {
            addAuthoritiesForRole(Constants.ROLE_GLOBAL_ADMIN, authorityNames);
        }

        // Load resolved privileges as PRIV_ authorities
        if (user != null && user.getSystemUserId() > 0) {
            Set<String> resolvedPrivileges = privilegeService
                    .getAllPrivilegesForUser(String.valueOf(user.getSystemUserId()));
            if (resolvedPrivileges.contains(Privileges.GLOBAL_ADMIN_SENTINEL)) {
                resolvedPrivileges = new HashSet<>();
                for (Privilege p : privilegeService.getAllPrivileges()) {
                    resolvedPrivileges.add(p.getName());
                }
            }
            for (String priv : resolvedPrivileges) {
                authorityNames.add(toPrivAuthority(priv));
            }
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String authorityName : authorityNames) {
            authorities.add(new SimpleGrantedAuthority(authorityName));
        }
        return authorities;
    }

    /*
     * Shared with KeycloakAuthoritiesExtractor so SAML/SSO logins produce the same
     * ROLE_* authorities as form logins; @PreAuthorize("hasRole('ADMIN')")
     * otherwise fails for SSO users because Keycloak ships role names like
     * "oeg-Global Administrator".
     */
    public static void addAuthoritiesForRole(String roleName, Set<String> sink) {
        if (roleName == null) {
            return;
        }
        String trimmed = roleName.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        sink.add(toRoleAuthority(trimmed));
        if (Constants.ROLE_GLOBAL_ADMIN.equalsIgnoreCase(trimmed)) {
            sink.add("ROLE_ADMIN");
        }
    }

    public static String toPrivAuthority(String privName) {
        String normalized = NON_ALNUM.matcher(privName.toUpperCase()).replaceAll("_");
        normalized = normalized.replaceAll("^_+|_+$", "").replaceAll("__+", "_");
        return "PRIV_" + normalized;
    }

    public static String toRoleAuthority(String roleName) {
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
