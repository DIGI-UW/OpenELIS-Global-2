package org.openelisglobal.common.security;

import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Custom expression root that short-circuits all authority checks when
 * SystemInitFlag is set (application startup phase) OR when the calling thread
 * holds ROLE_SYSTEM authentication (background schedulers, config loaders). All
 * other calls are delegated to the standard Spring Security expression root.
 */
public class SystemAwareSecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final MethodSecurityExpressionOperations delegate;

    public SystemAwareSecurityExpressionRoot(MethodSecurityExpressionOperations delegate) {
        this.delegate = delegate;
    }

    private boolean isSystemCaller() {
        if (SystemInitFlag.isSet()) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String threadName = Thread.currentThread().getName();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> SystemAuthentication.ROLE_SYSTEM.equals(a.getAuthority()))) {
            return true;
        }
        // Background scheduler/async threads have no web request context so auth is
        // null. Web request threads always get at minimum anonymous auth from
        // AnonymousAuthenticationFilter. Null auth + known scheduler/async thread name
        // = safe to treat as system caller.
        if (auth == null) {
            if (threadName.startsWith("SimpleAsyncTaskExecutor-") || threadName.startsWith("pool-")
                    || threadName.startsWith("scheduling-") || threadName.startsWith("MyScheduler_Worker-")
                    || threadName.startsWith("task-")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPrivilege(String name) {
        if (isSystemCaller()) {
            return true;
        }
        return delegate.hasAuthority(name);
    }

    @Override
    public boolean hasAuthority(String authority) {
        if (isSystemCaller()) {
            return true;
        }
        return delegate.hasAuthority(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        if (isSystemCaller()) {
            return true;
        }
        return delegate.hasAnyAuthority(authorities);
    }

    @Override
    public boolean hasRole(String role) {
        if (isSystemCaller()) {
            return true;
        }
        return delegate.hasRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        if (isSystemCaller()) {
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
        if (isSystemCaller()) {
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
        if (isSystemCaller()) {
            return true;
        }
        return delegate.isFullyAuthenticated();
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        if (isSystemCaller()) {
            return true;
        }
        return delegate.hasPermission(target, permission);
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        if (isSystemCaller()) {
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
