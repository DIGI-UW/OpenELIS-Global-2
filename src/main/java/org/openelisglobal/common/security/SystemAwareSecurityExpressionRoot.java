package org.openelisglobal.common.security;

import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom expression root that satisfies {@code @PreAuthorize} gates for
 * system-initiated work, so code with no human user behind it can call gated
 * services. All other calls delegate to the standard Spring Security root.
 *
 * <p>
 * There are two routes, and they are not equivalent:
 *
 * <ul>
 * <li><b>{@code ROLE_SYSTEM}</b> — the caller holds a
 * {@code DaemonAuthenticationToken} in the SecurityContext. This is an
 * <em>identity</em>: it is visible to auditing, it propagates through
 * {@code SecurityContextHolder} like any other principal, and it is scoped to
 * the work that installed it. Prefer this. Install it with
 * {@code DaemonContextExecutor.executeAsDaemon(...)}.</li>
 * <li><b>{@link SystemInitFlag}</b> — a thread-local boolean that makes every
 * check below return {@code true} regardless of who is on the thread. This is
 * <em>not</em> an identity; it is a blanket override, it is invisible to
 * auditing, and when it is entered on a request thread it escalates the
 * <em>logged-in user</em> past gates they do not hold. It exists for the
 * startup window, before any SecurityContext can exist, and for a residual set
 * of request-thread call sites that are tracked for removal (see
 * specs/017-rbac-open-items/t2-systeminitflag-bypass.md). Do not add new call
 * sites.</li>
 * </ul>
 */
public class SystemAwareSecurityExpressionRoot implements MethodSecurityExpressionOperations {

    /**
     * Authority carried by {@code DaemonAuthenticationToken}. Declared here as a
     * literal rather than importing the token class, to keep this expression root
     * free of a dependency on the security package it is evaluated for.
     */
    static final String SYSTEM_ROLE = "ROLE_SYSTEM";

    private final MethodSecurityExpressionOperations delegate;

    public SystemAwareSecurityExpressionRoot(MethodSecurityExpressionOperations delegate) {
        this.delegate = delegate;
    }

    /**
     * True when this invocation is system-initiated: either the caller holds the
     * daemon identity ({@code ROLE_SYSTEM}), or the startup/legacy thread-local
     * override is active.
     *
     */
    private boolean isSystemInitiated() {
        // Flag first, deliberately. During startup there is no SecurityContext at
        // all, and the delegate's getAuthentication() THROWS
        // AuthenticationCredentialsNotFoundException rather than returning null —
        // so probing for the daemon role first would break every @PostConstruct
        // that calls a gated service. The flag check is also cheaper.
        return SystemInitFlag.isSet() || hasSystemRole();
    }

    private boolean hasSystemRole() {
        Authentication authentication;
        try {
            authentication = delegate.getAuthentication();
        } catch (AuthenticationCredentialsNotFoundException e) {
            // No SecurityContext on this thread: not the daemon, and not an error
            // here — the caller simply is not system-initiated.
            return false;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (SYSTEM_ROLE.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPrivilege(String name) {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.hasAuthority(name);
    }

    @Override
    public boolean hasAuthority(String authority) {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.hasAuthority(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.hasAnyAuthority(authorities);
    }

    @Override
    public boolean hasRole(String role) {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.hasRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.hasAnyRole(roles);
    }

    @Override
    public boolean permitAll() {
        return delegate.permitAll();
    }

    @Override
    public boolean denyAll() {
        return delegate.denyAll();
    }

    @Override
    public boolean isAnonymous() {
        return delegate.isAnonymous();
    }

    @Override
    public boolean isAuthenticated() {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.isAuthenticated();
    }

    @Override
    public boolean isRememberMe() {
        return delegate.isRememberMe();
    }

    @Override
    public boolean isFullyAuthenticated() {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.isFullyAuthenticated();
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.hasPermission(target, permission);
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        if (isSystemInitiated()) {
            return true;
        }
        return delegate.hasPermission(targetId, targetType, permission);
    }

    @Override
    public Authentication getAuthentication() {
        return delegate.getAuthentication();
    }

    @Override
    public void setFilterObject(Object filterObject) {
        delegate.setFilterObject(filterObject);
    }

    @Override
    public Object getFilterObject() {
        return delegate.getFilterObject();
    }

    @Override
    public void setReturnObject(Object returnObject) {
        delegate.setReturnObject(returnObject);
    }

    @Override
    public Object getReturnObject() {
        return delegate.getReturnObject();
    }

    @Override
    public Object getThis() {
        return delegate.getThis();
    }
}
