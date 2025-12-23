package org.openelisglobal.panel;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Moved mapping validation to PanelItemServiceTest")
public class HibernateMappingValidationTest {

    @Test
    public void testHibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        // Register only the new mapping resources to validate them in isolation
        config.addResource("hibernate/hbm/PanelLabUnit.hbm.xml");
        config.addResource("hibernate/hbm/PanelImportLog.hbm.xml");

        // Minimal required properties for offline mapping validation
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.default_schema", "clinlims");

        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("Panel-related Hibernate mappings should load without errors", sf);
        sf.close();
    }
}
