package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the method-security semantics the RBAC migration relies on: a
 * class-level {@code @PreAuthorize} on a service INTERFACE is enforced by
 * {@code @EnableMethodSecurity} on a real implementation bean, and a
 * method-level interface annotation is enforced on a JDK-proxy stub bean (the
 * shape {@code SecuritySliceMockMvcTest#nullStub} produces). If a framework
 * upgrade changes either behavior, this test fails before the holes go silent.
 */
public class ClassLevelPreAuthorizeSemanticsTest {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    public interface ClassGatedService {
        String ping();
    }

    public interface MethodGatedService {
        @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
        String ping();
    }

    public static class ClassGatedServiceImpl implements ClassGatedService {
        @Override
        public String ping() {
            return "pong";
        }
    }

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class Config {
        @Bean
        ClassGatedService realImpl() {
            return new ClassGatedServiceImpl();
        }

        @Bean
        MethodGatedService methodGatedStub() {
            // NOT a Mockito mock: Mockito 2.x copies @PreAuthorize onto the generated
            // mock class, so Spring Security's unique-annotation scan finds it twice
            // and throws AnnotationConfigurationException. A JDK proxy leaves the
            // interface as the single annotation source. Slice tests use
            // SecuritySliceMockMvcTest#nullStub for the same reason.
            return (MethodGatedService) Proxy.newProxyInstance(MethodGatedService.class.getClassLoader(),
                    new Class<?>[] { MethodGatedService.class }, (proxy, method, args) -> {
                        switch (method.getName()) {
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "stub<MethodGatedService>";
                        default:
                            return null;
                        }
                    });
        }
    }

    @After
    public void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authWith(String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("u", "p", authorities);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void classLevelInterfaceAnnotation_isEnforcedOnRealImpl() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            ClassGatedService svc = ctx.getBean(ClassGatedService.class);
            authWith("PRIV_OTHER");
            try {
                svc.ping();
                fail("class-level interface @PreAuthorize was NOT enforced on real impl");
            } catch (AccessDeniedException expected) {
                // enforced
            }
            authWith("PRIV_TEST_CONFIGURE");
            assertEquals("pong", svc.ping());
        }
    }

    @Test
    public void methodLevelInterfaceAnnotation_isEnforcedOnJdkProxyStub() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            MethodGatedService svc = ctx.getBean(MethodGatedService.class);
            authWith("PRIV_OTHER");
            try {
                String r = svc.ping();
                fail("method-level interface @PreAuthorize NOT enforced on JDK proxy stub (returned " + r + ")");
            } catch (AccessDeniedException expected) {
                // enforced cleanly
            }
            authWith("PRIV_TEST_CONFIGURE");
            assertEquals(null, svc.ping());
        }
    }
}
