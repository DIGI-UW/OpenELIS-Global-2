package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * A service interface that AppTestConfig publishes as a Mockito mock bean must
 * carry its {@code @PreAuthorize} on the METHODS, never on the interface
 * itself.
 *
 * <p>
 * Mockito copies a CLASS-level annotation onto the generated mock class, so the
 * mock ends up carrying {@code @PreAuthorize} twice — once inherited from the
 * interface, once on the mock — and Spring Security's
 * {@code AuthorizationAnnotationUtils.findUniqueAnnotation} throws
 * {@code AnnotationConfigurationException: Found more than one annotation of
 * type ... PreAuthorize}. Method-level annotations are not copied, so they are
 * safe.
 *
 * <p>
 * This is not theoretical. A class-level gate on {@code WHONetReportService},
 * which AppTestConfig mocks as a shared context bean, aborted every Spring
 * context that loaded it: one bad annotation produced ~16,900 test errors
 * across unrelated suites (storage, compliance, localization, vector…), because
 * each failing context cascaded into every test that needed it. The signal is
 * misleading too — the errors surface as unrelated NPEs on {@code @Autowired}
 * fields in {@code tearDown}, far from the real cause.
 *
 * <p>
 * Only mocks published as context {@code @Bean}s matter: a plain
 * {@code mock(Foo.class)} inside a unit test has no method-security interceptor
 * evaluating its annotations, which is why ~18 other class-level gates that are
 * mocked locally are harmless.
 */
public class MockableServiceAnnotationPlacementTest {

    private static final Path APP_TEST_CONFIG = Paths.get("src/test/java/org/openelisglobal/AppTestConfig.java");
    private static final Path MAIN = Paths.get("src/main/java");

    /** {@code return mock(SomeService.class);} inside AppTestConfig. */
    private static final Pattern MOCK_BEAN = Pattern.compile("return mock\\((\\w+)\\.class\\)");

    /** A @PreAuthorize sitting immediately above `public interface Foo`. */
    private static final Pattern CLASS_LEVEL = Pattern.compile("^@PreAuthorize\\([^\\n]*\\)\\s*\\npublic interface ",
            Pattern.MULTILINE);

    @Test
    public void mockedServiceBeans_gateOnMethodsNotOnTheInterface() throws IOException {
        String config = new String(Files.readAllBytes(APP_TEST_CONFIG));

        List<String> mockedTypes = new ArrayList<>();
        Matcher m = MOCK_BEAN.matcher(config);
        while (m.find()) {
            mockedTypes.add(m.group(1));
        }
        // Guard against a silent pass if AppTestConfig is restructured.
        assertTrue("Parsed no mock(...) beans from " + APP_TEST_CONFIG + " — test would silently pass",
                mockedTypes.size() > 5);

        List<String> violations = new ArrayList<>();
        for (String type : mockedTypes) {
            Path source = findInterface(type);
            if (source == null) {
                continue; // not one of ours (FhirContext, CloseableHttpClient, …)
            }
            String src = new String(Files.readAllBytes(source));
            if (CLASS_LEVEL.matcher(src).find()) {
                violations.add(type + " (" + source + ") has a CLASS-level @PreAuthorize but is published as a"
                        + " Mockito mock bean in AppTestConfig — move the annotation onto its methods");
            }
        }

        assertTrue("Mocked service beans must not carry a class-level @PreAuthorize: " + violations,
                violations.isEmpty());
    }

    private Path findInterface(String simpleName) throws IOException {
        return Files.walk(MAIN).filter(p -> p.getFileName().toString().equals(simpleName + ".java")).findFirst()
                .orElse(null);
    }
}
