package org.openelisglobal.interceptor;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Locks the compensating control behind
 * {@link ModuleAuthenticationInterceptor#isRestFullPath}.
 *
 * <p>
 * Background. The interceptor is deny-by-default: a URL with no
 * {@code system_module_url} row is rejected. Retrofitting REST onto that
 * page-oriented design (2025, "Implement Roles for REST endpoints") added an
 * escape hatch — requests whose path starts with a whitelisted prefix are
 * auto-allowed for any authenticated user rather than being denied. Module
 * authorization therefore does NOT protect these endpoints; the only thing that
 * does is a method-security annotation on the controller.
 *
 * <p>
 * That substitution was reviewed and accepted (PR #2794, which documented it
 * and added {@code hasRole('ADMIN')} to the admin-only controllers), but it is
 * held together by convention alone. A new controller under an auto-allowed
 * prefix that omits {@code @PreAuthorize} is reachable by any authenticated
 * user, and nothing fails — not a build, not a test, not a log line. This test
 * is that missing signal.
 *
 * <p>
 * Scope. Only {@code /api} is asserted. The older {@code /rest} prefix carries
 * substantial pre-existing debt (59 of its 135 controllers declare no
 * method-security annotation at all), so including it here would assert a state
 * the codebase has never been in. Widening {@link #AUTO_ALLOWED_PREFIXES} to
 * {@code /rest} is the goal; it needs that debt paid down first.
 */
public class AutoAllowedPathAuthorizationTest {

    /**
     * Prefixes auto-allowed by {@code isRestFullPath} that this test enforces. Keep
     * in sync when that whitelist grows — a prefix added there without being added
     * here silently opts a new URL space out of module authorization.
     */
    private static final String[] AUTO_ALLOWED_PREFIXES = { "/api" };

    private static final String BASE_PACKAGE = "org.openelisglobal";

    @Test
    public void controllersUnderAutoAllowedPathsDeclareMethodSecurity() throws ClassNotFoundException {
        List<String> violations = new ArrayList<>();

        for (Class<?> controller : findControllers()) {
            if (!hasAutoAllowedMapping(controller)) {
                continue;
            }
            if (!isSecured(controller)) {
                violations.add(controller.getName());
            }
        }

        assertTrue(
                "Controllers mapped under an auto-allowed path prefix " + String.join(", ", AUTO_ALLOWED_PREFIXES)
                        + " bypass ModuleAuthenticationInterceptor's"
                        + " module-permission check, so @PreAuthorize is their only authorization. These declare"
                        + " none, making them reachable by any authenticated user: " + violations,
                violations.isEmpty());
    }

    private List<Class<?>> findControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // @RestController is meta-annotated with @Controller, so this matches both.
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<Class<?>> controllers = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            // Load without initializing: several controllers have static initializers that
            // reach for a live Spring context, which does not exist in a plain unit test.
            controllers.add(Class.forName(definition.getBeanClassName(), false, getClass().getClassLoader()));
        }
        return controllers;
    }

    /**
     * Collects class-level and method-level mappings, since a controller may carry
     * no class-level {@code @RequestMapping} and declare full paths on its handlers
     * instead.
     */
    private boolean hasAutoAllowedMapping(Class<?> controller) {
        Set<String> paths = new LinkedHashSet<>();

        RequestMapping typeMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        if (typeMapping != null) {
            for (String path : typeMapping.path()) {
                paths.add(path);
            }
        }
        for (Method method : org.springframework.util.ReflectionUtils.getAllDeclaredMethods(controller)) {
            // GetMapping and friends are meta-annotated with @RequestMapping, so the
            // merged lookup resolves their paths too.
            RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (methodMapping != null) {
                for (String path : methodMapping.path()) {
                    paths.add(path);
                }
            }
        }

        for (String path : paths) {
            for (String prefix : AUTO_ALLOWED_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A class-level annotation covers handlers added later, so it is the preferred
     * form; per-method annotations are accepted only when every mapped handler
     * carries one.
     */
    private boolean isSecured(Class<?> controller) {
        if (AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class)) {
            return true;
        }

        List<Method> handlers = new ArrayList<>();
        for (Method method : org.springframework.util.ReflectionUtils.getAllDeclaredMethods(controller)) {
            if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null) {
                handlers.add(method);
            }
        }
        if (handlers.isEmpty()) {
            return false;
        }
        for (Method handler : handlers) {
            if (!AnnotatedElementUtils.hasAnnotation(handler, PreAuthorize.class)) {
                return false;
            }
        }
        return true;
    }
}
