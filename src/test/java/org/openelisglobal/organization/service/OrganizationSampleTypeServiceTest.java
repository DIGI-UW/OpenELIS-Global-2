package org.openelisglobal.organization.service;

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
import org.openelisglobal.organization.dao.OrganizationSampleTypeDAO;
import org.openelisglobal.organization.valueholder.OrganizationSampleType;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationSampleTypeServiceTest {

    @Mock
    private OrganizationSampleTypeDAO organizationSampleTypeDAO;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @InjectMocks
    private OrganizationSampleTypeServiceImpl service;

    private static final String ORG_ID = "1";
    private static final String SAMPLE_TYPE_ID_1 = "10";
    private static final String SAMPLE_TYPE_ID_2 = "20";

    private TypeOfSample sampleType1;
    private TypeOfSample sampleType2;
    private OrganizationSampleType association1;
    private OrganizationSampleType association2;

    @Before
    public void setUp() {
        sampleType1 = new TypeOfSample();
        sampleType1.setId(SAMPLE_TYPE_ID_1);
        sampleType1.setDescription("Blood");
        sampleType1.setActive(true);

        sampleType2 = new TypeOfSample();
        sampleType2.setId(SAMPLE_TYPE_ID_2);
        sampleType2.setDescription("Urine");
        sampleType2.setActive(true);

        association1 = new OrganizationSampleType();
        association1.setId("100");
        association1.setOrganizationId(ORG_ID);
        association1.setSampleTypeId(SAMPLE_TYPE_ID_1);
        association1.setIsActive(true);
        association1.setSortOrder(0);

        association2 = new OrganizationSampleType();
        association2.setId("101");
        association2.setOrganizationId(ORG_ID);
        association2.setSampleTypeId(SAMPLE_TYPE_ID_2);
        association2.setIsActive(true);
        association2.setSortOrder(1);
    }

    @Test
    public void testGetSampleTypesForOrganization_ReturnsMatchingSampleTypes() {
        // Arrange
        List<String> sampleTypeIds = Arrays.asList(SAMPLE_TYPE_ID_1, SAMPLE_TYPE_ID_2);

        when(organizationSampleTypeDAO.getSampleTypeIdsForOrganization(ORG_ID)).thenReturn(sampleTypeIds);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_1)).thenReturn(sampleType1);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_2)).thenReturn(sampleType2);

        // Act
        List<TypeOfSample> result = service.getSampleTypesForOrganization(ORG_ID);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Blood", result.get(0).getDescription());
        assertEquals("Urine", result.get(1).getDescription());
    }

    @Test
    public void testGetSampleTypesForOrganization_EmptyList() {
        // Arrange
        when(organizationSampleTypeDAO.getSampleTypeIdsForOrganization(ORG_ID)).thenReturn(Collections.emptyList());

        // Act
        List<TypeOfSample> result = service.getSampleTypesForOrganization(ORG_ID);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetActiveSampleTypesForOrganization_FiltersInactive() {
        // Arrange
        sampleType2.setActive(false); // Make second sample type inactive

        List<OrganizationSampleType> associations = Arrays.asList(association1, association2);
        when(organizationSampleTypeDAO.getActiveByOrganizationId(ORG_ID)).thenReturn(associations);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_1)).thenReturn(sampleType1);
        when(typeOfSampleService.get(SAMPLE_TYPE_ID_2)).thenReturn(sampleType2);

        // Act
        List<TypeOfSample> result = service.getActiveSampleTypesForOrganization(ORG_ID);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Blood", result.get(0).getDescription());
    }

    @Test
    public void testLinkOrganizationAndSampleType_CreatesNewLink() {
        // Arrange
        when(organizationSampleTypeDAO.getByOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1)).thenReturn(null);
        when(organizationSampleTypeDAO.insert(any(OrganizationSampleType.class))).thenReturn("100");

        // Act
        service.linkOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1);

        // Assert
        verify(organizationSampleTypeDAO, times(1)).insert(any(OrganizationSampleType.class));
        verify(organizationSampleTypeDAO, never()).update(any(OrganizationSampleType.class));
    }

    @Test
    public void testLinkOrganizationAndSampleType_UpdatesExistingLink() {
        // Arrange
        when(organizationSampleTypeDAO.getByOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1))
                .thenReturn(association1);
        when(organizationSampleTypeDAO.update(any(OrganizationSampleType.class))).thenReturn(association1);

        // Act
        service.linkOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1, 5);

        // Assert
        verify(organizationSampleTypeDAO, never()).insert(any(OrganizationSampleType.class));
        verify(organizationSampleTypeDAO, times(1)).update(any(OrganizationSampleType.class));
    }

    @Test
    public void testUnlinkOrganizationAndSampleType_DeletesExisting() {
        // Arrange
        when(organizationSampleTypeDAO.getByOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1))
                .thenReturn(association1);
        // Mock the get method which is called by delete() in AuditableBaseObjectServiceImpl
        when(organizationSampleTypeDAO.get("100")).thenReturn(Optional.of(association1));

        // Act
        service.unlinkOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1);

        // Assert
        verify(organizationSampleTypeDAO, times(1)).delete(association1);
    }

    @Test
    public void testUnlinkOrganizationAndSampleType_NoOpWhenNotExists() {
        // Arrange
        when(organizationSampleTypeDAO.getByOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1)).thenReturn(null);

        // Act
        service.unlinkOrganizationAndSampleType(ORG_ID, SAMPLE_TYPE_ID_1);

        // Assert
        verify(organizationSampleTypeDAO, never()).delete(any(OrganizationSampleType.class));
    }

    @Test
    public void testUpdateSampleTypesForOrganization_ReplacesAllLinks() {
        // Arrange
        List<String> newSampleTypeIds = Arrays.asList(SAMPLE_TYPE_ID_1, SAMPLE_TYPE_ID_2);
        when(organizationSampleTypeDAO.getByOrganizationAndSampleType(anyString(), anyString())).thenReturn(null);
        when(organizationSampleTypeDAO.insert(any(OrganizationSampleType.class))).thenReturn("100");

        // Act
        service.updateSampleTypesForOrganization(ORG_ID, newSampleTypeIds);

        // Assert
        verify(organizationSampleTypeDAO, times(1)).deleteAllForOrganization(ORG_ID);
        verify(organizationSampleTypeDAO, times(2)).insert(any(OrganizationSampleType.class));
    }

    @Test
    public void testGetByOrganizationId_DelegatesToDAO() {
        // Arrange
        List<OrganizationSampleType> expected = Arrays.asList(association1, association2);
        when(organizationSampleTypeDAO.getByOrganizationId(ORG_ID)).thenReturn(expected);

        // Act
        List<OrganizationSampleType> result = service.getByOrganizationId(ORG_ID);

        // Assert
        assertEquals(2, result.size());
        verify(organizationSampleTypeDAO).getByOrganizationId(ORG_ID);
    }

    @Test
    public void testGetSampleTypeIdsForOrganization_DelegatesToDAO() {
        // Arrange
        List<String> expected = Arrays.asList(SAMPLE_TYPE_ID_1, SAMPLE_TYPE_ID_2);
        when(organizationSampleTypeDAO.getSampleTypeIdsForOrganization(ORG_ID)).thenReturn(expected);

        // Act
        List<String> result = service.getSampleTypeIdsForOrganization(ORG_ID);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(SAMPLE_TYPE_ID_1));
        assertTrue(result.contains(SAMPLE_TYPE_ID_2));
    }

    @Test
    public void testGetBySampleTypeId_DelegatesToDAO() {
        // Arrange
        List<OrganizationSampleType> expected = Arrays.asList(association1);
        when(organizationSampleTypeDAO.getBySampleTypeId(SAMPLE_TYPE_ID_1)).thenReturn(expected);

        // Act
        List<OrganizationSampleType> result = service.getBySampleTypeId(SAMPLE_TYPE_ID_1);

        // Assert
        assertEquals(1, result.size());
        verify(organizationSampleTypeDAO).getBySampleTypeId(SAMPLE_TYPE_ID_1);
    }

    @Test
    public void testGetOrganizationIdsForSampleType_DelegatesToDAO() {
        // Arrange
        List<String> expected = Arrays.asList(ORG_ID);
        when(organizationSampleTypeDAO.getOrganizationIdsForSampleType(SAMPLE_TYPE_ID_1)).thenReturn(expected);

        // Act
        List<String> result = service.getOrganizationIdsForSampleType(SAMPLE_TYPE_ID_1);

        // Assert
        assertEquals(1, result.size());
        assertEquals(ORG_ID, result.get(0));
    }

    @Test
    public void testDeleteAllLinksForOrganization_DelegatesToDAO() {
        // Act
        service.deleteAllLinksForOrganization(ORG_ID);

        // Assert
        verify(organizationSampleTypeDAO).deleteAllForOrganization(ORG_ID);
    }
}
