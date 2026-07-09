package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerPendingCodeDAOImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query query;

    private AnalyzerPendingCodeDAOImpl dao;

    @Before
    public void setUp() {
        dao = new AnalyzerPendingCodeDAOImpl();
        ReflectionTestUtils.setField(dao, "entityManager", entityManager);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
    }

    @Test
    public void testFindByAnalyzerId_BindsNumericParameter() {
        when(session.createQuery(eq("FROM AnalyzerPendingCode a WHERE a.analyzerId = :analyzerId ORDER BY a.lastSeenAt DESC"),
                eq(org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode.class))).thenReturn(query);
        when(query.setParameter("analyzerId", 101)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        dao.findByAnalyzerId(" 101 ");

        verify(query).setParameter("analyzerId", 101);
    }

    @Test
    public void testCountByAnalyzerIdAndStatus_BindsNumericParameter() {
        when(session.createQuery(
                eq("SELECT COUNT(a) FROM AnalyzerPendingCode a WHERE a.analyzerId = :analyzerId AND a.status = :status"),
                eq(Long.class))).thenReturn(query);
        when(query.setParameter("analyzerId", 77)).thenReturn(query);
        when(query.setParameter("status", org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode.Status.PENDING))
                .thenReturn(query);
        when(query.uniqueResult()).thenReturn(3L);

        long count = dao.countByAnalyzerIdAndStatus("77",
                org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode.Status.PENDING);

        assertEquals(3L, count);
        verify(query).setParameter("analyzerId", 77);
    }

    @Test
    public void testFindByAnalyzerId_InvalidNumericString_Throws() {
        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class, () -> dao.findByAnalyzerId("abc"));
        assertEquals(true, ex.getMessage().contains("Invalid analyzer ID format"));
    }
}
