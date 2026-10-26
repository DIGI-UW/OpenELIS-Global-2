package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.*;

public class MicroAstReportingOrmValidationTest {
    @Test
    public void choicesAssignmentsBindingsAndResolutionsLoadTogetherWithoutDatabase() {
        var config = new Configuration();
        for (var type : java.util.List.of(MicroAstPanelAssignment.class, MicroAstReportChoice.class,
                MicroMolecularDstBinding.class, MicroDstResolution.class))
            config.addAnnotatedClass(type);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        config.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        try (var factory = config.buildSessionFactory()) {
            assertEquals(4, factory.getMetamodel().getEntities().size());
            assertEquals(String.class,
                    factory.getMetamodel().entity(MicroAstReportChoice.class).getAttribute("readingId").getJavaType());
            assertEquals(String.class, factory.getMetamodel().entity(MicroMolecularDstBinding.class)
                    .getAttribute("componentId").getJavaType());
        }
    }
}
