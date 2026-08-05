package org.openelisglobal.textmacro;

import static org.junit.Assert.assertNotNull;

import jakarta.persistence.EntityManagerFactory;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.textmacro.valueholder.TextMacro;
import org.springframework.beans.factory.annotation.Autowired;

public class TextMacroOrmValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void textMacroMappingIsRegistered() {
        assertNotNull(entityManagerFactory.getMetamodel().entity(TextMacro.class));
        assertNotNull(entityManagerFactory.getMetamodel().entity(TextMacro.class).getAttribute("contexts"));
    }
}
