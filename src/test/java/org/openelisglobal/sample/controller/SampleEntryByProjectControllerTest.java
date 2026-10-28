package org.openelisglobal.sample.controller;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Test;

/**
 * The sample entry controllers are singletons. Request-specific FHIR resources
 * must stay local to the request that loaded them, so no controller may keep
 * them in instance fields.
 */
public class SampleEntryByProjectControllerTest {

    @Test
    public void controllersKeepNoFhirResourcesInInstanceFields() {
        assertNoFhirResourceFields(SampleEntryByProjectController.class);
        assertNoFhirResourceFields(SampleTbEntryController.class);
    }

    private void assertNoFhirResourceFields(Class<?> controller) {
        for (Field field : controller.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            assertFalse(
                    controller.getSimpleName() + " must not keep request state in the instance field '"
                            + field.getName() + "'",
                    Resource.class.isAssignableFrom(field.getType()));
        }
    }
}
