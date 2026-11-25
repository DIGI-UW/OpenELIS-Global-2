package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;
import org.openelisglobal.program.valueholder.Program;

public class ProgramOrmValidationTest {
    @Test(timeout = 5000)
    public void programVisibilityMappingBuildsWithoutDatabaseAccess() {
        Configuration config = new Configuration();
        for (String mapping : new String[] { "Program", "TestSection", "Organization", "OrganizationType" }) {
            config.addResource("hibernate/hbm/" + mapping + ".hbm.xml");
        }
        config.addAnnotatedClass(Localization.class);
        config.addAnnotatedClass(LocalizationValue.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        config.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        config.setProperty("hibernate.search.enabled", "false");
        var registry = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
        try (SessionFactory factory = config.buildSessionFactory(registry)) {
            assertEquals(boolean.class,
                    factory.getMetamodel().entity(Program.class).getAttribute("showOnMicroCase").getJavaType());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
