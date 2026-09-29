package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the two facts {@link GatedServiceMocks} rests on, by reflection on real
 * generated types rather than by reading source:
 *
 * <ol>
 * <li>Mockito's generated mock carries the mocked type's {@code @PreAuthorize}
 * itself, so Spring Security's unique-annotation scan sees it twice (mock +
 * declaring type) and throws. This is the defect; if it ever stops reproducing,
 * Mockito has changed and the helper can be reconsidered.</li>
 * <li>The stubs {@code GatedServiceMocks} produces carry no annotation of their
 * own, so the same scan finds exactly one — on the interface or superclass —
 * and method security enforces it cleanly, for interfaces AND classes.</li>
 * </ol>
 *
 * <p>
 * A source scan then keeps test configurations on the helper: no {@code @Bean}
 * may hand Spring a raw Mockito mock of a gated service, and
 * {@code withoutAnnotations()} — which does not strip these annotations on the
 * pinned Mockito version, and was mistaken for a fix — may not appear.
 */
public class GatedServiceMocksTest {

    static final String GATE = "hasAuthority('PRIV_TEST_CONFIGURE')";

    @PreAuthorize(GATE)
    public interface TypeGatedIface {
        String ping();
    }

    public interface MethodGatedIface {
        @PreAuthorize(GATE)
        String ping();
    }

    /** Constructor takes a dependency, as a real {@code @Service} class would. */
    @PreAuthorize(GATE)
    public static class TypeGatedClass {
        public TypeGatedClass(Object dependency) {
        }

        public String ping() {
            return "real";
        }
    }

    public static class MethodGatedClass {
        @PreAuthorize(GATE)
        public String ping() {
            return "real";
        }
    }

    private static final Class<?>[] FIXTURES = { TypeGatedIface.class, MethodGatedIface.class, TypeGatedClass.class,
            MethodGatedClass.class };

    /** Occurrences Spring Security's {@code findUniqueAnnotation} would count. */
    private static long preAuthorizeSources(java.lang.reflect.AnnotatedElement element) {
        return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
                .stream(PreAuthorize.class).count();
    }

    private static Method ping(Object bean) throws NoSuchMethodException {
        return bean.getClass().getMethod("ping");
    }

    @Test
    public void mockitoMocksCarryTheGateThemselves_whichIsTheDefect() throws Exception {
        for (Class<?> type : FIXTURES) {
            Object mock = Mockito.mock(type);
            boolean onType = mock.getClass().getDeclaredAnnotation(PreAuthorize.class) != null;
            boolean onMethod = ping(mock).getDeclaredAnnotation(PreAuthorize.class) != null;
            assertTrue("Mockito no longer copies @PreAuthorize onto the mock of " + type.getSimpleName()
                    + " — GatedServiceMocks may be retirable", onType || onMethod);
            assertTrue(type.getSimpleName() + ": Spring would see the gate more than once",
                    preAuthorizeSources(mock.getClass()) + preAuthorizeSources(ping(mock)) >= 2);
        }
    }

    @Test
    public void gatedStubsCarryNoGateOfTheirOwn() throws Exception {
        for (Class<?> type : FIXTURES) {
            Object stub = GatedServiceMocks.stubbableMock(type);
            assertTrue(type.getSimpleName() + " stub is not assignable to its type", type.isInstance(stub));
            assertNull(type.getSimpleName() + ": stub type carries @PreAuthorize",
                    stub.getClass().getDeclaredAnnotation(PreAuthorize.class));
            assertNull(type.getSimpleName() + ": stub method carries @PreAuthorize",
                    ping(stub).getDeclaredAnnotation(PreAuthorize.class));
            // Exactly one source in the hierarchy: the declaring interface/superclass.
            assertEquals(type.getSimpleName() + ": gate sources across type+method", 1,
                    preAuthorizeSources(stub.getClass()) + preAuthorizeSources(ping(stub)));
        }
    }

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class Config {
        @Bean
        TypeGatedIface typeGatedIface() {
            return GatedServiceMocks.stubbableMock(TypeGatedIface.class);
        }

        @Bean
        MethodGatedIface methodGatedIface() {
            return GatedServiceMocks.stubbableMock(MethodGatedIface.class);
        }

        @Bean
        TypeGatedClass typeGatedClass() {
            return GatedServiceMocks.stubbableMock(TypeGatedClass.class);
        }

        @Bean
        MethodGatedClass methodGatedClass() {
            return GatedServiceMocks.stubbableMock(MethodGatedClass.class);
        }
    }

    @After
    public void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void authWith(String authority) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("u", "p", authority);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * End to end, for every shape: the bean is method-security proxied, the gate
     * denies the wrong authority and admits the right one, and stubbing/verifying
     * through {@code mockBehind} reaches the Mockito mock behind both proxy layers.
     */
    @Test
    public void methodSecurityEnforcesTheGateOnEveryStubShape_andStubbingWorksThroughMockBehind() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            for (Class<?> type : FIXTURES) {
                Object bean = ctx.getBean(type);
                Object mock = GatedServiceMocks.mockBehind(bean);
                assertTrue(type.getSimpleName() + ": mockBehind did not reach a Mockito mock",
                        Mockito.mockingDetails(mock).isMock());
                assertSame(mock, GatedServiceMocks.mockBehind(mock)); // idempotent on the mock itself

                authWith("PRIV_OTHER");
                try {
                    invokePing(bean);
                    fail(type.getSimpleName() + ": gate was NOT enforced on the stub bean");
                } catch (AccessDeniedException expected) {
                    // enforced, cleanly — no AnnotationConfigurationException
                }

                authWith("PRIV_TEST_CONFIGURE");
                when(invokePing(mock)).thenReturn("stubbed");
                assertEquals(type.getSimpleName(), "stubbed", invokePing(bean));
                invokePing(verify(mock, Mockito.atLeastOnce()));
            }
        }
    }

    private static String invokePing(Object target) {
        try {
            return (String) target.getClass().getMethod("ping").invoke(target);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- source scan

    private static final Path TEST_ROOT = Paths.get("src/test/java");
    private static final Path MAIN_ROOT = Paths.get("src/main/java");
    private static final Set<String> SELF = Set.of("GatedServiceMocks.java", "GatedServiceMocksTest.java");

    /**
     * A {@code @Bean} method body; indentation varies (class-level vs nested
     * config).
     */
    static final Pattern BEAN_METHOD = Pattern.compile("@Bean\\b[\\s\\S]{0,1500}?\\n[ \\t]*\\}");
    static final Pattern MOCK_OF = Pattern.compile("\\bmock\\(\\s*(\\w+)\\.class");

    /**
     * Pure function over one test source, so its own detection can be inverted
     * below.
     */
    static List<String> violationsIn(String fileName, String src, Set<String> gatedTypes) {
        List<String> out = new ArrayList<>();
        if (src.contains("withoutAnnotations()")) {
            out.add(fileName + ": withoutAnnotations() does not strip @PreAuthorize on this Mockito version;"
                    + " use GatedServiceMocks");
        }
        Matcher bean = BEAN_METHOD.matcher(src);
        while (bean.find()) {
            String body = bean.group();
            boolean wrapped = body.contains("asGatedBean(") || body.contains("stubbableMock(");
            Matcher mock = MOCK_OF.matcher(body);
            while (mock.find()) {
                String type = mock.group(1);
                if (!gatedTypes.contains(type)) {
                    continue;
                }
                boolean returnedDirectly = body.contains("return mock(" + type + ".class")
                        || body.contains("return Mockito.mock(" + type + ".class");
                if (returnedDirectly || !wrapped) {
                    out.add(fileName + ": @Bean publishes a raw Mockito mock of gated " + type
                            + "; return GatedServiceMocks.asGatedBean(mock) or stubbableMock(type)");
                }
            }
        }
        return out;
    }

    @Test
    public void sourceScan_detectsWhatItIsSupposedTo_andAcceptsTheSanctionedShapes() {
        Set<String> gated = Set.of("GatedThing");
        String bad1 = "class T { @Bean\n GatedThing g() {\n return mock(GatedThing.class);\n }\n}";
        String bad2 = "class T { @Bean\n GatedThing g() {\n GatedThing s = mock(GatedThing.class, withSettings().withoutAnnotations());\n return s;\n }\n}";
        String good1 = "class T { @Bean\n GatedThing g() {\n return stubbableMock(GatedThing.class);\n }\n}";
        String good2 = "class T { @Bean\n GatedThing g() {\n GatedThing s = mock(GatedThing.class);\n when(s.x()).thenReturn(1);\n return asGatedBean(s);\n }\n}";
        String good3 = "class T { @Bean\n Other o() {\n return mock(Other.class);\n }\n}"; // not gated
        assertEquals(1, violationsIn("bad1", bad1, gated).size());
        assertTrue(violationsIn("bad2", bad2, gated).size() >= 1);
        assertEquals(List.of(), violationsIn("good1", good1, gated));
        assertEquals(List.of(), violationsIn("good2", good2, gated));
        assertEquals(List.of(), violationsIn("good3", good3, gated));
    }

    @Test
    public void noTestConfigurationHandsSpringARawMockitoMockOfAGatedService() throws IOException {
        Set<String> gated = gatedTypeNames();
        assertTrue("Indexed suspiciously few gated types — scan would silently pass", gated.size() > 100);
        List<String> violations = new ArrayList<>();
        try (Stream<Path> tests = Files.walk(TEST_ROOT)) {
            for (Path test : tests.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = test.getFileName().toString();
                if (SELF.contains(name)) {
                    continue;
                }
                violations.addAll(violationsIn(name, Files.readString(test), gated));
            }
        }
        assertTrue(String.join("\n", violations), violations.isEmpty());
    }

    private static Set<String> gatedTypeNames() throws IOException {
        Set<String> out = new HashSet<>();
        try (Stream<Path> paths = Files.walk(MAIN_ROOT)) {
            for (Path p : paths.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (Files.readString(p).contains("@PreAuthorize")) {
                    String n = p.getFileName().toString();
                    out.add(n.substring(0, n.length() - ".java".length()));
                }
            }
        }
        return out;
    }
}
