package org.openelisglobal.security;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The EQA module belongs to the EQA Coordinator, and to nobody else.
 *
 * <p>
 * Scope note after the develop merge: EQA V2 deleted EQADistributionService and
 * moved that surface under cycles and panels, so the distribution assertions are
 * gone. EQA is now gated twice - EQAGuards on the controllers (qa.* model) and
 * @CrudPrivileges on the services (this branch) - and what remains here pins the
 * service half.
 *
 * <p>
 * Every EQA service declared its finders with eqa:view / eqa:manage but
 * declared no {@code @CrudPrivileges} and no type-level gate, so
 * {@link org.openelisglobal.common.security.CrudGate} fell through to its open
 * branch and the INHERITED CRUD was callable by any authenticated user. The EQA
 * REST controllers carry no gates of their own and call that inherited CRUD
 * directly, so the hole was reachable: verified live, the Results role (holding
 * no eqa:* privilege) read the full programme and distribution lists through
 * {@code listPrograms}/{@code listDistributions}, and {@code createProgram}
 * committed a new programme through {@code insert} before the handler's next
 * gated call failed and turned the response into a misleading 400.
 *
 * <p>
 * The declared finders were never the problem; they denied correctly
 * throughout. That is why this went unnoticed: the endpoints that looked gated
 * were.
 */
public class EqaModuleAccessTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EQAProgramService eqaProgramService;


    /**
     * Not named authenticateAs: the base class has a method by that name granting
     * fullTestAuthorities(), which would satisfy every assertion here regardless of
     * the seed.
     */
    private void authenticateWithSeededRole(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(role.toLowerCase(), "N/A", SeededRoleAuthorities.role(role)));
    }

    private void authenticateWithNoPrivileges() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", "N/A", Collections.emptyList()));
    }

    @Test
    public void coordinatorReadsProgrammes() {
        authenticateWithSeededRole("EQA Coordinator");
        assertNotNull(eqaProgramService.getAll());
    }

    /** The read hole: getAll is inherited CRUD, not a declared finder. */
    @Test(expected = AccessDeniedException.class)
    public void programmeListIsRefusedWithoutEqaView() {
        authenticateWithNoPrivileges();
        eqaProgramService.getAll();
    }


    /**
     * The write hole, and the one that actually committed: a role with no eqa:*
     * privilege created a programme through the inherited insert.
     */
    @Test(expected = AccessDeniedException.class)
    public void programmeCreationIsRefusedWithoutEqaManage() {
        authenticateWithNoPrivileges();
        eqaProgramService.insert(new EQAProgram());
    }

    /**
     * Results is the concrete role the live probe used, and the one the EQA routes
     * used to admit. It holds result:enter and no eqa:*, so it must be refused.
     */
    @Test(expected = AccessDeniedException.class)
    public void resultsRoleCannotCreateAProgramme() {
        authenticateWithSeededRole("Results");
        eqaProgramService.insert(new EQAProgram());
    }

    @Test(expected = AccessDeniedException.class)
    public void resultsRoleCannotListProgrammes() {
        authenticateWithSeededRole("Results");
        eqaProgramService.getAll();
    }

    /**
     * Inversion on the write side: eqa:view alone must not be enough to write, or
     * the read/write split in {@code @CrudPrivileges} would be decorative. The
     * coordinator holds both, so this uses a bare eqa:view authority.
     */
    @Test(expected = AccessDeniedException.class)
    public void eqaViewAloneCannotCreateAProgramme() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "N/A",
                List.of(new SimpleGrantedAuthority("PRIV_EQA_VIEW"))));
        eqaProgramService.insert(new EQAProgram());
    }
}
