package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertSame;
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
import org.openelisglobal.analyzer.daoimpl.AnalyzerDAOImpl;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerDAOImplTest {

    private static final String WITH_PINNED_PROFILE = "SELECT a FROM Analyzer a " + "LEFT JOIN FETCH a.analyzerType "
            + "LEFT JOIN FETCH a.siteBindingRevision revision " + "LEFT JOIN FETCH revision.siteBinding binding "
            + "LEFT JOIN FETCH binding.profileBinding";

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<Analyzer> query;

    private AnalyzerDAOImpl dao;

    @Before
    public void setUp() {
        dao = new AnalyzerDAOImpl();
        ReflectionTestUtils.setField(dao, "entityManager", entityManager);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
    }

    @Test
    public void findAllWithTypesEagerlyLoadsTheAuthoritativeProfileChain() {
        Analyzer analyzer = new Analyzer();
        when(session.createQuery(eq(WITH_PINNED_PROFILE), eq(Analyzer.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(analyzer));

        List<Analyzer> result = dao.findAllWithTypes();

        assertSame(analyzer, result.get(0));
        verify(session).createQuery(WITH_PINNED_PROFILE, Analyzer.class);
    }

    @Test
    public void findByIdWithTypeEagerlyLoadsTheAuthoritativeProfileChain() {
        Analyzer analyzer = new Analyzer();
        String hql = WITH_PINNED_PROFILE + " WHERE a.id = :id";
        when(session.createQuery(eq(hql), eq(Analyzer.class))).thenReturn(query);
        when(query.setParameter("id", "31")).thenReturn(query);
        when(query.uniqueResult()).thenReturn(analyzer);

        assertSame(analyzer, dao.findByIdWithType("31").orElseThrow());
        verify(query).setParameter("id", "31");
    }
}
