package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.*;

/**
 * Database-free relationship/getter mapping check; persistence is covered by
 * the integration suite.
 */
public class MicroCultureOrmValidationTest {
    @Test
    public void cultureAndAllHistoryMappingsBuildTogether() {
        var config = new Configuration();
        config.addAnnotatedClass(MicroCaseInoculation.class);
        config.addAnnotatedClass(MicroCultureReading.class);
        config.addAnnotatedClass(MicroCultureExtension.class);
        config.addAnnotatedClass(MicroCultureProposal.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        config.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        try (var factory = config.buildSessionFactory()) {
            assertEquals(4, factory.getMetamodel().getEntities().size());
            assertEquals(MicroCaseInoculation.class,
                    factory.getMetamodel().entity(MicroCultureReading.class).getAttribute("inoculation").getJavaType());
        }
    }
}
