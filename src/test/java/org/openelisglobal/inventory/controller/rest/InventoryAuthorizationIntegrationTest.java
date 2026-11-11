package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

public class InventoryAuthorizationIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ApplicationContext applicationContext;

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

        assertEquals("the roster case passes vacuously if it finds nothing", 10, inventoryControllers);
    }

}
