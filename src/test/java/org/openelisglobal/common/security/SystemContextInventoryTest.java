package org.openelisglobal.common.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * {@link SystemContext} bypasses service-layer authorization, so every call
 * site is a place where a privilege check does not happen. This pins the
 * inventory so a new one cannot be added without a reviewer seeing it in the
 * diff.
 *
 * <p>
 * The inventory started at 44 sites, almost all of them working around the same
 * problem: an order-entry role could not read the catalogue it orders from,
 * because the catalogue's read methods were gated on administrative privileges
 * ({@code result:view}, {@code test:configure}, {@code sample_type:view}). That
 * is an authorization bug, and wrapping it in system context hid it rather than
 * fixing it. {@code PRIV_CATALOGUE_VIEW} fixes it: the reads are gated normally
 * and the grant is visible in Role Management.
 *
 * <p>
 * What legitimately remains are <b>system actions</b>, writes the application
 * performs as a consequence of an order the caller was permitted to place,
 * using privileges the caller deliberately does not hold (routing onto the
 * microbiology bench, typing a referring organization, recording label
 * quantities, notifying results staff), plus two read-only assemblies that
 * genuinely cross non-catalogue domains ({@code result:view},
 * {@code storage:view}, {@code referral:view}, {@code micro:view}).
 *
 * <p>
 * If this test fails because you added a call site: check first whether the
 * read you are bypassing is catalogue data. If it is, widen its gate to accept
 * {@code PRIV_CATALOGUE_VIEW} instead and leave the count alone.
 */
public class SystemContextInventoryTest {

    private static final Path MAIN_JAVA = Paths.get("src/main/java");

    private static final Pattern CALL_SITE = Pattern
            .compile("SystemContext\\s*\\.?\\s*(callAsSystem|runAsSystem)\\s*\\(");

    /**
     * The lower-level door to the same bypass. {@code SystemContext} is a thin
     * wrapper over {@code SystemInitFlag.enter()/exit()}, so a call site that uses
     * the flag directly skips exactly the same privilege checks while being
     * invisible to {@link #CALL_SITE}. Counting only the wrapper pinned half the
     * surface: eleven direct users already existed when this was added.
     */
    private static final Pattern RAW_FLAG_SITE = Pattern.compile("SystemInitFlag\\s*\\.\\s*enter\\s*\\(");

    /**
     * The files permitted to bypass authorization, with the number of sites in
     * each. Raising a number, or adding a key, means adding an unreviewed
     * authorization bypass, justify it in the PR.
     */
    private static final Map<String, Integer> ALLOWED = Map.of(
            // Routing onto the microbiology bench, typing a referring organization,
            // recording label quantities, writing compliance links, and reading the
            // order's own analyses to avoid a duplicate insert (result:view).
            "org/openelisglobal/sample/service/SamplePatientEntryServiceImpl.java", 9,
            // Notifying results staff that a STAT order needs picking up.
            "org/openelisglobal/sample/controller/rest/SamplePatientEntryRestController.java", 1,
            // One order's full detail, crossing result:view, storage:view,
            // referral:view, nce:view and micro:view, none of them catalogue.
            "org/openelisglobal/sample/controller/rest/OrderSearchRestController.java", 1,
            // Landing-page workload counts, from result:view-gated analysis counts.
            "org/openelisglobal/common/rest/provider/PatientDashBoardProvider.java", 1);

    private static final int EXPECTED_TOTAL = 12;

    /**
     * Direct {@code SystemInitFlag.enter()} users, with the number of sites in
     * each. Infrastructure that runs before or outside a user's authorities
     * (startup configuration loading, the login success handler, the async task
     * decorator, reference-data caches) plus three self-identity primitives that
     * resolve the caller's own SystemUser row. Same rule as ALLOWED above: raising
     * a number or adding a key adds an unreviewed bypass.
     */
    private static final Map<String, Integer> ALLOWED_RAW_FLAG = Map.ofEntries(
            Map.entry("org/openelisglobal/common/security/SystemContext.java", 2),
            Map.entry("org/openelisglobal/common/security/SystemContextTaskDecorator.java", 2),
            Map.entry("org/openelisglobal/common/services/DisplayListService.java", 1),
            Map.entry("org/openelisglobal/common/util/ConfigurationListenerServiceImpl.java", 1),
            // Self-identity: the current principal's own SystemUser, needed on every
            // audited write and while authorities are still being built.
            Map.entry("org/openelisglobal/common/util/UserContextHolder.java", 1),
            Map.entry("org/openelisglobal/configuration/service/ConfigurationInitializationService.java", 1),
            // Self-identity: the signer is always the authenticated caller.
            Map.entry("org/openelisglobal/esig/service/ElectronicSignatureServiceImpl.java", 1),
            Map.entry("org/openelisglobal/login/controller/LoginPageController.java", 1),
            Map.entry("org/openelisglobal/security/login/CustomFormAuthenticationSuccessHandler.java", 1),
            Map.entry("org/openelisglobal/systemuser/service/UserServiceImpl.java", 1),
            Map.entry("org/openelisglobal/result/action/util/ResultsLoadUtility.java", 1),
            Map.entry("org/openelisglobal/dataexchange/fhir/service/FhirTransformServiceImpl.java", 1));

    @Test
    public void systemContextCallSitesMatchTheReviewedInventory() throws IOException {
        Map<String, Integer> actual = scanCallSites();

        assertEquals(
                "SystemContext call sites changed. Each one skips a privilege check;"
                        + " see this test's javadoc before updating the expected inventory.",
                new TreeMap<>(ALLOWED), new TreeMap<>(actual));

        assertEquals("Total bypass count", EXPECTED_TOTAL, actual.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void rawSystemInitFlagUsesMatchTheReviewedInventory() throws IOException {
        Map<String, Integer> actual = scan(RAW_FLAG_SITE);
        // SystemInitFlag declares enter() itself; that is the definition, not a use.
        actual.remove("org/openelisglobal/common/security/SystemInitFlag.java");

        assertEquals(
                "Direct SystemInitFlag.enter() sites changed. This is the same authorization bypass as"
                        + " SystemContext, one layer down, and is not counted by the scan above.",
                new TreeMap<>(ALLOWED_RAW_FLAG), new TreeMap<>(actual));
    }

    /**
     * Inversion test: the scanner must actually find call sites. A regex that
     * silently matches nothing would make the assertion above pass for the wrong
     * reason, an empty map equal to an empty map, and every future bypass would go
     * unnoticed.
     */
    @Test
    public void scannerDetectsCallSites() {
        assertTrue("callAsSystem must be detected",
                CALL_SITE.matcher("return SystemContext.callAsSystem(() -> foo());").find());
        assertTrue("runAsSystem must be detected", CALL_SITE.matcher("SystemContext.runAsSystem(bar::baz);").find());
        assertTrue("a line-wrapped call must be detected",
                CALL_SITE.matcher("List<X> x = SystemContext\n        .callAsSystem(() -> y());").find());
        assertFalse("an unrelated mention must not be detected",
                CALL_SITE.matcher("import org.openelisglobal.common.security.SystemContext;").find());

        assertTrue("a direct flag use must be detected",
                RAW_FLAG_SITE.matcher("boolean wasSet = SystemInitFlag.enter();").find());
        assertFalse("exit() is the restore half, not a new bypass",
                RAW_FLAG_SITE.matcher("SystemInitFlag.exit(wasSet);").find());
        assertFalse("the wrapper pattern must not match a raw flag use",
                CALL_SITE.matcher("boolean wasSet = SystemInitFlag.enter();").find());
    }

    /**
     * The catalogue privilege the refactor introduced must exist as a constant and
     * be in real use. Without it, the only way to make a catalogue read reachable
     * is another bypass, and the inventory above starts growing again.
     *
     * <p>
     * Note the two spellings: {@code Privileges.CATALOGUE_VIEW} holds the seeded
     * privilege NAME ({@code catalogue:view}), while a {@code @PreAuthorize}
     * expression names the AUTHORITY, which Spring Security sees with the
     * {@code PRIV_} prefix the authority mapper adds. Asserting on the wrong one
     * passes vacuously.
     */
    @Test
    public void catalogueViewPrivilegeGatesRealReads() throws IOException {
        assertTrue("Privileges must declare the catalogue:view privilege name",
                Files.readString(MAIN_JAVA.resolve("org/openelisglobal/common/constants/Privileges.java"))
                        .contains("\"catalogue:view\""));

        Set<String> gatingFiles;
        try (Stream<Path> sources = javaSources()) {
            gatingFiles = sources.filter(p -> readSafely(p).contains("PRIV_CATALOGUE_VIEW")).map(this::relativePath)
                    .collect(Collectors.toSet());
        }

        // Well above the handful a typo or a stray reference would produce, and far
        // below a blanket find-and-replace: these are the catalogue services whose
        // read methods the order-entry roles need.
        assertTrue("PRIV_CATALOGUE_VIEW should gate many catalogue services, found " + gatingFiles.size(),
                gatingFiles.size() >= 12);
    }

    private Map<String, Integer> scanCallSites() throws IOException {
        Map<String, Integer> counts = scan(CALL_SITE);
        // SystemContext declares callAsSystem/runAsSystem; that is the definition,
        // not a call site. Scoped to this scan: for the raw-flag scan below, the
        // same file's two SystemInitFlag.enter() uses ARE the bypass it wraps.
        counts.remove("org/openelisglobal/common/security/SystemContext.java");
        return counts;
    }

    private Map<String, Integer> scan(Pattern pattern) throws IOException {
        Map<String, Integer> counts = new TreeMap<>();
        try (Stream<Path> sources = javaSources()) {
            List<Path> files = sources.collect(Collectors.toList());
            for (Path file : files) {
                Matcher matcher = pattern.matcher(readSafely(file));
                int count = 0;
                while (matcher.find()) {
                    count++;
                }
                if (count > 0) {
                    counts.put(relativePath(file), count);
                }
            }
        }
        return counts;
    }

    /**
     * Slash-separated regardless of platform, so the literals above are portable.
     */
    private String relativePath(Path file) {
        StringBuilder path = new StringBuilder();
        for (Path segment : MAIN_JAVA.relativize(file)) {
            if (path.length() > 0) {
                path.append('/');
            }
            path.append(segment);
        }
        return path.toString();
    }

    private Stream<Path> javaSources() throws IOException {
        return Files.walk(MAIN_JAVA).filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java"));
    }

    private String readSafely(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
