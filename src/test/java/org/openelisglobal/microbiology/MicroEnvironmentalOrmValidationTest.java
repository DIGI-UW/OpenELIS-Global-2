package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;

public class MicroEnvironmentalOrmValidationTest {
    @Test(timeout = 5000)
    public void siteSubjectsAndCasePurposesMapWithoutDatabase() {
        var config = new Configuration().addAnnotatedClass(MicroCase.class).addAnnotatedClass(VectorSamplingSite.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        config.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        try (var factory = config.buildSessionFactory()) {
            var entity = factory.getMetamodel().entity(MicroCase.class);
            assertEquals(String.class, entity.getAttribute("culturePurpose").getJavaType());
            assertEquals(String.class, entity.getAttribute("siteId").getJavaType());
            assertThrows(IllegalArgumentException.class, () -> entity.getAttribute("workflowType"));
        }
    }
}
