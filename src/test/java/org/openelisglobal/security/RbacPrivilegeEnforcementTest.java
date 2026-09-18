package org.openelisglobal.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Integration test: verifies the full RBAC enforcement chain against a real DB.
 *
 * Chain under test: DB privilege row → CustomUserDetailsService →
 * SecurityContext authority → @PreAuthorize("hasPrivilege(...)") →
 * SystemAwareSecurityExpressionRoot
 */
public class RbacPrivilegeEnforcementTest extends BaseWebContextSensitiveTest {

    // AppTestConfig excludes org.openelisglobal.security.login.* from its scan, so
    // the DB-backed CustomUserDetailsService is not a bean and cannot be autowired.
    // This test exercises exactly that chain (DB privilege -> authority), so build
    // one directly and let the context autowire its fields — avoiding a nested
    // @Configuration, which would fork the shared test ApplicationContext and
    // corrupt the cache for sibling tests in the same fork.
    @Autowired
    private org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory;

    private UserDetailsService userDetailsService;

    @Autowired
    private PatientService patientService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/rbac-privilege-enforcement.xml");
        // Build the DB-backed service and inject its @Autowired collaborators from
        // the running context (it is not a registered bean here — see field note).
        org.openelisglobal.security.login.CustomUserDetailsService uds = new org.openelisglobal.security.login.CustomUserDetailsService();
        beanFactory.autowireBean(uds);
        this.userDetailsService = uds;
    }

    // --- CustomUserDetailsService loads correct authorities from DB ---

    @Test
    public void privilegedUser_hasPatientViewAuthority() {
        UserDetails details = userDetailsService.loadUserByUsername("rbac_priv_user");
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertTrue("PRIV_PATIENT_VIEW authority must be present for user with patient:view privilege",
                authorities.stream().anyMatch(a -> "PRIV_PATIENT_VIEW".equals(a.getAuthority())));
    }

    @Test
    public void unprivilegedUser_lacksPatientViewAuthority() {
        UserDetails details = userDetailsService.loadUserByUsername("rbac_nopriv_user");
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertFalse("PRIV_PATIENT_VIEW authority must NOT be present for user without patient:view privilege",
                authorities.stream().anyMatch(a -> "PRIV_PATIENT_VIEW".equals(a.getAuthority())));
    }

    // --- @PreAuthorize enforcement via real SecurityContext ---

    @Test
    public void privilegedUser_canCallPreAuthorizeProtectedMethod() {
        authenticateWithRealPrivileges("rbac_priv_user");
        Patient patient = patientService.getData("901");
        assertNotNull("Privileged user must be able to retrieve a patient", patient);
    }

    @Test(expected = AccessDeniedException.class)
    public void unprivilegedUser_isBlockedByPreAuthorize() {
        authenticateWithRealPrivileges("rbac_nopriv_user");
        patientService.getData("99999999");
    }

    private void authenticateWithRealPrivileges(String username) {
        UserDetails details = userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
