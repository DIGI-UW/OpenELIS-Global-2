package org.openelisglobal.common.security;

import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * Custom expression root that short-circuits all authority checks when
 * SystemInitFlag is set (application startup phase), allowing @PostConstruct
 * methods to call @PreAuthorize-protected services without an auth context.
 * All other calls are delegated to the standard Spring Security expression root.
 */
public class SystemAwareSecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final MethodSecurityExpressionOperations delegate;

    public SystemAwareSecurityExpressionRoot(MethodSecurityExpressionOperations delegate) {
        this.delegate = delegate;
    }

    public boolean hasPrivilege(String name) {
        if (SystemInitFlag.isSet()) {
            return true;
        }
        return delegate.hasAuthority(name);
    }

    @Override
    public boolean hasAuthority(String authority) {
        if (SystemInitFlag.isSet()) {
            return true;
        }
        return delegate.hasAuthority(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        if (SystemInitFlag.isSet()) {
            return true;
        }
        return delegate.hasAnyAuthority(authorities);
    }

    @Override
    public boolean hasRole(String role) {
        if (SystemInitFlag.isSet()) {
            return true;
        }
        return delegate.hasRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        if (SystemInitFlag.isSet()) {
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
        if (SystemInitFlag.isSet()) {
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
        if (SystemInitFlag.isSet()) {
            return true;
        }
        return delegate.isFullyAuthenticated();
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        if (SystemInitFlag.isSet()) {
            return true;
        }
        return delegate.hasPermission(target, permission);
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        if (SystemInitFlag.isSet()) {
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
