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
    public void hasPrivilege_delegatesToAuthority_whenNoAuthInContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        when(delegate.hasAuthority("ANY_PRIVILEGE")).thenReturn(false);
        assertFalse(root.hasPrivilege("ANY_PRIVILEGE"));
        verify(delegate).hasAuthority("ANY_PRIVILEGE");
    }

    @Test
    public void hasPrivilege_delegatesToAuthority_whenAuthPresent() {
        org.springframework.security.core.Authentication auth = mock(
                org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(delegate.hasAuthority("PATIENT_VIEW")).thenReturn(true);
        assertTrue(root.hasPrivilege("PATIENT_VIEW"));
        verify(delegate).hasAuthority("PATIENT_VIEW");
    }

    @Test
    public void hasPrivilege_returnsFalse_whenAuthPresentAndNotGranted() {
        org.springframework.security.core.Authentication auth = mock(
                org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

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

    // --- ROLE_SYSTEM (daemon identity) ---
    //
    // These cover the route that replaces SystemInitFlag for system-initiated work:
    // a caller holding DaemonAuthenticationToken satisfies PRIV_* gates by
    // IDENTITY,
    // with no thread-local override involved. See T2.

    private static org.springframework.security.core.Authentication authWith(String... authorities) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("who", "creds",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList(authorities));
    }

    @Test
    public void hasAuthority_isSatisfied_byRoleSystem_withoutTheFlag() {
        when(delegate.getAuthentication()).thenReturn(authWith("ROLE_SYSTEM"));

        assertFalse("precondition: the thread-local override must NOT be active", SystemInitFlag.isSet());
        assertTrue(root.hasAuthority("PRIV_ANYTHING"));
        verify(delegate, never()).hasAuthority("PRIV_ANYTHING");
    }

    @Test
    public void hasPrivilege_isSatisfied_byRoleSystem_withoutTheFlag() {
        when(delegate.getAuthentication()).thenReturn(authWith("ROLE_SYSTEM"));

        assertTrue(root.hasPrivilege("PRIV_ANYTHING"));
        verify(delegate, never()).hasAuthority("PRIV_ANYTHING");
    }

    /**
     * Inversion: an ordinary authenticated user must NOT be treated as system. If
     * hasSystemRole() ever matched on something broader than the exact ROLE_SYSTEM
     * authority, this is the test that fails.
     */
    @Test
    public void ordinaryUser_isNotTreatedAsSystem() {
        when(delegate.getAuthentication()).thenReturn(authWith("PRIV_RESULT_VIEW", "ROLE_RESULTS"));
        when(delegate.hasAuthority("PRIV_PATIENT_DELETE")).thenReturn(false);

        assertFalse(root.hasAuthority("PRIV_PATIENT_DELETE"));
        verify(delegate).hasAuthority("PRIV_PATIENT_DELETE");
    }

    /**
     * A near-miss authority must not be mistaken for the daemon identity — guards
     * against a prefix/contains check creeping in.
     */
    @Test
    public void similarlyNamedAuthority_isNotTreatedAsSystem() {
        when(delegate.getAuthentication()).thenReturn(authWith("ROLE_SYSTEM_ADMIN", "PRIV_SYSTEM"));
        when(delegate.hasAuthority("PRIV_PATIENT_DELETE")).thenReturn(false);

        assertFalse(root.hasAuthority("PRIV_PATIENT_DELETE"));
    }

    @Test
    public void unauthenticatedToken_carryingRoleSystem_isNotTreatedAsSystem() {
        // The 3-arg UsernamePasswordAuthenticationToken constructor marks itself
        // authenticated and refuses setAuthenticated(false), so build an
        // unauthenticated token that still advertises ROLE_SYSTEM directly.
        org.springframework.security.core.Authentication token = mock(
                org.springframework.security.core.Authentication.class);
        when(token.isAuthenticated()).thenReturn(false);
        org.mockito.Mockito.<java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>>when(
                token.getAuthorities()).thenReturn(
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_SYSTEM"));
        when(delegate.getAuthentication()).thenReturn(token);
        when(delegate.hasAuthority("PRIV_ANYTHING")).thenReturn(false);

        assertFalse(root.hasAuthority("PRIV_ANYTHING"));
    }

    @Test
    public void noAuthentication_doesNotThrow_andDelegates() {
        when(delegate.getAuthentication()).thenReturn(null);
        when(delegate.hasAuthority("PRIV_ANYTHING")).thenReturn(false);

        assertFalse(root.hasAuthority("PRIV_ANYTHING"));
        verify(delegate).hasAuthority("PRIV_ANYTHING");
    }

    /**
     * denyAll() must stay deny even for the daemon — it is the one expression whose
     * whole purpose is to be unsatisfiable.
     */
    @Test
    public void denyAll_staysDenied_forRoleSystem() {
        when(delegate.getAuthentication()).thenReturn(authWith("ROLE_SYSTEM"));
        when(delegate.denyAll()).thenReturn(false);

        assertFalse(root.denyAll());
    }
}
