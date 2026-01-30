package org.openelisglobal.analyzer;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.analyzer.valueholder.UnitMapping;

/**
 * ORM validation test for analyzer entities. Verifies Hibernate mappings load
 * successfully without database. Catches configuration errors like
 * getter/setter conflicts, property mismatches, etc. Per Constitution V.4 - ORM
 * Validation Tests.
 */
public class HibernateMappingValidationTest {

    @Test
    public void testAnalyzerMappingsLoadSuccessfully() {
        Configuration config = new Configuration();

        // Add annotation-based analyzer entities
        config.addAnnotatedClass(UnitMapping.class);
        config.addAnnotatedClass(QualitativeResultMapping.class);

        // Use PostgreSQL dialect to match production
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // Build SessionFactory - will fail if mappings have errors
        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("All analyzer mappings should load without errors", sf);
        sf.close();
    }
}
