package org.openelisglobal.microbiology;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Method;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.springframework.transaction.annotation.Transactional;

public class MicrobiologyArchitectureTest {

    @Test
    public void microbiologyControllersDoNotDeclareTransactions() {
        assertFalse(MicroCaseRestController.class.isAnnotationPresent(Transactional.class));
        for (Method method : MicroCaseRestController.class.getDeclaredMethods()) {
            assertFalse(method.isAnnotationPresent(Transactional.class));
        }
    }
}
