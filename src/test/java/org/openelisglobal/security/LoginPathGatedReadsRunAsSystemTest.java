package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Authority loading cannot itself require an authority.
 *
 * <p>
 * {@code CustomUserDetailsService.getGrantedAuthorities} runs DURING
 * authentication, before any {@code Authentication} is in the SecurityContext,
 * and {@code /session} answers for a caller whose own roles are the subject of
 * the question. Both reach {@code RoleModuleService.getPermittedModuleNames} to
 * derive develop's qa.* permission keys — and this branch puts a type-level
 * {@code @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")} on that interface,
 * which develop does not have.
 *
 * <p>
 * Ungated, that call is the circular case: you would need authorities to load
 * your authorities. It threw {@code AuthenticationCredentialsNotFoundException}
 * out of {@code DaoAuthenticationProvider.retrieveUser} and EVERY login failed,
 * for every user including admin, with the UI showing only "Unsuccessful login
 * attempt" (dev stack, 2026-09-30). On {@code /session} the same call denies
 * for any role lacking role:view — which is every bench role — so the endpoint
 * that tells the frontend what the user may see would fail for exactly the
 * users the sidebar filter is for.
 *
 * <p>
 * The fix is the self-identity pattern {@code UserContextHolder} already uses:
 * run the read inside {@code SystemInitFlag.enter()/exit()}. The caller's own
 * access is established by the login itself, so the read must not be gated
 * against a principal that does not exist yet.
 *
 * <p>
 * Asserted by source scan: reproducing it needs the full Spring context, a
 * database and a real authentication round-trip, and this catches the
 * regression at the cheap layer. A new gated call added to either method
 * outside the wrapper fails here.
 */
public class LoginPathGatedReadsRunAsSystemTest {

    /** {file, enclosing method signature} whose gated reads must run as system. */
    private static final String[][] LOGIN_PATH_METHODS = {
            { "src/main/java/org/openelisglobal/security/login/CustomUserDetailsService.java",
                    "private List<GrantedAuthority> getGrantedAuthorities(LoginUser user)" },
            { "src/main/java/org/openelisglobal/login/controller/LoginPageController.java",
                    "public UserSession getSesssionDetails(HttpServletRequest request, CsrfToken token)" } };

    /**
     * Services whose interface carries a type-level or per-method gate AND is
     * called on the login path. RoleModuleService is the one that broke; the others
     * are listed so that wiring any of them in later is caught too.
     */
    private static final String[] GATED_CALLS = { "roleModuleService.getPermittedModuleNames(",
            "roleModuleService.getAllPermittedPagesFromAgentId(", "userRoleService.getUserRolesForUser(",
            "roleService.getAllRoles(" };

    /**
     * Deliberately NOT listed above: {@code PrivilegeService} is annotated
     * {@code @CrossDomainService} naming this very caller, so its reads are ungated
     * by design and need no wrapper. Listing it here would demand a wrapper the
     * design says is unnecessary.
     */
    private static final String UNGATED_BY_DESIGN = "privilegeService";

    private static String bodyOf(String src, String signature) {
        int start = src.indexOf(signature);
        assertTrue("method not found, the login path moved: " + signature, start >= 0);
        int brace = src.indexOf('{', start);
        int depth = 0;
        for (int i = brace; i < src.length(); i++) {
            char c = src.charAt(i);
            depth += c == '{' ? 1 : c == '}' ? -1 : 0;
            if (depth == 0) {
                return src.substring(brace, i);
            }
        }
        throw new AssertionError("unbalanced braces in " + signature);
    }

    /**
     * The character range of every {@code SystemInitFlag.enter()} try-block in the
     * body, so a call can be checked for being inside one.
     */
    private static List<int[]> systemContextRanges(String body) {
        List<int[]> ranges = new ArrayList<>();
        int from = 0;
        while (true) {
            int enter = body.indexOf("SystemInitFlag.enter()", from);
            if (enter < 0) {
                break;
            }
            int exit = body.indexOf("SystemInitFlag.exit(", enter);
            if (exit < 0) {
                break;
            }
            ranges.add(new int[] { enter, exit });
            from = exit + 1;
        }
        // SystemContext.runAsSystem / callAsSystem wrap a lambda instead.
        from = 0;
        while (true) {
            int call = body.indexOf("SystemContext.", from);
            if (call < 0) {
                break;
            }
            int close = body.indexOf("});", call);
            ranges.add(new int[] { call, close < 0 ? body.length() : close });
            from = call + 1;
        }
        return ranges;
    }

    @Test
    public void gatedReadsOnTheLoginPathRunInSystemContext() throws IOException {
        List<String> unwrapped = new ArrayList<>();
        for (String[] target : LOGIN_PATH_METHODS) {
            String src = Files.readString(Paths.get(target[0]));
            String body = bodyOf(src, target[1]);
            List<int[]> safe = systemContextRanges(body);
            for (String call : GATED_CALLS) {
                int at = body.indexOf(call);
                while (at >= 0) {
                    final int pos = at;
                    if (safe.stream().noneMatch(r -> pos > r[0] && pos < r[1])) {
                        unwrapped.add(target[0].substring(target[0].lastIndexOf('/') + 1) + ": " + call
                                + " is not inside SystemInitFlag/SystemContext");
                    }
                    at = body.indexOf(call, at + 1);
                }
            }
        }
        assertTrue("A gated service read on the authentication path must run as the system actor; unauthenticated"
                + " there, it denies and takes every login (or /session) with it:\n" + String.join("\n", unwrapped),
                unwrapped.isEmpty());
    }

    /**
     * Inversion: the scan must actually find the calls it is guarding. If the login
     * path stopped calling them, the assertion above would pass vacuously.
     */
    @Test
    public void theScanFindsTheCallsItGuards() throws IOException {
        int found = 0;
        for (String[] target : LOGIN_PATH_METHODS) {
            String body = bodyOf(Files.readString(Paths.get(target[0])), target[1]);
            for (String call : GATED_CALLS) {
                int at = body.indexOf(call);
                while (at >= 0) {
                    found++;
                    at = body.indexOf(call, at + 1);
                }
            }
        }
        assertTrue("the scan found no gated calls at all — the login path moved and this test is now vacuous",
                found >= 2);
        assertTrue("PrivilegeService must stay @CrossDomainService; if it gains a gate it belongs in GATED_CALLS",
                Files.readString(Paths.get("src/main/java/org/openelisglobal/privilege/service/PrivilegeService.java"))
                        .contains("@CrossDomainService") && UNGATED_BY_DESIGN.equals("privilegeService"));
    }
}
