package org.openelisglobal.labunit;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import org.openelisglobal.labunit.valueholder.LabUnit;
import org.openelisglobal.labunit.valueholder.LabUnitWorkflow;
import org.openelisglobal.labunit.valueholder.LabUnitProgram;
import org.openelisglobal.labunit.valueholder.LabUnitProject;

import static org.junit.Assert.assertNotNull;

/**
 * ORM Validation Test for LabUnit entity mappings.
 * 
 * This test validates that all Hibernate/JPA annotations are correctly configured
 * and can build a SessionFactory without errors. It catches:
 * - Getter/setter conflicts (e.g., getActive() vs isActive())
 * - Property name mismatches
 * - Missing annotations
 * - Invalid relationship mappings
 * 
 * Follows Constitution Principle V.4 - ORM Validation Tests
 */
public class LabUnitMappingValidationTest {

    @Test
    public void testLabUnitMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        
        // Add LabUnit entity to configuration
        config.addAnnotatedClass(LabUnit.class);
        
        // Also test related entities to ensure relationship mappings work
        config.addAnnotatedClass(LabUnitWorkflow.class);
        config.addAnnotatedClass(LabUnitProgram.class);
        config.addAnnotatedClass(LabUnitProject.class);
        
        // Configure basic Hibernate properties for testing
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        config.setProperty("hibernate.connection.url", "jdbc:postgresql://localhost/test");
        config.setProperty("hibernate.connection.username", "test");
        config.setProperty("hibernate.connection.password", "test");
        config.setProperty("hibernate.hbm2ddl.auto", "validate");
        config.setProperty("hibernate.show_sql", "false");
        
        // This should succeed without errors if mappings are correct
        SessionFactory sessionFactory = config.buildSessionFactory();
        
        assertNotNull("SessionFactory should be created successfully", sessionFactory);
        
        sessionFactory.close();
    }
}