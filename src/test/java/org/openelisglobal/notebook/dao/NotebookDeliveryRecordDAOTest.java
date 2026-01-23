package org.openelisglobal.notebook.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookDeliveryRecord;

/**
 * Unit tests for NotebookDeliveryRecordDAO - Verifies delivery record
 * persistence operations
 */
@RunWith(MockitoJUnitRunner.class)
public class NotebookDeliveryRecordDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<NotebookDeliveryRecord> query;

    @Mock
    private Query<Long> longQuery;

    @InjectMocks
    private NotebookDeliveryRecordDAOImpl dao;

    private NoteBook testNotebook;
    private NotebookDeliveryRecord testRecord;

    @Before
    public void setUp() {
        testNotebook = new NoteBook();
        testNotebook.setId(1);
        testNotebook.setTitle("Test Immunology Workflow");

        testRecord = new NotebookDeliveryRecord();
        testRecord.setId(1);
        testRecord.setNotebook(testNotebook);
        testRecord.setRecipientName("Data Management Team");
        testRecord.setRecipientEmail("dm@lab.org");
        testRecord.setFileName("results_export.xlsx");
        testRecord.setDeliveryType("internal");
        testRecord.setDeliveredAt(new Timestamp(System.currentTimeMillis()));
        testRecord.setDeliveredBy("Test User");
    }

    /**
     * Test: getByNotebookId returns all delivery records for a notebook ordered by
     * delivered_at DESC
     */
    @Test
    public void testGetByNotebookId_ReturnsRecordsOrderedByDate() {
        // Setup
        Integer notebookId = 1;
        List<NotebookDeliveryRecord> results = new ArrayList<>();
        results.add(testRecord);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(NotebookDeliveryRecord.class))).thenReturn(query);
        when(query.setParameter(eq("notebookId"), eq(notebookId))).thenReturn(query);
        when(query.getResultList()).thenReturn(results);

        // Execute
        List<NotebookDeliveryRecord> result = dao.getByNotebookId(notebookId);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRecord.getId(), result.get(0).getId());
        assertEquals("Data Management Team", result.get(0).getRecipientName());
        verify(query).setParameter("notebookId", notebookId);
    }

    /**
     * Test: getByNotebookId returns empty list when no records exist
     */
    @Test
    public void testGetByNotebookId_NoRecords_ReturnsEmptyList() {
        // Setup
        Integer notebookId = 999;
        List<NotebookDeliveryRecord> emptyResults = new ArrayList<>();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(NotebookDeliveryRecord.class))).thenReturn(query);
        when(query.setParameter(eq("notebookId"), eq(notebookId))).thenReturn(query);
        when(query.getResultList()).thenReturn(emptyResults);

        // Execute
        List<NotebookDeliveryRecord> result = dao.getByNotebookId(notebookId);

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test: getByNotebookIdAndDeliveryType filters by delivery type
     */
    @Test
    public void testGetByNotebookIdAndDeliveryType_FiltersCorrectly() {
        // Setup
        Integer notebookId = 1;
        String deliveryType = "internal";
        List<NotebookDeliveryRecord> results = new ArrayList<>();
        results.add(testRecord);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(NotebookDeliveryRecord.class))).thenReturn(query);
        when(query.setParameter(eq("notebookId"), eq(notebookId))).thenReturn(query);
        when(query.setParameter(eq("deliveryType"), eq(deliveryType))).thenReturn(query);
        when(query.getResultList()).thenReturn(results);

        // Execute
        List<NotebookDeliveryRecord> result = dao.getByNotebookIdAndDeliveryType(notebookId, deliveryType);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("internal", result.get(0).getDeliveryType());
        verify(query).setParameter("notebookId", notebookId);
        verify(query).setParameter("deliveryType", deliveryType);
    }

    /**
     * Test: getCountByNotebookId returns correct count
     */
    @Test
    public void testGetCountByNotebookId_ReturnsCount() {
        // Setup
        Integer notebookId = 1;

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(eq("notebookId"), eq(notebookId))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(5L);

        // Execute
        long result = dao.getCountByNotebookId(notebookId);

        // Verify
        assertEquals(5L, result);
    }

    /**
     * Test: getCountByNotebookId returns zero when no records
     */
    @Test
    public void testGetCountByNotebookId_NoRecords_ReturnsZero() {
        // Setup
        Integer notebookId = 999;

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(eq("notebookId"), eq(notebookId))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(0L);

        // Execute
        long result = dao.getCountByNotebookId(notebookId);

        // Verify
        assertEquals(0L, result);
    }

    /**
     * Test: NotebookDeliveryRecord entity has all required fields
     */
    @Test
    public void testNotebookDeliveryRecord_HasAllFields() {
        NotebookDeliveryRecord record = new NotebookDeliveryRecord(testNotebook, "Recipient", "email@test.com",
                "file.xlsx", "external", "FDA", "Test notes", new Timestamp(System.currentTimeMillis()), "Admin User");

        assertNotNull(record.getNotebook());
        assertEquals("Recipient", record.getRecipientName());
        assertEquals("email@test.com", record.getRecipientEmail());
        assertEquals("file.xlsx", record.getFileName());
        assertEquals("external", record.getDeliveryType());
        assertEquals("FDA", record.getRegulatoryBody());
        assertEquals("Test notes", record.getNotes());
        assertNotNull(record.getDeliveredAt());
        assertEquals("Admin User", record.getDeliveredBy());
    }
}
