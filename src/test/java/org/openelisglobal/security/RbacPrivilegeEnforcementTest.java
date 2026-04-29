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

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PatientService patientService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/rbac-privilege-enforcement.xml");
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
        authenticateAs("rbac_priv_user");
        Patient patient = patientService.getData("901");
        assertNotNull("Privileged user must be able to retrieve a patient", patient);
    }

    @Test(expected = AccessDeniedException.class)
    public void unprivilegedUser_isBlockedByPreAuthorize() {
        authenticateAs("rbac_nopriv_user");
        patientService.getData("99999999");
    }

    private void authenticateAs(String username) {
        UserDetails details = userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
