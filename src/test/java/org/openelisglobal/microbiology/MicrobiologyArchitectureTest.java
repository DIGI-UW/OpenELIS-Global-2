package org.openelisglobal.microbiology;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Method;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicroAstRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseReadinessRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.controller.rest.MicroIsolateRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyReferenceRestController;
import org.springframework.transaction.annotation.Transactional;

public class MicrobiologyArchitectureTest {

    @Test
    public void microbiologyControllersDoNotDeclareTransactions() {
        Class<?>[] controllers = { MicroCaseRestController.class, MicroIsolateRestController.class,
                MicroAstRestController.class, MicroCaseReadinessRestController.class,
                MicrobiologyReferenceRestController.class };
        for (Class<?> controller : controllers) {
            assertFalse(controller.isAnnotationPresent(Transactional.class));
            for (Method method : controller.getDeclaredMethods()) {
                assertFalse(method.isAnnotationPresent(Transactional.class));
            }
        }
    }
}
