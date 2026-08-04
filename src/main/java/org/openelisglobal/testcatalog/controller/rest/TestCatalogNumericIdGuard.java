package org.openelisglobal.testcatalog.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OGC-1153 — one guard for the whole {@code /rest/test-catalog} URL space:
 * answers 404 when a numeric entity id in the path is not actually numeric,
 * instead of letting the value reach Hibernate and escape as a 500.
 *
 * <p>
 * Root cause: every catalog id column is {@code NUMERIC(10)} bound through
 * {@code LIMSStringNumberUserType}, whose {@code nullSafeSet} does a bare
 * {@code Integer.parseInt}. The handlers take the id as a {@code String} and
 * pass it straight to {@code TestService.getTestById} (HQL
 * {@code where t.id = :id}), so {@code /tests/notanumber/basic-info} threw
 * {@code NumberFormatException} out of the DAO — {@code getTestById} only
 * catches {@code HibernateException}. {@code ControllerSetup}, an
 * {@code @Order(HIGHEST_PRECEDENCE)} {@code @ControllerAdvice} mapping
 * {@code RuntimeException}, then rendered it as 500.
 *
 * <p>
 * 404 rather than 400 because that is already the answer for a well-formed but
 * absent id ({@code /tests/999999}, {@code /tests/-1}), so the SPA keeps one
 * uniform "no such test" branch.
 *
 * <p>
 * An interceptor keyed on the URL — not a per-handler guard, a {@code @Pattern}
 * on each path variable, or a package-scoped {@code @ControllerAdvice} —
 * because the defect belongs to the URL space, not to a Java package: six
 * controllers serve {@code /rest/test-catalog/**} and two of them
 * ({@code testreagentlink}, {@code testalertrule}) live outside
 * {@code org.openelisglobal.testcatalog}, so a package-scoped advice would miss
 * them. An exception-mapping advice could not work here at all —
 * {@code ControllerSetup} sits at {@code HIGHEST_PRECEDENCE} and
 * {@code ExceptionHandlerExceptionResolver} takes the first advice bean that
 * matches, so a later advice never sees the exception. A new endpoint anywhere
 * under this prefix that takes one of {@link #NUMERIC_ID_PATH_VARIABLES} is
 * covered with no action from its author; the guard registers itself, so it
 * cannot be half-wired.
 *
 * <p>
 * The set of guarded names is explicit and must stay that way: ids in this URL
 * space are not uniformly numeric — {@code test_alert_rule.id} is a 36-char
 * UUID, as are result-component ids — so a blanket "every {@code *Id} must be
 * digits" rule would break them. Add a name here when a new endpoint introduces
 * another {@code NUMERIC(10)}-backed id;
 * {@code TestCatalogNumericIdGuardTest.everyCatalogPathVariableIsClassified}
 * fails until an unclassified one is triaged.
 */
@Component
public class TestCatalogNumericIdGuard implements HandlerInterceptor, WebMvcConfigurer {

    /** The URL space this guard owns — every Test Catalog REST endpoint. */
    public static final String GUARDED_PATH_PATTERN = "/rest/test-catalog/**";

    /**
     * Path-variable names that name a {@code NUMERIC(10)} entity id, and so must
     * parse as a non-negative int before any DAO sees them.
     */
    public static final Set<String> NUMERIC_ID_PATH_VARIABLES = Set.of("testId", "sourceId", "sampleTypeId", "panelId");

    /** {@code NUMERIC(10)} — 2147483647 is the widest id the user type can bind. */
    private static final int MAX_ID_DIGITS = 10;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns(GUARDED_PATH_PATTERN);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map)) {
            return true;
        }
        for (Map.Entry<?, ?> variable : ((Map<?, ?>) attribute).entrySet()) {
            if (NUMERIC_ID_PATH_VARIABLES.contains(String.valueOf(variable.getKey()))
                    && !isNumericId(variable.getValue())) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
        }
        return true;
    }

    /**
     * True when the path value is a run of ASCII digits that fits the id column. A
     * sign, emptiness or an out-of-range value all read as "no such row" —
     * {@code -1} already answered 404 before this guard existed and must keep doing
     * so.
     *
     * <p>
     * Trimmed first because {@code ControllerSetup}'s {@code @InitBinder} registers
     * a global {@code StringTrimmerEditor} for {@code String}, so a padded id
     * ({@code /tests/%205/basic-info}) reaches the handler as {@code "5"} and
     * resolves today; the guard must not start rejecting a URL that still works.
     */
    public static boolean isNumericId(Object value) {
        if (value == null) {
            return false;
        }
        String id = String.valueOf(value).trim();
        if (id.isEmpty() || id.length() > MAX_ID_DIGITS) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char digit = id.charAt(i);
            if (digit < '0' || digit > '9') {
                return false;
            }
        }
        return Long.parseLong(id) <= Integer.MAX_VALUE;
    }
}
