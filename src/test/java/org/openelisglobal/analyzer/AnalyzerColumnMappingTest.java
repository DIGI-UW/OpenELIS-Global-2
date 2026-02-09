package org.openelisglobal.analyzer;

import static org.junit.Assert.assertArrayEquals;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validates that Analyzer entity column mappings match the actual database
 * schema.
 *
 * Prevents regression where JPA @Column annotations don't match database column
 * names.
 *
 * Reference: Similar pattern to UnitMappingColumnMappingTest
 */
public class AnalyzerColumnMappingTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void testAnalyzerScriptIdColumnName() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory
                .getEntityPersister(Analyzer.class.getName());
        String[] columnNames = persister.getPropertyColumnNames("script_id");
        // Database uses legacy typo: scrip_id (missing 't')
        assertArrayEquals("Analyzer.script_id must map to database column 'scrip_id'", new String[] { "scrip_id" },
                columnNames);
    }
}
