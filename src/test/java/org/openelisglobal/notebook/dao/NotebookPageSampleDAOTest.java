package org.openelisglobal.notebook.dao;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Unit tests for NotebookPageSampleDAO - Verifies per-sample-per-page query
 * operations
 */
@RunWith(MockitoJUnitRunner.class)
public class NotebookPageSampleDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<NotebookPageSample> query;

    @Mock
    private NativeQuery<NotebookPageSample> nativeQuery;

    @Mock
    private Query<Object[]> countQuery;

    @Mock
    private Query<Long> longQuery;

    @InjectMocks
    private NotebookPageSampleDAOImpl dao;

    private NotebookPageSample testPageSample;
    private NoteBookPage testPage;
    private SampleItem testSampleItem;

    @Before
    public void setUp() {
        testPage = new NoteBookPage();
        testPage.setId(1);
        testPage.setTitle("Sample Reception");

        testSampleItem = new SampleItem();
        testSampleItem.setId("1000");

        testPageSample = new NotebookPageSample();
        testPageSample.setId(1);
        testPageSample.setNotebookPage(testPage);
        testPageSample.setSampleItemId(testSampleItem.getId());
        testPageSample.setStatus(Status.PENDING);
    }

    /** Test: getByPageId returns all samples for a page (uses native query) */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetByPageId_ReturnsAllSamples() {
        // Setup
        Integer pageId = 1;
        List<NotebookPageSample> results = new ArrayList<>();
        results.add(testPageSample);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createNativeQuery(anyString(), eq(NotebookPageSample.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("pageId"), eq(pageId))).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(results);

        // Execute
        List<NotebookPageSample> result = dao.getByPageId(pageId);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPageSample.getId(), result.get(0).getId());
        verify(nativeQuery).setParameter("pageId", pageId);
    }

    /** Test: getByPageIdAndStatus filters by status (uses native query) */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetByPageIdAndStatus_FiltersCorrectly() {
        // Setup
        Integer pageId = 1;
        Status status = Status.PENDING;
        List<NotebookPageSample> results = new ArrayList<>();
        results.add(testPageSample);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createNativeQuery(anyString(), eq(NotebookPageSample.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("pageId"), eq(pageId))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("status"), eq(status.name()))).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(results);

        // Execute
        List<NotebookPageSample> result = dao.getByPageIdAndStatus(pageId, status);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Status.PENDING, result.get(0).getStatus());
        verify(nativeQuery).setParameter("pageId", pageId);
        verify(nativeQuery).setParameter("status", status.name());
    }

    /** Test: getByPageIdAndSampleItemId returns specific sample */
    @Test
    public void testGetByPageIdAndSampleItemId_ReturnsMatch() {
        // Setup
        Integer pageId = 1;
        Integer sampleItemId = 1000;
        List<NotebookPageSample> results = new ArrayList<>();
        results.add(testPageSample);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(NotebookPageSample.class))).thenReturn(query);
        when(query.setParameter(eq("pageId"), eq(pageId))).thenReturn(query);
        when(query.setParameter(eq("sampleItemId"), anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);

        // Execute
        NotebookPageSample result = dao.getByPageIdAndSampleItemId(pageId, sampleItemId);

        // Verify
        assertNotNull(result);
        assertEquals(testPageSample.getId(), result.getId());
    }

    /** Test: getByPageIdAndSampleItemId returns null when not found */
    @Test
    public void testGetByPageIdAndSampleItemId_NotFound_ReturnsNull() {
        // Setup
        Integer pageId = 1;
        Integer sampleItemId = 9999;
        List<NotebookPageSample> emptyResults = new ArrayList<>();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(NotebookPageSample.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(emptyResults);

        // Execute
        NotebookPageSample result = dao.getByPageIdAndSampleItemId(pageId, sampleItemId);

        // Verify
        assertNull(result);
    }

    /** Test: getStatusCountsByPageId returns correct counts for all statuses */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetStatusCountsByPageId_ReturnsAllStatusCounts() {
        // Setup
        Integer pageId = 1;
        List<Object[]> countResults = new ArrayList<>();
        countResults.add(new Object[] { Status.PENDING, 150L });
        countResults.add(new Object[] { Status.COMPLETED, 50L });

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(Object[].class))).thenReturn(countQuery);
        when(countQuery.setParameter(eq("pageId"), eq(pageId))).thenReturn(countQuery);
        when(countQuery.getResultList()).thenReturn(countResults);

        // Execute
        Map<Status, Long> result = dao.getStatusCountsByPageId(pageId);

        // Verify
        assertNotNull(result);
        assertEquals(Long.valueOf(150L), result.get(Status.PENDING));
        assertEquals(Long.valueOf(50L), result.get(Status.COMPLETED));
        assertEquals(Long.valueOf(0L), result.get(Status.IN_PROGRESS));
        assertEquals(Long.valueOf(0L), result.get(Status.SKIPPED));
    }

    /** Test: bulkUpdateStatus returns zero for empty list */
    @Test
    public void testBulkUpdateStatus_EmptyList_ReturnsZero() {
        // Execute
        int result = dao.bulkUpdateStatus(1, new ArrayList<>(), Status.COMPLETED);

        // Verify
        assertEquals(0, result);
    }

    /** Test: getByPageIdPaginated handles pagination correctly */
    @Test
    public void testGetByPageIdPaginated_AppliesPagination() {
        // Setup
        Integer pageId = 1;
        Status status = Status.PENDING;
        int offset = 0;
        int limit = 50;
        List<NotebookPageSample> results = new ArrayList<>();
        results.add(testPageSample);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(NotebookPageSample.class))).thenReturn(query);
        when(query.setParameter(eq("pageId"), eq(pageId))).thenReturn(query);
        when(query.setParameter(eq("status"), eq(status))).thenReturn(query);
        when(query.setFirstResult(eq(offset))).thenReturn(query);
        when(query.setMaxResults(eq(limit))).thenReturn(query);
        when(query.getResultList()).thenReturn(results);

        // Execute
        List<NotebookPageSample> result = dao.getByPageIdPaginated(pageId, status, offset, limit);

        // Verify
        assertNotNull(result);
        verify(query).setFirstResult(offset);
        verify(query).setMaxResults(limit);
    }

    /** Test: getCountByPageId returns correct count */
    @Test
    public void testGetCountByPageId_ReturnsCount() {
        // Setup
        Integer pageId = 1;
        Status status = Status.PENDING;

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(eq("pageId"), eq(pageId))).thenReturn(longQuery);
        when(longQuery.setParameter(eq("status"), eq(status))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(150L);

        // Execute
        long result = dao.getCountByPageId(pageId, status);

        // Verify
        assertEquals(150L, result);
    }
}
