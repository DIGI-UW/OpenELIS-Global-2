package org.openelisglobal.userrole;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.userrole.dao.UserRoleDAO;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class UserRoleDAOImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UserRoleDAO userRoleDAO;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void testDeleteLabUnitRoleMap_nonExistentRow_throwsWithCorrectMethodName() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.execute(status -> {
            Session session = entityManager.unwrap(Session.class);
            LabUnitRoleMap proxy = session.load(LabUnitRoleMap.class, 999999);

            try {
                userRoleDAO.deleteLabUnitRoleMap(proxy);
                fail("expected LIMSRuntimeException to be thrown");
            } catch (LIMSRuntimeException e) {
                assertTrue("error message should correctly reference deleteLabUnitRoleMap()",
                        e.getMessage().contains("deleteLabUnitRoleMap"));
            }
            status.setRollbackOnly();
            return null;
        });
    }
}