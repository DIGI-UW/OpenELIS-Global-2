package org.openelisglobal.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.security.DaemonAuthenticationToken;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Central access point for the current user's identity. Works in all thread
 * contexts: HTTP requests, @Async methods, @Scheduled tasks, and daemon
 * operations.
 *
 * <p>
 * Provides both the full {@link SystemUser} object and the convenience
 * {@code sysUserId} string. The SystemUser is cached per principal name since
 * the login_name to system_user mapping is stable at runtime.
 */
@Component
public class UserContextHolder {

    private final ConcurrentMap<String, SystemUser> principalToUserCache = new ConcurrentHashMap<>();

    @Autowired
    private SystemUserService systemUserService;

    @Qualifier("daemonSystemUser")
    @Autowired
    private SystemUser daemonSystemUser;

    /**
     * Returns the current {@link SystemUser}, or null if no user context is
     * available.
     */
    public SystemUser getCurrentSysUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        if (auth instanceof DaemonAuthenticationToken) {
            return daemonSystemUser;
        }

        if (auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String principalName = auth.getName();
        if (principalName == null) {
            return null;
        }

        return principalToUserCache.computeIfAbsent(principalName, this::resolveSystemUser);
    }

    /**
     * Returns the current system user ID, or null if no user context is available.
     */
    public String getCurrentSysUserId() {
        SystemUser user = getCurrentSysUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Returns the current system user ID. Throws {@link LIMSRuntimeException} if no
     * user context is available.
     */
    public String requireSysUserId() {
        String userId = getCurrentSysUserId();
        if (userId == null || userId.isEmpty()) {
            throw new LIMSRuntimeException(
                    "No user context available. Ensure the operation runs within an authenticated "
                            + "request or daemon context.");
        }
        return userId;
    }

    /**
     * Returns true if the current thread is running as the daemon user.
     */
    public boolean isDaemonContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof DaemonAuthenticationToken;
    }

    /**
     * Returns the daemon system user ID.
     */
    public String getDaemonSysUserId() {
        return daemonSystemUser.getId();
    }

    /**
     * Returns the daemon {@link SystemUser}.
     */
    public SystemUser getDaemonSysUser() {
        return daemonSystemUser;
    }

    private SystemUser resolveSystemUser(String principalName) {
        try {
            SystemUser user = systemUserService.getDataForLoginUser(principalName);
            if (user != null) {
                return user;
            }
        } catch (Exception e) {
            LogEvent.logDebug(UserContextHolder.class.getSimpleName(), "resolveSystemUser",
                    "SystemUser lookup failed for principal '" + principalName + "': " + e.getMessage());
        }
        return null;
    }
}
