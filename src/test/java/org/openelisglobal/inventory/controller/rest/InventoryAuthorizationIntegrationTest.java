package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every inventory REST controller declares a role guard.
 *
 * <p>
 * The interceptor cannot: it matches {@code system_module_url.url_path} by
 * exact string, so no row reaches a path carrying an id, and an unmatched
 * {@code /rest} path is allowed rather than refused. So these annotations are
 * the whole of the module's server-side authorization, and a controller added
 * without one is silently open to any authenticated user — which is what the
 * roster case below exists to catch.
 *
 * <p>
 * This context excludes {@code SecurityConfig} from its component scan, so
 * method security is not switched on here and no call made through it can be
 * refused. That is why this pins the declaration only: whether the guard is
 * enforced is proven in {@code InventoryAuthorizationSecurityTest}, which runs
 * as a security slice.
 */
public class InventoryAuthorizationIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The roster: a new controller in this package with no guard is the failure
     * this pins, not a specific endpoint.
     */
    @Test
    public void everyInventoryRestControllerCarriesARoleGuard() {
        List<String> unguarded = applicationContext.getBeansWithAnnotation(RestController.class).values().stream()
                .map(bean -> org.springframework.aop.support.AopUtils.getTargetClass(bean))
                .filter(type -> type.getPackageName().startsWith("org.openelisglobal.inventory"))
                .filter(type -> type.getAnnotation(PreAuthorize.class) == null).map(Class::getSimpleName).sorted()
                .toList();

        assertEquals("every inventory REST controller needs a role guard; the interceptor will not"
                + " cover these paths and an unmatched /rest path is allowed", List.of(), unguarded);
    }

    @Test
    public void theRosterCaseSeesTheControllersItClaimsTo() {
        long inventoryControllers = applicationContext.getBeansWithAnnotation(RestController.class).values().stream()
                .map(bean -> org.springframework.aop.support.AopUtils.getTargetClass(bean))
                .filter(type -> type.getPackageName().startsWith("org.openelisglobal.inventory")).count();

        assertEquals("the roster case passes vacuously if it finds nothing", 9, inventoryControllers);
    }

}
