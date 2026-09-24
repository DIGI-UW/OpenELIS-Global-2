package org.openelisglobal.reports.vectorsurveillance.manualentry;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntrySubmissionAudit;

/**
 * ORM validation test (Constitution V.4) for the two M3 entities. Builds a
 * SessionFactory from the JPA annotations only — no database connection, runs
 * in &lt; 5 seconds. Catches JPA mapping errors and JavaBean getter/setter
 * conflicts before any integration test needs a DB.
 */
public class ManualEntryOrmValidationTest {

    @Test
    public void manualEntryEntities_hibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(ManualEntryFieldMap.class);
        config.addAnnotatedClass(ManualEntrySubmissionAudit.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        SessionFactory sf = config.buildSessionFactory();

        assertNotNull("Manual Entry JPA mappings (ManualEntryFieldMap + ManualEntrySubmissionAudit)"
                + " should load without errors", sf);

        sf.close();
    }
}
