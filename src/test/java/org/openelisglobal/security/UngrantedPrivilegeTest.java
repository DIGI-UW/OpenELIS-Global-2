package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * A privilege that gates production code but is granted to no role denies every
 * user except Global Administrator.
 *
 * <p>
 * Admin is not a counterexample: its {@code "*"} sentinel expands to whatever
 * rows exist in {@code system_privilege}, so an ungranted privilege still
 * "works" for admin and the gap survives every admin-driven test. It surfaces
 * only when a real role reaches the gate.
 *
 * <p>
 * {@code site_info:view} was exactly this. It gates
 * {@code ImageService.getImageBySiteInfoName}, which
 * {@code Report.createReportParameters} calls to put the lab logo into the
 * header of all 18 report types, and 012-009 renamed it (from
 * {@code siteinfo:view}) so the {@code PRIV_SITE_INFO_VIEW} authority would
 * match, without ever granting it. The result: no non-admin role could generate
 * any report, and the failure came after the form was filled in, as a bare 403.
 *
 * <p>
 * This is a ratchet, not a clean sheet. {@link #BASELINE} is the set that was
 * ungranted when this was written; it may shrink but must never grow. Adding a
 * privilege here means shipping a gate nobody can pass.
 */
public class UngrantedPrivilegeTest {

    private static final Path SEED_DIR = Paths.get("src/main/resources/liquibase/3.5.x.x");

    private static final Pattern PRIV_NAME = Pattern
            .compile("<column\\s+name=\"name\"\\s+value=\"([a-z_]+:[a-z_-]+)\"");

    private static final Pattern PRIV_IN_SQL = Pattern.compile("p\\.name\\s*(?:=\\s*'([^']+)'|IN\\s*\\(([^)]*)\\))");

    private static final Pattern QUOTED = Pattern.compile("'([^']+)'");

    /**
     * Privileges seeded but granted to no role as of this writing. Most are
     * administrative by design ({@code :manage}, {@code system:configure}): only
     * Global Administrator is meant to hold them, and its sentinel covers them.
     *
     * <p>
     * The {@code :view} entries are less obviously safe. Several of them
     * ({@code panel:view}, {@code dictionary:view}, {@code program:view},
     * {@code analyte:view}, {@code method:view}) gate catalogue reads that
     * {@code catalogue:view} now also accepts, which is why no role needs them
     * directly. The rest gate screens no seeded role currently reaches. If a
     * walkthrough finds a role that DOES need one, grant it and remove it here; the
     * list may shrink but must never grow.
     */
    private static final Set<String> BASELINE = new TreeSet<>(List.of("alert:manage", "barcode:manage", "barcode:view",
            "branding:manage", "calendar:manage", "calendar:view", "coldstorage:manage", "dictionary:manage",
            "dictionary:view", "extconnection:manage", "extconnection:view", "inventory:manage", "localization:manage",
            "localization:view", "method:view", "notebook:manage", "notebook:view", "notification:manage",
            "notification:view", "organization:manage", "panel:manage", "panel:view", "program:manage", "program:view",
            "provider:manage", "referral:manage", "report:configure", "sample_type:manage", "shipment:edit",
            "storage:manage", "system:configure", "system_user:manage", "system_user:view", "test:configure",
            "testcalc:view", "user_role:manage"));

    /**
     * BASELINE is an exemption list, so an entry that later gets granted goes stale
     * silently: the ratchet keeps passing while the list overstates how much is
     * ungranted. Failing here forces it to shrink when it should.
     */
    @Test
    public void baselineDoesNotListPrivilegesThatAreNowGranted() throws IOException {
        Set<String> granted = grantedPrivileges();
        List<String> stale = BASELINE.stream().filter(granted::contains).sorted().collect(Collectors.toList());
        assertEquals("These are granted now, so they must be removed from BASELINE: " + stale, List.of(), stale);
    }

    @Test
    public void everySeededPrivilegeIsGrantedToSomeRole() throws IOException {
        Set<String> seeded = seededPrivileges();
        Set<String> granted = grantedPrivileges();

        List<String> ungranted = seeded.stream().filter(p -> !granted.contains(p)).filter(p -> !BASELINE.contains(p))
                .sorted().collect(Collectors.toList());

        assertEquals(
                "These privileges are seeded but granted to no role, so they deny everyone except Global"
                        + " Administrator (whose \"*\" sentinel hides the gap). If one gates a shared path the way"
                        + " site_info:view gated every report header, no role can use that feature: " + ungranted,
                List.of(), ungranted);
    }

    /**
     * Inversion, both halves: the two scans must actually find something, or the
     * assertion above passes by comparing one empty set against another and every
     * future ungranted privilege goes unnoticed.
     */
    @Test
    public void bothScansFindRealData() throws IOException {
        Set<String> seeded = seededPrivileges();
        Set<String> granted = grantedPrivileges();

        assertTrue("the seed scan found almost nothing: " + seeded.size(), seeded.size() > 50);
        assertTrue("the grant scan found almost nothing: " + granted.size(), granted.size() > 30);

        // Spot-check one of each, so a regex that matches the wrong thing is caught.
        assertTrue("order:create is a seeded privilege", seeded.contains("order:create"));
        assertTrue("order:create is granted (to Reception)", granted.contains("order:create"));

        // The privilege this test was written for must now be granted.
        assertTrue("site_info:view is seeded", seeded.contains("site_info:view"));
        assertTrue("site_info:view must be granted (012-004n)", granted.contains("site_info:view"));

        assertFalse("a privilege that does not exist must not be reported as granted",
                granted.contains("not:a-real-privilege"));
    }

    private Set<String> seededPrivileges() throws IOException {
        Set<String> names = new TreeSet<>();
        for (Path f : seedFiles()) {
            String src = Files.readString(f);
            Matcher m = PRIV_NAME.matcher(src);
            while (m.find()) {
                names.add(m.group(1));
            }
            // 012-003a-style inserts: SELECT 'micro:view', '...', 'microbiology'
            Matcher sql = Pattern
                    .compile("INSERT INTO clinlims\\.system_privilege[^;]*?SELECT\\s+'([^']+)'", Pattern.DOTALL)
                    .matcher(src);
            while (sql.find()) {
                names.add(sql.group(1));
            }
        }
        // 012-009 renames siteinfo:view to site_info:view; the old name is gone.
        names.remove("siteinfo:view");
        return names;
    }

    private Set<String> grantedPrivileges() throws IOException {
        Set<String> names = new TreeSet<>();
        for (Path f : seedFiles()) {
            for (String stmt : Files.readString(f).split(";")) {
                if (!stmt.contains("system_role_privilege") || !stmt.toUpperCase().contains("INSERT")) {
                    continue;
                }
                Matcher m = PRIV_IN_SQL.matcher(stmt);
                while (m.find()) {
                    if (m.group(1) != null) {
                        names.add(m.group(1));
                    } else {
                        Matcher q = QUOTED.matcher(m.group(2));
                        while (q.find()) {
                            names.add(q.group(1));
                        }
                    }
                }
                // 012-009-style: privilege named in a subselect rather than a join term.
                Matcher sub = Pattern.compile("system_privilege WHERE name = '([^']+)'").matcher(stmt);
                while (sub.find()) {
                    names.add(sub.group(1));
                }
            }
        }
        return names;
    }

    private List<Path> seedFiles() throws IOException {
        try (Stream<Path> files = Files.list(SEED_DIR)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .filter(p -> p.getFileName().toString().startsWith("012-")).sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }
}
