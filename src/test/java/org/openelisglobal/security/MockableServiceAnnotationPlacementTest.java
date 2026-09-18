package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * A {@code @PreAuthorize}-gated service that a test publishes as a Spring
 * {@code @Bean} must be mocked with
 * {@code withSettings().withoutAnnotations()}.
 *
 * <p>
 * Mockito copies the interface's {@code @PreAuthorize} onto the generated mock
 * — class-level AND method-level alike — so the mock carries the annotation
 * twice and Spring Security's
 * {@code AuthorizationAnnotationUtils.findUniqueAnnotation} throws
 * {@code AnnotationConfigurationException: Found more than one annotation
 * of type ... PreAuthorize}. Because the bean is in the context, that aborts
 * the whole Spring context and every test that needs it.
 *
 * <p>
 * This has bitten twice. A class-level gate on {@code WHONetReportService}
 * produced ~16,900 errors across unrelated suites (storage, compliance,
 * localization, vector…). Fixing only that revealed seven more — this time
 * METHOD-level gated — in the analyzer, alert and notification slice tests. The
 * signal is misleading in both cases: failures surface as NPEs on
 * {@code @Autowired} fields in {@code tearDown}, far from the real cause.
 *
 * <p>
 * Only mocks published as context {@code @Bean}s matter. A plain
 * {@code mock(Foo.class)} inside a unit test has no method-security interceptor
 * evaluating its annotations, which is why ~160 such call sites are harmless
 * and deliberately not flagged here.
 */
public class MockableServiceAnnotationPlacementTest {

    private static final Path TEST_ROOT = Paths.get("src/test/java");
    private static final Path MAIN_ROOT = Paths.get("src/main/java");

    /**
     * A {@code @Bean} method body, non-greedily up to its closing brace. The
     * indentation is not fixed: {@code @Bean} methods live both at class level (4
     * spaces) and inside a nested static {@code TestConfig} (8). Matching a literal
     * {@code "\n    }"} silently skipped every nested one — which is how the first
     * version of this guard passed its own inversion check.
     */
    private static final Pattern BEAN_METHOD = Pattern.compile("@Bean\\b[\\s\\S]{0,800}?\\n[ \\t]*\\}");

    /** {@code mock(SomeService.class)} with no trailing settings argument. */
    private static final Pattern BARE_MOCK = Pattern.compile("mock\\((\\w+)\\.class\\)(?!\\s*,)");

    @Test
    public void gatedServicesMockedAsBeans_disableAnnotationCopying() throws IOException {
        Map<String, Path> mainSources = indexMainSources();
        // Guard against a silent pass if the tree moves.
        assertTrue("Indexed no main sources under " + MAIN_ROOT + " — test would silently pass",
                mainSources.size() > 100);

        List<String> violations = new ArrayList<>();
        try (Stream<Path> tests = Files.walk(TEST_ROOT)) {
            for (Path test : tests.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = new String(Files.readAllBytes(test));
                Matcher bean = BEAN_METHOD.matcher(src);
                while (bean.find()) {
                    Matcher mock = BARE_MOCK.matcher(bean.group());
                    while (mock.find()) {
                        String type = mock.group(1);
                        Path main = mainSources.get(type);
                        if (main == null) {
                            continue; // third-party type (FhirContext, CloseableHttpClient, …)
                        }
                        if (new String(Files.readAllBytes(main)).contains("@PreAuthorize")) {
                            violations.add(type + " mocked as a @Bean in " + test.getFileName()
                                    + " without withSettings().withoutAnnotations()");
                        }
                    }
                }
            }
        }

        assertTrue("Gated services published as mock @Beans must disable Mockito annotation copying,"
                + " or they abort the Spring context: " + violations, violations.isEmpty());
    }

    private Map<String, Path> indexMainSources() throws IOException {
        Map<String, Path> index = new HashMap<>();
        try (Stream<Path> paths = Files.walk(MAIN_ROOT)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String name = p.getFileName().toString();
                index.put(name.substring(0, name.length() - ".java".length()), p);
            });
        }
        return index;
    }
}
