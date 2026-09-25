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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * {@code PRIV_CATALOGUE_VIEW} must stay read-only.
 *
 * <p>
 * It is granted broadly, to Reception, Results, Validation, Reports,
 * Pathologist, Cytopathologist, EQA Coordinator and Analyser Import, because
 * every role that works an order has to see the catalogue it orders from. That
 * breadth is safe only while the privilege confers no write.
 *
 * <p>
 * The trap is the <b>type-level</b> {@code @PreAuthorize}. Widening one to
 * {@code hasAnyAuthority('PRIV_X_VIEW','PRIV_CATALOGUE_VIEW')} widens every
 * method on that interface that has no gate of its own, including writes. Two
 * were caught this way when the privilege was introduced:
 * {@code TypeOfSampleService.moveToSortOrderPosition} and
 * {@code TypeOfSampleTestService.updateDisplayOrder}, both of which reorder the
 * catalogue and would have become reachable by a receptionist. Both are now
 * pinned to {@code PRIV_SAMPLE_TYPE_MANAGE}.
 *
 * <p>
 * Inherited CRUD is not in scope here: {@code BaseObjectService} gates
 * {@code insert/update/delete} through {@code CrudGate.write}, which resolves
 * {@code @CrudPrivileges(write=…)} and never falls back to the read gate.
 * {@link InheritedCrudGateCoverageTest} covers that side.
 */
public class CatalogueViewReadOnlyTest {

    private static final Path MAIN_JAVA = Paths.get("src/main/java");

    /**
     * A method declaration in an interface body: no braces, ends in a semicolon.
     */
    private static final Pattern DECLARATION = Pattern
            .compile("^[A-Za-z][A-Za-z0-9_<>,.\\[\\]\\s]*?\\b(\\w+)\\s*\\([^;{}]*\\)\\s*;$");

    /**
     * Verbs that mutate. Deliberately generous: a false positive is a comment
     * explaining why a method named like a write is a read, which is worth having
     * anyway. A false negative is an unnoticed privilege escalation.
     */
    private static final Pattern MUTATING_NAME = Pattern
            .compile("^(save|insert|update|delete|create|remove|add|set|move|reorder|assign|revoke|clear|purge|archive"
                    + "|activate|deactivate|link|unlink|merge|copy|import|persist|replace|apply|renumber|reset)"
                    + "([A-Z].*)?$");

    /**
     * Reads whose names begin with a mutating verb. Each is a genuine read; listed
     * here rather than renamed, because renaming a published service method is a
     * larger change than this refactor warrants.
     */
    private static final List<String> READS_WITH_MUTATING_NAMES = List.of(
            // Populates the passed-in object from the database, the historical
            // OpenELIS "getData" idiom, a read despite the void return.
            "getData",
            // Returns a transient copy; writes nothing.
            "getTransientTypeOfSampleById",
            // Flushes an in-memory read cache, persisting nothing. It must stay
            // reachable with catalogue:view because getUserSampleTypes, the call
            // behind every order-entry sample-type dropdown, invokes it itself
            // (UserServiceImpl), so pinning it to a management privilege would deny
            // order entry outright.
            "clearCache");

    @Test
    public void noWriteMethodIsReachableWithCatalogueViewAlone() throws IOException {
        List<String> exposed = new ArrayList<>();

        for (Path file : catalogueGatedInterfaces()) {
            String source = Files.readString(file);
            if (!hasCatalogueViewAtTypeLevel(source)) {
                continue;
            }
            for (String method : ungatedDeclarations(source)) {
                if (READS_WITH_MUTATING_NAMES.contains(method)) {
                    continue;
                }
                if (MUTATING_NAME.matcher(method).matches()) {
                    exposed.add(MAIN_JAVA.relativize(file) + "#" + method);
                }
            }
        }

        assertEquals("These methods look like writes but inherit a type-level gate that accepts PRIV_CATALOGUE_VIEW,"
                + " so any order-entry role can call them. Give each its own @PreAuthorize naming the management"
                + " privilege. Exposed: " + exposed, List.of(), exposed);
    }

    /**
     * Inversion test. The scan above passes trivially if the declaration regex
     * matches nothing, if no interface is recognised as catalogue-gated, or if the
     * mutating-verb list is empty, three separate ways to get a green test that
     * checks nothing. Each is probed directly.
     */
    @Test
    public void scannerWouldCatchAnExposedWrite() throws IOException {
        String fixture = "@PreAuthorize(\"hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')\")\n"
                + "public interface FixtureService extends BaseObjectService<Panel, String> {\n"
                + "    List<Panel> getAllPanels();\n" + "    void deleteEverything(String id);\n"
                + "    @PreAuthorize(\"hasAuthority('PRIV_PANEL_MANAGE')\")\n" + "    void updateGuarded(Panel p);\n"
                + "}\n";

        assertTrue("fixture must be recognised as catalogue-gated", hasCatalogueViewAtTypeLevel(fixture));

        List<String> ungated = ungatedDeclarations(fixture);
        assertTrue("an unguarded write must be seen as ungated", ungated.contains("deleteEverything"));
        assertTrue("a read must be seen as ungated", ungated.contains("getAllPanels"));
        assertFalse("a method with its own gate must not be reported", ungated.contains("updateGuarded"));

        assertTrue("the verb list must flag a delete", MUTATING_NAME.matcher("deleteEverything").matches());
        assertFalse("the verb list must not flag a plain read", MUTATING_NAME.matcher("getAllPanels").matches());

        // And the real scan must actually be looking at something.
        assertFalse("no catalogue-gated interfaces found, the scan would pass vacuously",
                catalogueGatedInterfaces().isEmpty());
    }

    private boolean hasCatalogueViewAtTypeLevel(String source) {
        int declaration = source.indexOf("public interface");
        if (declaration < 0) {
            return false;
        }
        // Only annotations between the imports and the interface keyword are
        // type-level; a PRIV_CATALOGUE_VIEW further down is on a method.
        return source.substring(0, declaration).contains("PRIV_CATALOGUE_VIEW");
    }

    /** Method names in the interface body that carry no {@code @PreAuthorize}. */
    private List<String> ungatedDeclarations(String source) {
        List<String> names = new ArrayList<>();
        boolean gated = false;
        for (String raw : source.substring(source.indexOf("public interface")).split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("*") || line.startsWith("/*") || line.startsWith("//")) {
                continue;
            }
            if (line.startsWith("@PreAuthorize")) {
                gated = true;
                continue;
            }
            if (line.startsWith("@")) {
                continue;
            }
            Matcher matcher = DECLARATION.matcher(line);
            if (matcher.matches()) {
                if (!gated) {
                    names.add(matcher.group(1));
                }
            }
            gated = false;
        }
        return names;
    }

    private List<Path> catalogueGatedInterfaces() throws IOException {
        try (Stream<Path> sources = Files.walk(MAIN_JAVA)) {
            return sources.filter(Files::isRegularFile).filter(p -> p.toString().endsWith("Service.java"))
                    .filter(p -> hasCatalogueViewAtTypeLevel(read(p))).collect(Collectors.toList());
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
