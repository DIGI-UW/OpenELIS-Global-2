package org.openelisglobal.security;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The role named after the audit trail must be able to read it.
 *
 * <p>
 * Audit Trail held audit:view and user_role:view and nothing else, so
 * {@code GET /rest/systemAuditEvents} returned 403 and the role could not open
 * the one screen it exists for. The event list resolves each event's actor to a
 * name through {@code SystemUserService.getUserById}, gated on
 * system_user:view, which was granted to NO role: the same shape as
 * site_info:view before 012-004n, and invisible to admin-driven testing because
 * the {@code "*"} sentinel covers it. 012-004s grants the four reads the list
 * needs.
 *
 * <p>
 * The accession-level AuditTrailReport is deliberately out of scope: it
 * rebuilds the whole sample view and needs about six further grants plus a
 * widened ReportTrackingService gate, so it stays admin-only rather than
 * turning an auditor into a reader of most of the sample domain.
 */
public class AuditLogAccessTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemUserService systemUserService;

    /**
     * Not named authenticateAs: the base class has a method by that name granting
     * fullTestAuthorities(), which would make every assertion here pass regardless
     * of the seed.
     */
    private void authenticateWithSeededRole(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(role.toLowerCase(), "N/A", SeededRoleAuthorities.role(role)));
    }

    /**
     * The gate that actually blocked the event log. Resolving the actor behind each
     * event is what the list does for every row.
     */
    @Test
    public void auditRoleCanResolveTheActorBehindAnEvent() {
        authenticateWithSeededRole("Audit Trail");
        // getUserById, not getAll: SystemUserService declares no @CrudPrivileges
        // and no type-level gate, so its INHERITED getAll is open to any
        // authenticated user (pre-existing, on the InheritedCrudGateCoverageTest
        // baseline). Asserting on getAll would pass without system_user:view and
        // prove nothing. getUserById carries the real gate, and is the call the
        // audit list makes per row.
        systemUserService.getUserById("1");
    }

    /** Inversion: the gate is real, so the test above is not vacuous. */
    @Test(expected = AccessDeniedException.class)
    public void actorLookupIsRefusedWithoutSystemUserView() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", "N/A", Collections.emptyList()));
        systemUserService.getUserById("1");
    }

    /**
     * Inversion on scope: reading the audit log does not make the role an
     * administrator. Reception holds none of the audit grants and must stay out.
     */
    @Test(expected = AccessDeniedException.class)
    public void receptionCannotResolveSystemUsers() {
        authenticateWithSeededRole("Reception");
        systemUserService.getUserById("1");
    }
}
