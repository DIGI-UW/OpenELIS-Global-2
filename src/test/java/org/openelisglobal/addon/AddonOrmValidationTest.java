package org.openelisglobal.addon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.addon.valueholder.Addon;
import org.openelisglobal.addon.valueholder.AddonAsset;
import org.openelisglobal.addon.valueholder.AddonSecret;

/**
 * Constitution V.4: the add-on entity mappings build without a database.
 */
public class AddonOrmValidationTest {

    private static final List<Class<?>> ENTITIES = List.of(Addon.class, AddonAsset.class, AddonSecret.class);
    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();
        ENTITIES.forEach(configuration::addAnnotatedClass);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        sessionFactory = configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());
    }

    @AfterClass
    public static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void everyAddonEntityIsMapped() {
        for (Class<?> entity : ENTITIES) {
            assertNotNull(entity.getSimpleName(), sessionFactory.getMetamodel().entity(entity));
        }
    }

    @Test
    public void noEntityHasBothGetXAndIsX() {
        for (Class<?> entity : ENTITIES) {
            Set<String> get = new HashSet<>();
            Set<String> is = new HashSet<>();
            for (Method m : entity.getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().startsWith("get") && !m.getName().equals("getClass")) {
                    get.add(m.getName().substring(3));
                } else if (m.getParameterCount() == 0 && m.getName().startsWith("is")) {
                    is.add(m.getName().substring(2));
                }
            }
            get.retainAll(is);
            assertFalse(entity.getSimpleName() + " has get/is conflicts: " + get, !get.isEmpty());
        }
    }
}
