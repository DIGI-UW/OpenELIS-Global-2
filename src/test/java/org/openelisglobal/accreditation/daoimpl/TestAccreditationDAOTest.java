package org.openelisglobal.accreditation.daoimpl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;

// Use Silent runner to avoid strict stubbing exceptions across varying environments
@RunWith(MockitoJUnitRunner.Silent.class)
public class TestAccreditationDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<TestAccreditation> typedQuery;

    @Mock
    private TypedQuery<Long> countTypedQuery;

    @InjectMocks
    private TestAccreditationDAOImpl testAccreditationDAO;

    private TestAccreditation testAccreditation1;
    private TestAccreditation testAccreditation2;
    private AccreditingBody accreditingBody;
    private org.openelisglobal.test.valueholder.Test mockCoreTest1;
    private org.openelisglobal.test.valueholder.Test mockCoreTest2;

    @Before
    public void setUp() {
        // Set up parent AccreditingBody
        accreditingBody = new AccreditingBody();
        accreditingBody.setId(1L);
        accreditingBody.setCode("ISO15189");
        accreditingBody.setActive(true);

        mockCoreTest1 = mock(org.openelisglobal.test.valueholder.Test.class);
        mockCoreTest2 = mock(org.openelisglobal.test.valueholder.Test.class);

        // Set up TestAccreditation fixtures matching entity properties
        testAccreditation1 = new TestAccreditation();
        testAccreditation1.setId(100L);
        testAccreditation1.setAccreditingBody(accreditingBody);
        testAccreditation1.setTest(mockCoreTest1);
        testAccreditation1.setExpiresOn(LocalDate.now().plusYears(1));
        testAccreditation1.setCreatedOn(LocalDateTime.now());
        testAccreditation1.setUpdatedOn(LocalDateTime.now());

        testAccreditation2 = new TestAccreditation();
        testAccreditation2.setId(101L);
        testAccreditation2.setAccreditingBody(accreditingBody);
        testAccreditation2.setTest(mockCoreTest2);
        testAccreditation2.setExpiresOn(LocalDate.now().plusYears(1));
        testAccreditation2.setCreatedOn(LocalDateTime.now());
        testAccreditation2.setUpdatedOn(LocalDateTime.now());
    }

    @Test
    public void testInsert_ShouldPersistAccreditation() {
        testAccreditationDAO.insert(testAccreditation1);
        verify(entityManager).persist(testAccreditation1);
    }

    @Test
    public void testGet_WithValidId_ReturnsAccreditation() {
        when(entityManager.find(TestAccreditation.class, 100L)).thenReturn(testAccreditation1);

        Optional<TestAccreditation> result = testAccreditationDAO.get(100L);

        assertTrue(result.isPresent());
        assertEquals(Long.valueOf(100L), result.get().getId());
    }

    @Test
    public void testFindByTestId_ShouldReturnAccreditationsForTest() {
        when(entityManager.createQuery(anyString(), eq(TestAccreditation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);

        List<TestAccreditation> list = new ArrayList<>();
        list.add(testAccreditation1);
        when(typedQuery.getResultList()).thenReturn(list);

        List<TestAccreditation> results = testAccreditationDAO.findByTestId(55L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(mockCoreTest1, results.get(0).getTest());
    }

    @Test
    public void testFindByTestAndBody_ShouldReturnSpecificAccreditation() {
        when(entityManager.createQuery(anyString(), eq(TestAccreditation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);

        List<TestAccreditation> list = new ArrayList<>();
        list.add(testAccreditation1);
        when(typedQuery.getResultList()).thenReturn(list);

        TestAccreditation result = testAccreditationDAO.findByTestAndBody(55L, 1L);

        assertNotNull("Result should not be null", result);
        assertEquals(Long.valueOf(100L), result.getId());
        assertEquals(mockCoreTest1, result.getTest());
    }

    @Test
    public void testFindByTestAndBody_ShouldReturnNullWhenNotExists() {
        when(entityManager.createQuery(anyString(), eq(TestAccreditation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(new ArrayList<>());

        TestAccreditation result = testAccreditationDAO.findByTestAndBody(999L, 1L);

        assertNull(result);
    }

    @Test
    public void testExistsByTestAndBody_ShouldReturnTrueWhenExists() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countTypedQuery);
        when(countTypedQuery.setParameter(anyString(), any())).thenReturn(countTypedQuery);
        when(countTypedQuery.getSingleResult()).thenReturn(1L);

        boolean exists = testAccreditationDAO.existsByTestAndBody(55L, 1L);

        assertTrue(exists);
    }

    @Test
    public void testExistsByTestAndBody_ShouldReturnFalseWhenNotExists() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countTypedQuery);
        when(countTypedQuery.setParameter(anyString(), any())).thenReturn(countTypedQuery);
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        boolean exists = testAccreditationDAO.existsByTestAndBody(999L, 1L);

        assertFalse(exists);
    }

    @Test
    public void testUpdate_ShouldModifyAccreditation() {
        testAccreditationDAO.update(testAccreditation1);
        verify(entityManager).merge(testAccreditation1);
    }

    @Test
    public void testDelete_ShouldRemoveAccreditation() {
        testAccreditationDAO.delete(testAccreditation1);
        verify(entityManager).remove(testAccreditation1);
    }

    @Test
    public void testFindByFilters_WithQuery_ShouldSearchInDescriptionAndLocalCode() {
        when(entityManager.createQuery(anyString(), eq(TestAccreditation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);

        List<TestAccreditation> list = new ArrayList<>();
        list.add(testAccreditation1);
        when(typedQuery.getResultList()).thenReturn(list);

        List<TestAccreditation> results = testAccreditationDAO.findByFilters(null, null, null, "test-q");

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(typedQuery).setParameter("q", "%test-q%");
    }
}
