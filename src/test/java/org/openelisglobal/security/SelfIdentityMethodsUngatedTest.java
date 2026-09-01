package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Guards the failure mode that took down {@code GET /rest/menu} for every
 * non-admin: a <em>self-identity</em> service method — one whose argument is
 * the CALLER'S OWN user id — gated on a privilege that no seeded base role
 * actually holds.
 *
 * <p>
 * The bug is invisible to {@link ServicePrivilegeCoverageTest}, which only asks
 * whether an annotation is <em>present</em>, never whether any real role can
 * satisfy it. {@code userInRole} was gated on {@code PRIV_USER_ROLE_VIEW} while
 * 012-004 grants {@code user_role:view} to no base role, so
 * {@code MenuController.isGlobalScopeUser} — the first statement on the
 * endpoint the UI hits on every page load — threw {@code AccessDeniedException}
 * for Reception/Results/Validation/Reports users and 403'd the navigation tree.
 * The same call shape sits in OrderSearchRestController,
 * PatientDashBoardProvider, StorageLocationRestController and
 * SampleEdit{,Rest}Controller, and {@code /rest/users/Pathologist} feeds the
 * Pathology, Cytology and IHC case views.
 *
 * <p>
 * Two invariants are asserted:
 *
 * <ol>
 * <li>the known self-identity methods carry no {@code @PreAuthorize} at all;
 * </li>
 * <li>any privilege named by a {@code @PreAuthorize} on those interfaces is
 * granted to at least one base role in the Liquibase seed — otherwise only a
 * Global Administrator could ever call it.</li>
 * </ol>
 */
public class SelfIdentityMethodsUngatedTest {

    private static final Path SEED = Paths
            .get("src/main/resources/liquibase/3.5.x.x/012-004-seed-role-privilege-mappings.xml");

    /**
     * Methods whose {@code userId}/{@code roleId} argument is the caller's own id
     * in production. Asking "what are my roles / am I an admin" must never require
     * a privilege, because authorities are still being built (authentication,
     * /session) or the answer only scopes the caller's own view.
     */
    private static final String[][] SELF_IDENTITY_METHODS = { { UserRoleService.class.getName(), "getRoleIdsForUser" },
            { UserRoleService.class.getName(), "userInRole" }, { RoleService.class.getName(), "getRoleById" },
            { RoleService.class.getName(), "getRoleByName" } };

    @Test
    public void selfIdentityMethodsCarryNoPreAuthorize() {
        List<String> violations = new ArrayList<>();

        for (String[] target : SELF_IDENTITY_METHODS) {
            Class<?> iface;
            try {
                iface = Class.forName(target[0]);
            } catch (ClassNotFoundException e) {
                violations.add(target[0] + " not found on classpath");
                continue;
            }
            for (Method m : iface.getMethods()) {
                if (!m.getName().equals(target[1])) {
                    continue;
                }
                PreAuthorize pre = m.getAnnotation(PreAuthorize.class);
                if (pre != null) {
                    violations.add(iface.getSimpleName() + "#" + m.getName() + " is gated on \"" + pre.value()
                            + "\" but its argument is the caller's own id — a self-identity read"
                            + " must stay ungated (see class javadoc)");
                }
            }
        }

        assertTrue("Self-identity service methods must not be privilege-gated: " + violations, violations.isEmpty());
    }

    /**
     * Methods that non-admin lab staff reach through ordinary screens, paired with
     * the screen that proves it. An admin-only privilege here means that screen
     * 403s for everyone but a Global Administrator — which is what happened to
     * {@code /rest/menu}. Methods that are genuinely administrative
     * ({@code role:manage}, {@code deleteLabUnitRoleMap}, …) are deliberately
     * absent: Global-Admin-only is the correct answer for those.
     */
    private static final String[][] NON_ADMIN_REACHABLE = { { UserRoleService.class.getName(), "getUserLabUnitRoles",
            "MenuController/OrderSearchRestController/PatientDashBoardProvider #isGlobalScopeUser"
                    + " and LoginPageController /session" } };

    @Test
    public void privilegesOnNonAdminReachableMethodsAreHeldByAtLeastOneBaseRole() throws IOException {
        Set<String> seeded = seededPrivilegeNames();
        // Guard against a silent no-op if the seed file is ever moved/renamed.
        assertTrue("Could not parse any role->privilege mappings from " + SEED + " — test would silently pass",
                seeded.size() > 10);

        List<String> violations = new ArrayList<>();
        for (String[] target : NON_ADMIN_REACHABLE) {
            Class<?> iface;
            try {
                iface = Class.forName(target[0]);
            } catch (ClassNotFoundException e) {
                violations.add(target[0] + " not found on classpath");
                continue;
            }
            for (Method m : iface.getMethods()) {
                if (!m.getName().equals(target[1])) {
                    continue;
                }
                PreAuthorize pre = m.getAnnotation(PreAuthorize.class);
                if (pre == null) {
                    continue;
                }
                for (String priv : privilegeNamesIn(pre.value())) {
                    if (!seeded.contains(priv)) {
                        violations.add(iface.getSimpleName() + "#" + m.getName() + " requires '" + priv
                                + "', which 012-004 grants to NO base role — so " + target[2]
                                + " denies for every non-admin");
                    }
                }
            }
        }

        assertTrue("Privileges on non-admin-reachable methods must be held by a real base role: " + violations,
                violations.isEmpty());
    }

    /** Privilege names granted to at least one role by the 012-004 seed. */
    private Set<String> seededPrivilegeNames() throws IOException {
        String xml = new String(Files.readAllBytes(SEED));
        Set<String> names = new HashSet<>();
        Matcher m = Pattern.compile("AND p\\.name = '([^']+)'").matcher(xml);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    /**
     * Extracts {@code order:view} from {@code hasAuthority('PRIV_ORDER_VIEW')} —
     * the seed stores {@code domain:action}, the annotation the PRIV_ authority.
     */
    private Set<String> privilegeNamesIn(String expression) {
        Set<String> privs = new HashSet<>();
        Matcher m = Pattern.compile("'PRIV_([A-Z0-9_]+)'").matcher(expression);
        while (m.find()) {
            String tail = m.group(1).toLowerCase();
            int last = tail.lastIndexOf('_');
            privs.add(last < 0 ? tail : tail.substring(0, last) + ":" + tail.substring(last + 1));
        }
        return privs;
    }
}
