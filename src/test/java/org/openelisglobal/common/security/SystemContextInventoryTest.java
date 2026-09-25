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

    @Test
    public void systemContextCallSitesMatchTheReviewedInventory() throws IOException {
        Map<String, Integer> actual = scanCallSites();

        assertEquals(
                "SystemContext call sites changed. Each one skips a privilege check;"
                        + " see this test's javadoc before updating the expected inventory.",
                new TreeMap<>(ALLOWED), new TreeMap<>(actual));

        assertEquals("Total bypass count", EXPECTED_TOTAL, actual.values().stream().mapToInt(Integer::intValue).sum());
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
        Map<String, Integer> counts = new TreeMap<>();
        try (Stream<Path> sources = javaSources()) {
            List<Path> files = sources.collect(Collectors.toList());
            for (Path file : files) {
                Matcher matcher = CALL_SITE.matcher(readSafely(file));
                int count = 0;
                while (matcher.find()) {
                    count++;
                }
                if (count > 0) {
                    counts.put(relativePath(file), count);
                }
            }
        }
        // SystemContext itself declares the methods; it is not a call site.
        counts.remove("org/openelisglobal/common/security/SystemContext.java");
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
