package org.openelisglobal.microbiology.daoimpl;

import static org.junit.Assert.assertNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class MicroWorklistContextHqlValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void worklistContextQueriesCompileAgainstRegisteredMappings() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            assertNotNull(entityManager.createQuery(MicroWorklistContextDAOImpl.SPECIMEN_CONTEXT_HQL, Object[].class));
            assertNotNull(
                    entityManager.createQuery(MicroWorklistContextDAOImpl.LATEST_ACTIVITY_CONTEXT_HQL, Object[].class));
            assertNotNull(
                    entityManager.createQuery(MicroWorklistContextDAOImpl.RECENT_ACTIVITY_CONTEXT_HQL, Object[].class));
            assertNotNull(entityManager.createQuery(MicroWorklistContextDAOImpl.FIRST_INOCULATION_CONTEXT_HQL,
                    Object[].class));
            assertNotNull(
                    entityManager.createQuery(MicroWorklistContextDAOImpl.CULTURE_TIMING_CONTEXT_HQL, Object[].class));
            assertNotNull(entityManager.createQuery(
                    MicroAstRunDAOImpl.REVIEWED_WORKLIST_SELECT_HQL + " order by run.startedAt, run.id",
                    Object[].class));
            assertNotNull(entityManager.createQuery(MicroAstRunDAOImpl.REVIEWED_WORKLIST_COUNT_HQL, Long.class));
        } finally {
            entityManager.close();
        }
    }
}
