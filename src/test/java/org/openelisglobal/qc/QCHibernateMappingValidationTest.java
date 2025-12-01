package org.openelisglobal.qc;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCCorrectiveAction;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * ORM Validation Test for QC entities (Constitution V.4 requirement).
 *
 * Purpose: Validates Hibernate/JPA mappings without requiring database
 * connection. Catches configuration errors (getter conflicts, property
 * mismatches) in <5 seconds.
 *
 * This test fills the gap between unit tests (pure mocks) and integration tests
 * (full stack).
 */
public class QCHibernateMappingValidationTest {

    @Test
    public void testHibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();

        // Add all QC entities
        config.addAnnotatedClass(QCControlLot.class);
        config.addAnnotatedClass(QCResult.class);
        config.addAnnotatedClass(QCStatistics.class);
        config.addAnnotatedClass(WestgardRuleConfig.class);
        config.addAnnotatedClass(QCRuleViolation.class);
        config.addAnnotatedClass(QCCorrectiveAction.class);
        config.addAnnotatedClass(QCAlert.class);

        // Set dialect for validation (no actual database needed)
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // Build SessionFactory - will fail if mappings are invalid
        SessionFactory sessionFactory = config.buildSessionFactory();

        assertNotNull("All QC entity mappings should load successfully", sessionFactory);

        sessionFactory.close();
    }

    @Test
    public void testNoGetterConflicts() {
        // This test passes if no JavaBean getter conflicts exist
        // Example conflict: both getActive() and isActive() would fail
        Configuration config = new Configuration();
        config.addAnnotatedClass(QCControlLot.class);
        config.addAnnotatedClass(QCResult.class);
        config.addAnnotatedClass(QCStatistics.class);
        config.addAnnotatedClass(WestgardRuleConfig.class);
        config.addAnnotatedClass(QCRuleViolation.class);
        config.addAnnotatedClass(QCCorrectiveAction.class);
        config.addAnnotatedClass(QCAlert.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        SessionFactory sessionFactory = config.buildSessionFactory();
        assertNotNull("No getter conflicts should exist", sessionFactory);
        sessionFactory.close();
    }
}
