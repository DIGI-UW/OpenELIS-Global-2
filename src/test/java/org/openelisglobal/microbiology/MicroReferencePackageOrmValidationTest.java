package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;

import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.*;

public class MicroReferencePackageOrmValidationTest {
    @Test
    public void sourceIdentityAndBoundaryMappingsLoadWithoutDatabase() {
        var configuration = new Configuration();
        for (var type : java.util.List.of(MicroOrganism.class, MicroAntibiotic.class, MicroBreakpointStandard.class,
                MicroBreakpointRule.class))
            configuration.addAnnotatedClass(type);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        configuration.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        try (var factory = configuration.buildSessionFactory()) {
            var model = factory.getMetamodel();
            assertEquals(4, model.getEntities().size());
            assertEquals(String.class, model.entity(MicroOrganism.class).getAttribute("referenceCode").getJavaType());
            assertEquals(String.class,
                    model.entity(MicroAntibiotic.class).getAttribute("referenceSource").getJavaType());
            assertEquals(String.class,
                    model.entity(MicroBreakpointStandard.class).getAttribute("configurationHash").getJavaType());
            assertEquals(boolean.class,
                    model.entity(MicroBreakpointRule.class).getAttribute("resistantInclusive").getJavaType());
        }
    }
}
