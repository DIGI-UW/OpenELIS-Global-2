package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
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
import org.openelisglobal.analyzer.valueholder.CustomFieldType;

/**
 * DAO tests for CustomFieldTypeDAO
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomFieldTypeDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<CustomFieldType> query;

    @InjectMocks
    private CustomFieldTypeDAOImpl customFieldTypeDAO;

    private CustomFieldType activeType1;
    private CustomFieldType activeType2;
    private CustomFieldType inactiveType;

    @Before
    public void setUp() {
        // Setup active custom field types
        activeType1 = new CustomFieldType();
        activeType1.setId("TYPE-001");
        activeType1.setTypeName("CUSTOM_NUMERIC");
        activeType1.setDisplayName("Custom Numeric Field");
        activeType1.setIsActive(true);

        activeType2 = new CustomFieldType();
        activeType2.setId("TYPE-002");
        activeType2.setTypeName("CUSTOM_TEXT");
        activeType2.setDisplayName("Custom Text Field");
        activeType2.setIsActive(true);

        // Setup inactive custom field type
        inactiveType = new CustomFieldType();
        inactiveType.setId("TYPE-003");
        inactiveType.setTypeName("CUSTOM_DEPRECATED");
        inactiveType.setDisplayName("Deprecated Field");
        inactiveType.setIsActive(false);
    }

    /**
     * Test: Find all active returns only active types
     */
    @Test
    public void testFindAllActive_ReturnsOnlyActiveTypes() {
        // Arrange
        List<CustomFieldType> expectedActiveTypes = new ArrayList<>();
        expectedActiveTypes.add(activeType1);
        expectedActiveTypes.add(activeType2);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(CustomFieldType.class))).thenReturn(query);
        when(query.list()).thenReturn(expectedActiveTypes);

        // Act
        List<CustomFieldType> actualTypes = customFieldTypeDAO.findAllActive();

        // Assert
        assertNotNull("Type list should not be null", actualTypes);
        assertEquals("Should return 2 active types", 2, actualTypes.size());
        assertTrue("All types should be active", actualTypes.stream().allMatch(CustomFieldType::getIsActive));
        assertEquals("First type ID should match", "TYPE-001", actualTypes.get(0).getId());
    }

    /**
     * Test: Find by name returns matching type
     */
    @Test
    public void testFindByName_ReturnsMatchingType() {
        // Arrange
        List<CustomFieldType> results = new ArrayList<>();
        results.add(activeType1);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(CustomFieldType.class))).thenReturn(query);
        when(query.setParameter(eq("name"), eq("Custom Numeric Field"))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.list()).thenReturn(results);

        // Act
        CustomFieldType result = customFieldTypeDAO.findByName("Custom Numeric Field");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Type ID should match", "TYPE-001", result.getId());
        assertEquals("Display name should match", "Custom Numeric Field", result.getDisplayName());
    }

    /**
     * Test: Find by name with no match returns null
     */
    @Test
    public void testFindByName_NoMatch_ReturnsNull() {
        // Arrange
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(CustomFieldType.class))).thenReturn(query);
        when(query.setParameter(eq("name"), eq("NonExistent"))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.list()).thenReturn(new ArrayList<>());

        // Act
        CustomFieldType result = customFieldTypeDAO.findByName("NonExistent");

        // Assert
        assertNull("Result should be null", result);
    }

    /**
     * Test: Find by type name returns matching type
     */
    @Test
    public void testFindByTypeName_ReturnsMatchingType() {
        // Arrange
        List<CustomFieldType> results = new ArrayList<>();
        results.add(activeType2);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(CustomFieldType.class))).thenReturn(query);
        when(query.setParameter(eq("typeName"), eq("CUSTOM_TEXT"))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.list()).thenReturn(results);

        // Act
        CustomFieldType result = customFieldTypeDAO.findByTypeName("CUSTOM_TEXT");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Type ID should match", "TYPE-002", result.getId());
        assertEquals("Type name should match", "CUSTOM_TEXT", result.getTypeName());
    }

    /**
     * Test: Find by type name with no match returns null
     */
    @Test
    public void testFindByTypeName_NoMatch_ReturnsNull() {
        // Arrange
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(CustomFieldType.class))).thenReturn(query);
        when(query.setParameter(eq("typeName"), eq("NON_EXISTENT"))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.list()).thenReturn(new ArrayList<>());

        // Act
        CustomFieldType result = customFieldTypeDAO.findByTypeName("NON_EXISTENT");

        // Assert
        assertNull("Result should be null", result);
    }
}
