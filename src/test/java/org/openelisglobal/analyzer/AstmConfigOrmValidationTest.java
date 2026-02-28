package org.openelisglobal.analyzer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;

/**
 * ORM validation for ASTM config entities (T038). Must run in &lt;5s without
 * DB. Constitution V.4.
 */
public class AstmConfigOrmValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(AnalyzerType.class);
        configuration.addAnnotatedClass(Analyzer.class);
        configuration.addAnnotatedClass(AstmAnalyzerConfig.class);
        configuration.addAnnotatedClass(AstmFieldExtractionConfig.class);
        configuration.addAnnotatedClass(AstmQcRule.class);
        configuration.addAnnotatedClass(AstmTestMappingConfig.class);
        configuration.addAnnotatedClass(AstmFlagMapping.class);
        configuration.addAnnotatedClass(AstmPendingCode.class);

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
    public void testAstmConfigMappingsLoadSuccessfully() {
        assertNotNull(sessionFactory.getMetamodel().entity(AstmAnalyzerConfig.class));
        assertNotNull(sessionFactory.getMetamodel().entity(AstmFieldExtractionConfig.class));
        assertNotNull(sessionFactory.getMetamodel().entity(AstmQcRule.class));
        assertNotNull(sessionFactory.getMetamodel().entity(AstmTestMappingConfig.class));
        assertNotNull(sessionFactory.getMetamodel().entity(AstmFlagMapping.class));
        assertNotNull(sessionFactory.getMetamodel().entity(AstmPendingCode.class));
    }

    @Test
    public void testAstmConfigEntitiesHaveNoGetterConflicts() {
        Class<?>[] entities = { AstmAnalyzerConfig.class, AstmFieldExtractionConfig.class, AstmQcRule.class,
                AstmTestMappingConfig.class, AstmFlagMapping.class, AstmPendingCode.class };

        for (Class<?> entityClass : entities) {
            Map<String, Set<String>> getterMap = new HashMap<>();
            for (Method method : entityClass.getMethods()) {
                String methodName = method.getName();
                if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                    String propertyName = methodName.substring(3);
                    getterMap.computeIfAbsent(propertyName, k -> new HashSet<>()).add("get" + propertyName);
                } else if (methodName.startsWith("is") && method.getParameterCount() == 0
                        && method.getReturnType() == boolean.class) {
                    String propertyName = methodName.substring(2);
                    getterMap.computeIfAbsent(propertyName, k -> new HashSet<>()).add("is" + propertyName);
                }
            }
            for (Map.Entry<String, Set<String>> entry : getterMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    fail(entityClass.getSimpleName() + ": Property " + entry.getKey()
                            + " should not have conflicting getters: " + entry.getValue());
                }
            }
        }
    }
}
