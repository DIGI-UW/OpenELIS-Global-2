package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Companion to {@link ServicePrivilegeCoverageTest}, which scans service
 * <em>interfaces</em> only. A {@code @Service} <em>class</em> with no interface
 * is invisible to that scan, so it passes by never being looked at rather than
 * by being authorized.
 *
 * <p>
 * That blind spot was not theoretical. {@code MicroAstAnalyzerEventService} and
 * {@code MicroCultureAnalyzerEventService} are exactly this shape: their
 * controllers' {@code hasRole('ANALYSER_IMPORT')} was the only guard, and when
 * the S011c migration removed controller guards, analyzer result ingestion was
 * left reachable by any authenticated user with nothing failing.
 *
 * <p>
 * This test asserts the narrower, checkable property: a {@code @Service} class
 * that a controller can reach must either carry {@code @PreAuthorize} or be
 * explicitly marked {@code @CrossDomainService} with a justification. Classes
 * no controller references are out of scope — they are reached only through
 * other services, which carry their own gates.
 */
public class ServiceClassPrivilegeCoverageTest {

    private static final Path MAIN = Paths.get("src/main/java");

    private static final Pattern SERVICE_ANNOTATION = Pattern.compile("^@Service\\b", Pattern.MULTILINE);
    private static final Pattern PUBLIC_CLASS = Pattern.compile("^public class (\\w+)", Pattern.MULTILINE);

    @Test
    public void controllerReachableServiceClasses_areGatedOrExplicitlyExempt() throws IOException {
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(MAIN)) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        // Guard against a silent pass if the tree moves.
        assertTrue("Indexed no sources under " + MAIN + " — test would silently pass", sources.size() > 100);

        Set<String> referencedByControllers = namesReferencedByControllers(sources);

        List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            String src = Files.readString(source);
            if (!SERVICE_ANNOTATION.matcher(src).find()) {
                continue;
            }
            Matcher cls = PUBLIC_CLASS.matcher(src);
            if (!cls.find()) {
                continue;
            }
            String name = cls.group(1);
            // *Impl pairs with an interface; the interface scan already covers it.
            if (name.endsWith("Impl")) {
                continue;
            }
            if (src.contains("@PreAuthorize") || src.contains("CrossDomainService")) {
                continue;
            }
            if (referencedByControllers.contains(name)) {
                violations.add(name + " (" + source + ") is a @Service class reachable from a controller with"
                        + " neither @PreAuthorize nor @CrossDomainService");
            }
        }

        assertTrue("Controller-reachable @Service classes must be gated or explicitly exempted — the interface"
                + " scan cannot see them: " + violations, violations.isEmpty());
    }

    /** Simple-names mentioned anywhere in a *Controller*.java source. */
    private Set<String> namesReferencedByControllers(List<Path> sources) throws IOException {
        Set<String> referenced = new HashSet<>();
        Pattern serviceLike = Pattern
                .compile("\\b([A-Z]\\w*(?:Service|Util|Utility|Worker|Submitter|Entry|Update))\\b");
        for (Path source : sources) {
            if (!source.getFileName().toString().contains("Controller")) {
                continue;
            }
            Matcher m = serviceLike.matcher(Files.readString(source));
            while (m.find()) {
                referenced.add(m.group(1));
            }
        }
        return referenced;
    }
}
