package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class MicroCaseWriteAccessGuardTest {
    private MicroCaseDAO cases;
    private MicrobiologyCaseAccessService access;
    private RecordingTransactions transactions;
    private GuardedServiceImpl service;
    private MicroCase current;

    @Before
    public void setup() {
        cases = mock(MicroCaseDAO.class);
        access = mock(MicrobiologyCaseAccessService.class);
        transactions = new RecordingTransactions();
        current = new MicroCase();
        current.setLabUnitId("original");
        when(cases.getForUpdate("case")).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return current;
        });
        var proxy = new AspectJProxyFactory(new GuardedServiceImpl());
        proxy.addAspect(new MicroCaseWriteAccessGuard(cases, access, transactions));
        service = proxy.getProxy();
        scope(Constants.ROLE_RESULTS);
    }

    private void scope(String role) {
        var request = new MockHttpServletRequest();
        request.setAttribute(MicroCaseWriteAccessGuard.REQUEST_SCOPE,
                new MicroCaseWriteAccessGuard.WriteScope("case", "writer", role));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @After
    public void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void transferBetweenControllerCheckAndServiceEntryDeniesMutation() {
        when(access.hasLabUnitRole("writer", "original", Constants.ROLE_RESULTS)).thenReturn(true);
        assertTrue(access.hasLabUnitRole("writer", "original", Constants.ROLE_RESULTS));
        current.setLabUnitId("destination");
        assertThrows(AccessDeniedException.class, () -> service.write());
        assertEquals(0, service.read());
        assertEquals(1, transactions.rollbacks);
        verify(access).hasLabUnitRole("writer", "destination", Constants.ROLE_RESULTS);
        verify(cases).getForUpdate("case");
    }

    @Test
    public void authorizedMutationRunsInsideTheTransactionHoldingTheLock() {
        when(access.hasLabUnitRole("writer", "original", Constants.ROLE_RESULTS)).thenReturn(true);
        service.write();
        assertEquals(1, service.read());
        assertEquals(1, transactions.commits);
        verify(cases, times(1)).getForUpdate("case");
    }

    @Test
    public void supervisorMutationRequiresCurrentUnitValidationRights() {
        scope(Constants.ROLE_VALIDATION);
        when(access.hasLabUnitRole("writer", "original", Constants.ROLE_RESULTS)).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> service.write());
        assertEquals(0, service.read());
        verify(access).hasLabUnitRole("writer", "original", Constants.ROLE_VALIDATION);
    }

    @Test
    public void readOnlyDirectAccessDoesNotRequireOwningUnitRights() {
        assertEquals(0, service.read());
        verifyZeroInteractions(cases, access);
        assertEquals(0, transactions.commits);
    }

    private static class RecordingTransactions extends AbstractPlatformTransactionManager {
        int commits, rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}

class GuardedServiceImpl {
    private int writes;

    @Transactional
    public void write() {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        writes++;
    }

    @Transactional(readOnly = true)
    public int read() {
        return writes;
    }
}
