package org.openelisglobal.common.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;

public class SystemAwareSecurityExpressionRootTest {

    private MethodSecurityExpressionOperations delegate;
    private SystemAwareSecurityExpressionRoot root;

    @Before
    public void setUp() {
        delegate = mock(MethodSecurityExpressionOperations.class);
        root = new SystemAwareSecurityExpressionRoot(delegate);
    }

    @After
    public void tearDown() {
        SystemInitFlag.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    public void hasPrivilege_returnsTrue_whenSystemInitFlagSet() {
        SystemInitFlag.set();
        assertTrue(root.hasPrivilege("ANY_PRIVILEGE"));
        verify(delegate, never()).hasAuthority("ANY_PRIVILEGE");
    }

    @Test
    public void hasPrivilege_returnsTrue_whenNoAuthInContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertTrue(root.hasPrivilege("ANY_PRIVILEGE"));
        verify(delegate, never()).hasAuthority("ANY_PRIVILEGE");
    }

    @Test
    public void hasPrivilege_delegatesToAuthority_whenAuthPresent() {
        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);

        when(delegate.hasAuthority("PATIENT_VIEW")).thenReturn(true);
        assertTrue(root.hasPrivilege("PATIENT_VIEW"));
        verify(delegate).hasAuthority("PATIENT_VIEW");
    }

    @Test
    public void hasPrivilege_returnsFalse_whenAuthPresentAndNotGranted() {
        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);

        when(delegate.hasAuthority("PATIENT_VIEW")).thenReturn(false);
        assertFalse(root.hasPrivilege("PATIENT_VIEW"));
    }

    @Test
    public void hasAuthority_shortCircuits_whenSystemInitFlagSet() {
        SystemInitFlag.set();
        assertTrue(root.hasAuthority("ANY"));
        verify(delegate, never()).hasAuthority("ANY");
    }

    @Test
    public void isAuthenticated_returnsTrue_whenSystemInitFlagSet() {
        SystemInitFlag.set();
        assertTrue(root.isAuthenticated());
        verify(delegate, never()).isAuthenticated();
    }
}
