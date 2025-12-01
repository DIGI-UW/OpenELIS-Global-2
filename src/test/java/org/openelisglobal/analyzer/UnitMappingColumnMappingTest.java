package org.openelisglobal.analyzer;

import static org.junit.Assert.assertArrayEquals;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.springframework.beans.factory.annotation.Autowired;

public class UnitMappingColumnMappingTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void testUnitMappingColumnName() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        // Use getEntityPersister() instead of getMetamodel().entity() for Hibernate 6
        // compatibility
        AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory
                .getEntityPersister(UnitMapping.class.getName());
        String[] columnNames = persister.getPropertyColumnNames("analyzerFieldId");
        assertArrayEquals(new String[] { "analyzer_field_id" }, columnNames);
    }
}
