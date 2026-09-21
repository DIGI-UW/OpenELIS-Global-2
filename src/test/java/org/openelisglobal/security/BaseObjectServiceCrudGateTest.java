package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.openelisglobal.common.security.CrudPrivileges;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins how the CRUD inherited from a generic base interface is authorized, in
 * the PRODUCTION shape: a generic abstract impl that implements the base and
 * holds the CRUD bodies, a descendant service interface, and a concrete impl
 * that inherits the CRUD — proxied by real method security. Slice tests use
 * JDK-proxy stubs whose class implements the descendant interface directly, and
 * that shape hides the effects pinned here.
 */
public class BaseObjectServiceCrudGateTest {

    static final String READ = "T(org.openelisglobal.common.security.CrudGate).read(#root)";
    static final String WRITE = "T(org.openelisglobal.common.security.CrudGate).write(#root)";

    public interface Base<T, PK> {
        @PreAuthorize(READ)
        T get(PK id);

        @PreAuthorize(WRITE)
        void delete(PK id, String user);
    }

    public abstract static class BaseImpl<T, PK> implements Base<T, PK> {
        @Override
        public T get(PK id) {
            return null;
        }

        @Override
        public void delete(PK id, String user) {
        }
    }

    // A — the interface declares its privileges
    @CrudPrivileges(read = "PRIV_A_VIEW", write = "PRIV_A_MANAGE")
    public interface DeclaringService extends Base<String, Long> {
    }

    public static class DeclaringServiceImpl extends BaseImpl<String, Long> implements DeclaringService {
    }

    // B — descendant carries a type-level gate only (the ~90 already-covered
    // services)
    @PreAuthorize("hasAuthority('PRIV_B_VIEW')")
    public interface TypeLevelService extends Base<String, Long> {
    }

    public static class TypeLevelServiceImpl extends BaseImpl<String, Long> implements TypeLevelService {
    }

    // C — neither: transitional, open
    public interface OpenService extends Base<String, Long> {
    }

    public static class OpenServiceImpl extends BaseImpl<String, Long> implements OpenService {
    }

    // D — descendant REDECLARES an inherited method with its own gate (the shape
    // AlertService briefly had, and
    // LocalizationService/MenuService/UnitOfMeasureService have)
    public interface RedeclaringService extends Base<String, Long> {
        @Override
        @PreAuthorize("hasAuthority('PRIV_D_VIEW')")
        String get(Long id);
    }

    public static class RedeclaringServiceImpl extends BaseImpl<String, Long> implements RedeclaringService {
    }

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class Config {
        @Bean
        DeclaringService a() {
            return new DeclaringServiceImpl();
        }

        @Bean
        TypeLevelService b() {
            return new TypeLevelServiceImpl();
        }

        @Bean
        OpenService c() {
            return new OpenServiceImpl();
        }

        @Bean
        RedeclaringService d() {
            return new RedeclaringServiceImpl();
        }

        /**
         * E — the slice-test shape: a JDK-proxy stub of the declaring interface, no
         * impl at all.
         */
        @Bean
        DeclaringService aStub() {
            return GatedServiceMocks.stubbableMock(DeclaringService.class);
        }
    }

    @After
    public void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void authWith(String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("u", "p", authorities);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static String outcome(Runnable call) {
        try {
            call.run();
            return "allowed";
        } catch (AccessDeniedException e) {
            return "denied";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    @Test
    public void declaredPrivileges_gateInheritedReadsAndWritesSeparately() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            DeclaringService a = ctx.getBean("a", DeclaringService.class);
            authWith("PRIV_OTHER");
            assertEquals("denied", outcome(() -> a.get(1L)));
            authWith("PRIV_A_VIEW");
            assertEquals("allowed", outcome(() -> a.get(1L)));
            assertEquals("read privilege must not unlock a write", "denied", outcome(() -> a.delete(1L, "u")));
            authWith("PRIV_A_MANAGE");
            assertEquals("allowed", outcome(() -> a.delete(1L, "u")));
        }
    }

    /**
     * The declaration must bind the same way for a stub as for the real impl, or
     * slice tests lie.
     */
    @Test
    public void declaredPrivileges_alsoGateAJdkProxyStubOfTheInterface() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            DeclaringService stub = ctx.getBean("aStub", DeclaringService.class);
            authWith("PRIV_OTHER");
            assertEquals("denied", outcome(() -> stub.delete(1L, "u")));
            authWith("PRIV_A_MANAGE");
            assertEquals("allowed", outcome(() -> stub.delete(1L, "u")));
        }
    }

    @Test
    public void typeLevelGateOnDescendant_stillCoversInheritedCrud_viaFallback() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            TypeLevelService b = ctx.getBean(TypeLevelService.class);
            authWith("PRIV_OTHER");
            assertEquals("denied", outcome(() -> b.get(1L)));
            assertEquals("denied", outcome(() -> b.delete(1L, "u")));
            authWith("PRIV_B_VIEW");
            assertEquals("allowed", outcome(() -> b.get(1L)));
        }
    }

    @Test
    public void undeclaredService_inheritedCrudStaysOpen_transitional() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            OpenService c = ctx.getBean(OpenService.class);
            authWith("PRIV_OTHER");
            assertEquals("allowed", outcome(() -> c.get(1L)));
        }
    }

    /**
     * Discovery pin: what does a descendant's redeclared method-level gate do in
     * the production shape? Whatever the answer, it is not "enforced as written",
     * which is why InheritedCrudGateCoverageTest forbids the shape.
     */
    @Test
    public void redeclaredMethodGateOnDescendant_isNotWhatGovernsInheritedCrud() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            RedeclaringService d = ctx.getBean(RedeclaringService.class);
            authWith("PRIV_D_VIEW"); // exactly what the redeclared gate asks for
            String withOwnPrivilege = outcome(() -> d.get(1L));
            authWith("PRIV_OTHER");
            String withoutIt = outcome(() -> d.get(1L));
            if (withOwnPrivilege.equals("allowed") && withoutIt.equals("denied")) {
                fail("the redeclared method-level gate IS enforced in the production shape; revise CrudPrivileges'"
                        + " javadoc and the ratchet rule");
            }
            // Observed behaviour is recorded in the assertion message for the reader.
            assertEquals("redeclared gate: with own privilege=" + withOwnPrivilege + ", without=" + withoutIt,
                    withOwnPrivilege, withoutIt);
        }
    }
}
