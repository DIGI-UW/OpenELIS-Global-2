package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.qc.controller.QCAlertRestController;
import org.openelisglobal.qc.controller.QCChartDataRestController;
import org.openelisglobal.qc.controller.QCRestController;
import org.openelisglobal.qc.controller.QCViolationRestController;
import org.openelisglobal.result.controller.AnalyzerResultsController;
import org.openelisglobal.security.login.CustomUserDetailsService;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Evaluates each handler's effective {@code @PreAuthorize} rule, the method's
 * when it has one and otherwise the class's, against the authorities a real
 * login grants for a role.
 */
public class AnalyzerRoleBoundaryTest {

    private static final Authentication IMPORT_ROLE = login("Analyser Import");
    private static final Authentication GLOBAL_ADMIN = login(Constants.ROLE_GLOBAL_ADMIN);

    private static final List<Class<?>> CONFIGURATION = List.of(AnalyzerTypeRestController.class,
            AnalyzerActivationRestController.class, AnalyzerConnectionProbeRestController.class,
            AnalyzerMappingRestController.class, AnalyzerBridgePairingRestController.class);

    private static final List<Class<?>> QUALITY_CONTROL = List.of(QCRestController.class, QCAlertRestController.class,
            QCChartDataRestController.class, QCViolationRestController.class);

    private static final List<Class<?>> IMPORT_WORK = List.of(AnalyzerResultsController.class,
            ImportIssuesRestController.class, AnalyzerDeliveryIssuesRestController.class,
            AnalyzerDeliveryBundleRestController.class, AnalyzerFailedRunRestController.class);

    /**
     * The analyzer reads the QC and microbiology pages need; the rest of the
     * controller is setup.
     */
    private static final Set<String> ANALYZER_LIST_READS = Set.of("list", "get");

    @Test
    public void onlyGlobalAdminConfiguresAnalyzers() {
        List<String> handlers = new ArrayList<>();
        for (Class<?> controller : CONFIGURATION) {
            handlers.addAll(handlers(controller));
        }
        handlers.addAll(instanceHandlers(false));

        for (String handler : handlers) {
            assertFalse(handler + " admits the Analyser Import role", admits(handler, IMPORT_ROLE));
            assertTrue(handler + " refuses Global Admin", admits(handler, GLOBAL_ADMIN));
        }
    }

    @Test
    public void qualityControlDoesNotAdmitTheImportRole() {
        for (Class<?> controller : QUALITY_CONTROL) {
            for (String handler : handlers(controller)) {
                assertFalse(handler + " admits the Analyser Import role", admits(handler, IMPORT_ROLE));
                assertTrue(handler + " refuses Global Admin", admits(handler, GLOBAL_ADMIN));
            }
        }
    }

    @Test
    public void importRoleKeepsResultReviewIssuesAndTheAnalyzerList() {
        List<String> handlers = new ArrayList<>();
        for (Class<?> controller : IMPORT_WORK) {
            handlers.addAll(handlers(controller));
        }
        handlers.addAll(instanceHandlers(true));

        for (String handler : handlers) {
            assertTrue(handler + " refuses the Analyser Import role", admits(handler, IMPORT_ROLE));
        }
    }

    private static Authentication login(String roleName) {
        Set<String> authorities = new LinkedHashSet<>();
        CustomUserDetailsService.addAuthoritiesForRole(roleName, authorities);
        return UsernamePasswordAuthenticationToken.authenticated("user", null,
                authorities.stream().map(SimpleGrantedAuthority::new).toList());
    }

    private static boolean admits(String handler, Authentication user) {
        String[] parts = handler.split("#");
        Class<?> controller;
        try {
            controller = Class.forName(parts[0]);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        Method method = null;
        for (Method candidate : controller.getDeclaredMethods()) {
            if (candidate.getName().equals(parts[1]) && isHandler(candidate)) {
                method = candidate;
            }
        }
        PreAuthorize rule = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
        if (rule == null) {
            rule = AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize.class);
        }
        if (rule == null) {
            return true;
        }
        SecurityExpressionRoot root = new SecurityExpressionRoot(user) {
        };
        return Boolean.TRUE.equals(new SpelExpressionParser().parseExpression(rule.value())
                .getValue(new StandardEvaluationContext(root), Boolean.class));
    }

    private static List<String> handlers(Class<?> controller) {
        List<String> handlers = new ArrayList<>();
        for (Method method : controller.getDeclaredMethods()) {
            if (isHandler(method)) {
                handlers.add(controller.getName() + "#" + method.getName());
            }
        }
        assertFalse(controller.getSimpleName() + " has no handlers", handlers.isEmpty());
        return handlers;
    }

    private static List<String> instanceHandlers(boolean listReads) {
        List<String> selected = handlers(AnalyzerInstanceRestController.class).stream().filter(
                handler -> ANALYZER_LIST_READS.contains(handler.substring(handler.indexOf('#') + 1)) == listReads)
                .toList();
        assertFalse("AnalyzerInstanceRestController has no such handlers", selected.isEmpty());
        return selected;
    }

    private static boolean isHandler(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }
}
