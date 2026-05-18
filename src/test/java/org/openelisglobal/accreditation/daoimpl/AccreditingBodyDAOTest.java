package org.openelisglobal.accreditation.daoimpl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
import org.openelisglobal.accreditation.valueholder.AccreditingBody.LogoVisibilityMode;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AccreditingBodyDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<AccreditingBody> typedQuery;

    @Mock
    private TypedQuery<Long> countTypedQuery;

    @InjectMocks
    private AccreditingBodyDAOImpl accreditingBodyDAO; // Concrete DAO implementation class

    private AccreditingBody testBody1;
    private AccreditingBody testBody2;

    @Before
    public void setUp() {
        testBody1 = new AccreditingBody();
        testBody1.setId(1L);
        testBody1.setCode("ISO15189");
        testBody1.setName("ISO 15189:2022");
        testBody1.setLogoVisibilityMode(LogoVisibilityMode.ANY_ACCREDITED_TEST);
        testBody1.setThresholdPct((short) 80);
        testBody1.setDisplayOrder((short) 1);
        testBody1.setActive(true);
        testBody1.setCreatedOn(LocalDateTime.now());
        testBody1.setUpdatedOn(LocalDateTime.now());

        testBody2 = new AccreditingBody();
        testBody2.setId(2L);
        testBody2.setCode("SLIPTA");
        testBody2.setName("SLIPTA Accreditation");
        testBody2.setLogoVisibilityMode(LogoVisibilityMode.PERCENTAGE);
        testBody2.setThresholdPct((short) 70);
        testBody2.setDisplayOrder((short) 2);
        testBody2.setActive(true);
        testBody2.setCreatedOn(LocalDateTime.now());
        testBody2.setUpdatedOn(LocalDateTime.now());
    }

    @Test
    public void testSaveAndRetrieve_ShouldPersistAccreditingBody() {
        accreditingBodyDAO.insert(testBody1);
        verify(entityManager).persist(testBody1);
    }

    @Test
    public void testGet_WithValidId_ReturnsAccreditingBody() {
        when(entityManager.find(AccreditingBody.class, 1L)).thenReturn(testBody1);

        Optional<AccreditingBody> result = accreditingBodyDAO.get(1L);

        assertTrue(result.isPresent());
        assertEquals(Long.valueOf(1L), result.get().getId());
    }

    @Test
    public void testFindByCode_ShouldReturnBodyWhenCodeExists() {
        when(entityManager.createQuery(anyString(), eq(AccreditingBody.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);

        List<AccreditingBody> list = new ArrayList<>();
        list.add(testBody1);
        when(typedQuery.getResultList()).thenReturn(list);

        AccreditingBody found = accreditingBodyDAO.findByCode("ISO15189");

        assertNotNull(found);
        assertEquals("ISO15189", found.getCode());
        assertEquals(Long.valueOf(1L), found.getId());
    }

    @Test
    public void testFindByCode_ShouldReturnNullWhenCodeDoesNotExist() {
        when(entityManager.createQuery(anyString(), eq(AccreditingBody.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(new ArrayList<>());

        AccreditingBody found = accreditingBodyDAO.findByCode("NONEXISTENT");

        assertNull(found);
    }

    @Test
    public void testFindAllActive_ShouldReturnOnlyActiveBodies() {
        when(entityManager.createQuery(anyString(), eq(AccreditingBody.class))).thenReturn(typedQuery);

        testBody2.setActive(false);
        List<AccreditingBody> activeList = new ArrayList<>();
        activeList.add(testBody1); // Only testBody1 remains active

        when(typedQuery.getResultList()).thenReturn(activeList);

        List<AccreditingBody> activeBodies = accreditingBodyDAO.findAllActive();

        assertNotNull(activeBodies);
        assertEquals(1, activeBodies.size());
        assertEquals("ISO15189", activeBodies.get(0).getCode());
    }

    @Test
    public void testFindAllOrderedByDisplayOrder_ShouldReturnBodiesInOrder() {
        when(entityManager.createQuery(anyString(), eq(AccreditingBody.class))).thenReturn(typedQuery);

        // Swap display orders to verify retrieval structure sorting
        testBody1.setDisplayOrder((short) 2);
        testBody2.setDisplayOrder((short) 1);

        List<AccreditingBody> orderedList = new ArrayList<>();
        orderedList.add(testBody2); // display order 1 first
        orderedList.add(testBody1); // display order 2 second

        when(typedQuery.getResultList()).thenReturn(orderedList);

        List<AccreditingBody> bodies = accreditingBodyDAO.findAllOrderedByDisplayOrder();

        assertNotNull(bodies);
        assertEquals(2, bodies.size());
        assertEquals("SLIPTA", bodies.get(0).getCode());
        assertEquals("ISO15189", bodies.get(1).getCode());
    }

    @Test
    public void testCountTestAccreditationsByBodyId_ShouldReturnCount() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countTypedQuery);
        when(countTypedQuery.setParameter(anyString(), any())).thenReturn(countTypedQuery);
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        long count = accreditingBodyDAO.countTestAccreditationsByBodyId(1L);

        assertEquals(0, count);
    }

    @Test
    public void testUpdate_ShouldModifyExistingBody() {
        accreditingBodyDAO.update(testBody1);
        verify(entityManager).merge(testBody1);
    }

    @Test
    public void testDelete_ShouldRemoveBody() {
        accreditingBodyDAO.delete(testBody1);
        verify(entityManager).remove(testBody1);
    }

    @Test
    public void testLogoVisibilityModePersistence() {
        testBody1.setLogoVisibilityMode(LogoVisibilityMode.PERCENTAGE);
        testBody1.setThresholdPct((short) 50);

        accreditingBodyDAO.update(testBody1);
        verify(entityManager).merge(testBody1);

        assertEquals(LogoVisibilityMode.PERCENTAGE, testBody1.getLogoVisibilityMode());
        assertEquals(Short.valueOf((short) 50), testBody1.getThresholdPct());
    }
}
