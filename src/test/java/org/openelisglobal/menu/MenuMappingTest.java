package org.openelisglobal.menu;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.menu.valueholder.Menu;

public class MenuMappingTest {
    @Test
    public void menuAndItsParentBuildWithoutADatabase() {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        configuration.addAnnotatedClass(Menu.class);
        try (SessionFactory factory = configuration.buildSessionFactory()) {
            assertNotNull(factory.getMetamodel().entity(Menu.class).getAttribute("parent"));
            assertNotNull(factory.getMetamodel().entity(Menu.class).getAttribute("icon"));
            assertNotNull(factory.getMetamodel().entity(Menu.class).getAttribute("presentationStyle"));
        }
    }
}
