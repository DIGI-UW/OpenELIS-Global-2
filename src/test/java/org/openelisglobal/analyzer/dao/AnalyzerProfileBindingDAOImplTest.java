package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileBindingDAOImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<AnalyzerProfileBinding> bindingQuery;

    @Mock
    private Query<Long> countQuery;

    @Mock
    private Query<Analyzer> analyzerQuery;

    private AnalyzerProfileBindingDAOImpl dao;

    @Before
    public void setUp() {
        dao = new AnalyzerProfileBindingDAOImpl();
        ReflectionTestUtils.setField(dao, "entityManager", entityManager);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
    }

    @Test
    public void findByProfileIdAndRevisionUsesExactImmutableIdentity() {
        String hql = "FROM AnalyzerProfileBinding b WHERE b.profileId = :profileId "
                + "AND b.profileRevision = :profileRevision";
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        when(session.createQuery(eq(hql), eq(AnalyzerProfileBinding.class))).thenReturn(bindingQuery);
        when(bindingQuery.setParameter("profileId", "site.mock-hematology")).thenReturn(bindingQuery);
        when(bindingQuery.setParameter("profileRevision", 3)).thenReturn(bindingQuery);
        when(bindingQuery.getResultList()).thenReturn(List.of(binding));

        Optional<AnalyzerProfileBinding> result = dao.findByProfileIdAndRevision(" site.mock-hematology ", 3);

        assertSame(binding, result.orElseThrow());
        verify(bindingQuery).setParameter("profileId", "site.mock-hematology");
        verify(bindingQuery).setParameter("profileRevision", 3);
    }

    @Test
    public void countAnalyzersByBindingIdUsesAuthoritativeSiteBindingReference() {
        String hql = "SELECT COUNT(a) FROM Analyzer a WHERE "
                + "a.siteBindingRevision.siteBinding.profileBinding.id = :bindingId";
        when(session.createQuery(eq(hql), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter("bindingId", "41")).thenReturn(countQuery);
        when(countQuery.uniqueResult()).thenReturn(2L);

        assertEquals(2L, dao.countAnalyzersByBindingId("41"));
        verify(countQuery).setParameter("bindingId", "41");
    }

    @Test
    public void findAnalyzersByProfileIdReturnsNamedReferencesAcrossPinnedRevisions() {
        String hql = "SELECT a FROM Analyzer a " + "JOIN a.siteBindingRevision r " + "JOIN r.siteBinding s "
                + "JOIN s.profileBinding b " + "WHERE b.profileId = :profileId " + "ORDER BY LOWER(a.name), a.id";
        Analyzer first = new Analyzer();
        first.setId("17");
        first.setName("GeneXpert - Main Lab");
        Analyzer second = new Analyzer();
        second.setId("22");
        second.setName("GeneXpert - TB Bench");
        when(session.createQuery(eq(hql), eq(Analyzer.class))).thenReturn(analyzerQuery);
        when(analyzerQuery.setParameter("profileId", "shipped.genexpert")).thenReturn(analyzerQuery);
        when(analyzerQuery.getResultList()).thenReturn(List.of(first, second));

        List<Analyzer> result = dao.findAnalyzersByProfileId(" shipped.genexpert ");

        assertEquals(List.of(first, second), result);
        verify(analyzerQuery).setParameter("profileId", "shipped.genexpert");
    }
}
