package org.openelisglobal.test.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.test.dao.DepartmentSampleTypeDAO;
import org.openelisglobal.test.valueholder.DepartmentSampleType;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@RunWith(MockitoJUnitRunner.class)
public class DepartmentSampleTypeServiceTest {

    @Mock
    private DepartmentSampleTypeDAO departmentSampleTypeDAO;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @InjectMocks
    private DepartmentSampleTypeServiceImpl service;

    private static final String DEPT_ID = "1";
    private static final String SAMPLE_TYPE_ID_1 = "10";
    private static final String SAMPLE_TYPE_ID_2 = "20";

    private TypeOfSample sampleType1;
    private TypeOfSample sampleType2;
    private DepartmentSampleType association1;
    private DepartmentSampleType association2;

    @Before
    public void setUp() {
        sampleType1 = new TypeOfSample();
        sampleType1.setId(SAMPLE_TYPE_ID_1);
        sampleType1.setDescription("Sputum");
        sampleType1.setActive(true);

        sampleType2 = new TypeOfSample();
        sampleType2.setId(SAMPLE_TYPE_ID_2);
        sampleType2.setDescription("BAL");
        sampleType2.setActive(true);

        association1 = new DepartmentSampleType();
        association1.setId("100");
        association1.setTestSectionId(DEPT_ID);
        association1.setSampleTypeId(SAMPLE_TYPE_ID_1);
        association1.setIsActive(true);
        association1.setSortOrder(0);

        association2 = new DepartmentSampleType();
        association2.setId("101");
        association2.setTestSectionId(DEPT_ID);
        association2.setSampleTypeId(SAMPLE_TYPE_ID_2);
        association2.setIsActive(true);
        association2.setSortOrder(1);
    }

    @Test
    public void testGetSampleTypesForDepartment_ReturnsMatchingSampleTypes() {
        // Arrange
        List<String> sampleTypeIds = Arrays.asList(SAMPLE_TYPE_ID_1, SAMPLE_TYPE_ID_2);

        when(departmentSampleTypeDAO.getSampleTypeIdsForTestSection(DEPT_ID)).thenReturn(sampleTypeIds);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_1)).thenReturn(sampleType1);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_2)).thenReturn(sampleType2);

        // Act
        List<TypeOfSample> result = service.getSampleTypesForDepartment(DEPT_ID);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Sputum", result.get(0).getDescription());
        assertEquals("BAL", result.get(1).getDescription());
    }

    @Test
    public void testGetSampleTypesForDepartment_EmptyList() {
        // Arrange
        when(departmentSampleTypeDAO.getSampleTypeIdsForTestSection(DEPT_ID)).thenReturn(Collections.emptyList());

        // Act
        List<TypeOfSample> result = service.getSampleTypesForDepartment(DEPT_ID);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetActiveSampleTypesForDepartment_FiltersInactive() {
        // Arrange
        sampleType2.setActive(false); // Make second sample type inactive

        List<DepartmentSampleType> associations = Arrays.asList(association1, association2);
        when(departmentSampleTypeDAO.getActiveByTestSectionId(DEPT_ID)).thenReturn(associations);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_1)).thenReturn(sampleType1);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_2)).thenReturn(sampleType2);

        // Act
        List<TypeOfSample> result = service.getActiveSampleTypesForDepartment(DEPT_ID);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Sputum", result.get(0).getDescription());
    }

    @Test
    public void testLinkDepartmentAndSampleType_CreatesNewLink() {
        // Arrange
        when(departmentSampleTypeDAO.getByTestSectionAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1)).thenReturn(null);
        when(departmentSampleTypeDAO.insert(any(DepartmentSampleType.class))).thenReturn("100");

        // Act
        service.linkDepartmentAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1);

        // Assert
        verify(departmentSampleTypeDAO, times(1)).insert(any(DepartmentSampleType.class));
        verify(departmentSampleTypeDAO, never()).update(any(DepartmentSampleType.class));
    }

    @Test
    public void testLinkDepartmentAndSampleType_UpdatesExistingLink() {
        // Arrange
        when(departmentSampleTypeDAO.getByTestSectionAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1))
                .thenReturn(association1);
        when(departmentSampleTypeDAO.update(any(DepartmentSampleType.class))).thenReturn(association1);

        // Act
        service.linkDepartmentAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1, 5);

        // Assert
        verify(departmentSampleTypeDAO, never()).insert(any(DepartmentSampleType.class));
        verify(departmentSampleTypeDAO, times(1)).update(any(DepartmentSampleType.class));
    }

    @Test
    public void testUnlinkDepartmentAndSampleType_DeletesExisting() {
        // Arrange
        when(departmentSampleTypeDAO.getByTestSectionAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1))
                .thenReturn(association1);
        // Mock the get method which is called by delete() in AuditableBaseObjectServiceImpl
        when(departmentSampleTypeDAO.get("100")).thenReturn(Optional.of(association1));

        // Act
        service.unlinkDepartmentAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1);

        // Assert
        verify(departmentSampleTypeDAO, times(1)).delete(association1);
    }

    @Test
    public void testUnlinkDepartmentAndSampleType_NoOpWhenNotExists() {
        // Arrange
        when(departmentSampleTypeDAO.getByTestSectionAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1)).thenReturn(null);

        // Act
        service.unlinkDepartmentAndSampleType(DEPT_ID, SAMPLE_TYPE_ID_1);

        // Assert
        verify(departmentSampleTypeDAO, never()).delete(any(DepartmentSampleType.class));
    }

    @Test
    public void testUpdateSampleTypesForDepartment_ReplacesAllLinks() {
        // Arrange
        List<String> newSampleTypeIds = Arrays.asList(SAMPLE_TYPE_ID_1, SAMPLE_TYPE_ID_2);
        when(departmentSampleTypeDAO.getByTestSectionAndSampleType(anyString(), anyString())).thenReturn(null);
        when(departmentSampleTypeDAO.insert(any(DepartmentSampleType.class))).thenReturn("100");

        // Act
        service.updateSampleTypesForDepartment(DEPT_ID, newSampleTypeIds);

        // Assert
        verify(departmentSampleTypeDAO, times(1)).deleteAllForTestSection(DEPT_ID);
        verify(departmentSampleTypeDAO, times(2)).insert(any(DepartmentSampleType.class));
    }

    @Test
    public void testGetByTestSectionId_DelegatesToDAO() {
        // Arrange
        List<DepartmentSampleType> expected = Arrays.asList(association1, association2);
        when(departmentSampleTypeDAO.getByTestSectionId(DEPT_ID)).thenReturn(expected);

        // Act
        List<DepartmentSampleType> result = service.getByTestSectionId(DEPT_ID);

        // Assert
        assertEquals(2, result.size());
        verify(departmentSampleTypeDAO).getByTestSectionId(DEPT_ID);
    }

    @Test
    public void testGetSampleTypeIdsForDepartment_DelegatesToDAO() {
        // Arrange
        List<String> expected = Arrays.asList(SAMPLE_TYPE_ID_1, SAMPLE_TYPE_ID_2);
        when(departmentSampleTypeDAO.getSampleTypeIdsForTestSection(DEPT_ID)).thenReturn(expected);

        // Act
        List<String> result = service.getSampleTypeIdsForDepartment(DEPT_ID);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(SAMPLE_TYPE_ID_1));
        assertTrue(result.contains(SAMPLE_TYPE_ID_2));
    }

    @Test
    public void testGetBySampleTypeId_DelegatesToDAO() {
        // Arrange
        List<DepartmentSampleType> expected = Arrays.asList(association1);
        when(departmentSampleTypeDAO.getBySampleTypeId(SAMPLE_TYPE_ID_1)).thenReturn(expected);

        // Act
        List<DepartmentSampleType> result = service.getBySampleTypeId(SAMPLE_TYPE_ID_1);

        // Assert
        assertEquals(1, result.size());
        verify(departmentSampleTypeDAO).getBySampleTypeId(SAMPLE_TYPE_ID_1);
    }

    @Test
    public void testGetTestSectionIdsForSampleType_DelegatesToDAO() {
        // Arrange
        List<String> expected = Arrays.asList(DEPT_ID);
        when(departmentSampleTypeDAO.getTestSectionIdsForSampleType(SAMPLE_TYPE_ID_1)).thenReturn(expected);

        // Act
        List<String> result = service.getTestSectionIdsForSampleType(SAMPLE_TYPE_ID_1);

        // Assert
        assertEquals(1, result.size());
        assertEquals(DEPT_ID, result.get(0));
    }

    @Test
    public void testDeleteAllLinksForDepartment_DelegatesToDAO() {
        // Act
        service.deleteAllLinksForDepartment(DEPT_ID);

        // Assert
        verify(departmentSampleTypeDAO).deleteAllForTestSection(DEPT_ID);
    }

    @Test
    public void testGetSampleTypesForDepartments_UnionFromMultipleDepts() {
        // Arrange
        String deptId2 = "2";
        when(departmentSampleTypeDAO.getSampleTypeIdsForTestSection(DEPT_ID))
                .thenReturn(Arrays.asList(SAMPLE_TYPE_ID_1));
        when(departmentSampleTypeDAO.getSampleTypeIdsForTestSection(deptId2))
                .thenReturn(Arrays.asList(SAMPLE_TYPE_ID_2));
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_1)).thenReturn(sampleType1);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_2)).thenReturn(sampleType2);

        // Act
        List<TypeOfSample> result = service.getSampleTypesForDepartments(Arrays.asList(DEPT_ID, deptId2));

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    public void testGetSampleTypesForDepartments_EmptyInput() {
        // Act
        List<TypeOfSample> result = service.getSampleTypesForDepartments(Collections.emptyList());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSampleTypesForDepartments_NullInput() {
        // Act
        List<TypeOfSample> result = service.getSampleTypesForDepartments(null);

        // Assert
        assertTrue(result.isEmpty());
    }
}
