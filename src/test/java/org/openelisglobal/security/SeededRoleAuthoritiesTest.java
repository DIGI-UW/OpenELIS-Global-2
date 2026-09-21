package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;

public class SeededRoleAuthoritiesTest {

    private static Set<String> names(List<GrantedAuthority> as) {
        return as.stream().map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    public void readsBothSeedRowShapes() {
        // "r.name = ... AND p.name = ..." rows
        assertTrue(SeededRoleAuthorities.grants().get("Reception").contains("order:create"));
        // "p.name IN (...)" rows (012-004d)
        assertTrue(SeededRoleAuthorities.grants().get("Results").contains("micro:view"));
        assertTrue("expected the full seed, got " + SeededRoleAuthorities.grants(),
                SeededRoleAuthorities.grants().values().stream().mapToInt(Set::size).sum() >= 50);
    }

    @Test
    public void springRoleAndSeedNameAreTheSameUser() {
        assertEquals(names(SeededRoleAuthorities.role("Results")), names(SeededRoleAuthorities.role("RESULTS")));
        assertEquals(names(SeededRoleAuthorities.role("Analyser Import")),
                names(SeededRoleAuthorities.role("ANALYSER_IMPORT")));
    }

    @Test
    public void roleCarriesItsRoleAuthorityAndItsSeededPrivileges_andNothingElse() {
        Set<String> results = names(SeededRoleAuthorities.role("RESULTS"));
        assertTrue(results.contains("ROLE_RESULTS"));
        assertTrue(results.contains("PRIV_RESULT_ENTER"));
        // Inversion: a role user is NOT an everything-user. report:run is seeded to
        // Reports, not Results; if this passes for Results the helper is lying.
        assertFalse(results.contains("PRIV_REPORT_RUN"));
        assertFalse(results.contains("ROLE_ADMIN"));
    }

    @Test
    public void adminHoldsEveryPrivilegeConstant() {
        Set<String> admin = names(SeededRoleAuthorities.admin());
        assertTrue(admin.contains("ROLE_ADMIN"));
        assertTrue(admin.containsAll(SeededRoleAuthorities.allPrivilegeAuthorityNames()));
        assertEquals(names(SeededRoleAuthorities.admin()), names(SeededRoleAuthorities.role("ADMIN")));
    }

    @Test
    public void unknownRoleFailsLoudly() {
        try {
            SeededRoleAuthorities.role("RESULT"); // typo
            fail("a typo must not yield a silent privilege-less user");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Results"));
        }
    }
}
