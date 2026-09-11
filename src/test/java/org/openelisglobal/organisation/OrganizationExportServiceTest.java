package org.openelisglobal.organisation;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.service.OrganizationExportServiceImpl;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationExportServiceTest {

    @Mock
    private OrganizationService organizationService;
    @Mock
    private FhirTransformService fhirTransformService;
    @Mock
    private FhirPersistanceService fhirPersistanceService;
    @Mock
    private FhirContext fhirContext;
    @Mock
    private IParser fhirParser;
    @InjectMocks
    private OrganizationExportServiceImpl organizationExportService;

    @Test
    public void testExportFhirOrganizationsFromOrganizations_WhenActiveTrue() throws FhirTransformationException {
        // Given
        Organization organizationMock = new Organization();
        organizationMock.setId("1");
        organizationMock.setIsActive("Y");
        organizationMock.setOrganizationName("Global Health Corp");
        organizationMock.setMlsSentinelLabFlag("N");

        org.hl7.fhir.r4.model.Organization fhirOrg = new org.hl7.fhir.r4.model.Organization();
        fhirOrg.setId("783");

        when(organizationService.getActiveOrganizations()).thenReturn(List.of(organizationMock));
        when(fhirTransformService.transformToFhirOrganization(organizationMock)).thenReturn(fhirOrg);
        when(fhirPersistanceService.makeTransactionBundleForCreate(any())).thenReturn(new Bundle());

        when(fhirContext.newJsonParser()).thenReturn(fhirParser);
        when(fhirParser.setPrettyPrint(true)).thenReturn(fhirParser);
        when(fhirParser.encodeResourceToString(any())).thenReturn("{ \"resourceType\": \"Bundle\" }");

        // When
        String jsonResult = organizationExportService.exportFhirOrganizationsFromOrganizations(true);

        // Then
        assertNotNull(jsonResult);
        verify(organizationService, times(1)).getActiveOrganizations();
        verify(fhirTransformService, times(1)).transformToFhirOrganization(organizationMock);
        verify(fhirPersistanceService, times(1)).makeTransactionBundleForCreate(any());
    }

    @Test
    public void testExportFhirOrganizationsFromOrganizations_WhenActiveFalse() throws FhirTransformationException {
        // Given
        Organization organizationMock = new Organization();
        organizationMock.setId("1");
        organizationMock.setIsActive("Y");
        organizationMock.setOrganizationName("Global Health Corp");
        organizationMock.setMlsSentinelLabFlag("N");

        org.hl7.fhir.r4.model.Organization fhirOrg = new org.hl7.fhir.r4.model.Organization();
        fhirOrg.setId("7854353");

        when(organizationService.getAll()).thenReturn(List.of(organizationMock));
        when(fhirTransformService.transformToFhirOrganization(organizationMock)).thenReturn(fhirOrg);
        when(fhirPersistanceService.makeTransactionBundleForCreate(any())).thenReturn(new Bundle());

        when(fhirContext.newJsonParser()).thenReturn(fhirParser);
        when(fhirParser.setPrettyPrint(true)).thenReturn(fhirParser);
        when(fhirParser.encodeResourceToString(any())).thenReturn("{ \"resourceType\": \"Bundle\" }");

        // When
        String jsonResult = organizationExportService.exportFhirOrganizationsFromOrganizations(false);

        // Then
        assertNotNull(jsonResult);
        verify(organizationService, times(1)).getAll();
        verify(fhirTransformService, times(1)).transformToFhirOrganization(organizationMock);
        verify(fhirPersistanceService, times(1)).makeTransactionBundleForCreate(any());
    }

    @Test
    public void testExportFhirOrganizationsFromOrganizations_WithInternetAddress() throws Exception {

        Organization mockOrg = new Organization();
        mockOrg.setId("1");
        mockOrg.setInternetAddress("http://example.com");

        org.hl7.fhir.r4.model.Organization fhirOrg = new org.hl7.fhir.r4.model.Organization();
        fhirOrg.setId("78945");

        when(organizationService.getActiveOrganizations()).thenReturn(List.of(mockOrg));
        when(fhirTransformService.transformToFhirOrganization(mockOrg)).thenReturn(fhirOrg);
        when(fhirTransformService.createReferenceFor(any(Endpoint.class))).thenReturn(new Reference());
        when(fhirPersistanceService.makeTransactionBundleForCreate(any())).thenReturn(new Bundle());

        when(fhirContext.newJsonParser()).thenReturn(fhirParser);
        when(fhirParser.setPrettyPrint(true)).thenReturn(fhirParser);
        when(fhirParser.encodeResourceToString(any())).thenReturn("{ \"resourceType\": \"Bundle\" }");

        String jsonResult = organizationExportService.exportFhirOrganizationsFromOrganizations(true);

        // Then
        assertNotNull(jsonResult);
        verify(fhirTransformService, times(1)).createReferenceFor(any(Endpoint.class));
    }

    @Test
    public void testExportFhirOrganizationsFromOrganizations_WithParentOrg() throws Exception {

        Organization parentOrg = new Organization();
        parentOrg.setId("2");

        Organization mockOrg = new Organization();
        mockOrg.setId("1");
        mockOrg.setOrganization(parentOrg);

        org.hl7.fhir.r4.model.Organization fhirOrg = new org.hl7.fhir.r4.model.Organization();
        fhirOrg.setId("8768");

        org.hl7.fhir.r4.model.Organization parentFhirOrg = new org.hl7.fhir.r4.model.Organization();
        parentFhirOrg.setId("54235");

        when(organizationService.getActiveOrganizations()).thenReturn(List.of(mockOrg));
        when(fhirTransformService.transformToFhirOrganization(any(Organization.class)))
                .thenReturn(new org.hl7.fhir.r4.model.Organization());
        when(fhirTransformService.createReferenceFor(any(org.hl7.fhir.r4.model.Organization.class)))
                .thenReturn(new Reference());
        when(fhirPersistanceService.makeTransactionBundleForCreate(any())).thenReturn(new Bundle());

        when(fhirContext.newJsonParser()).thenReturn(fhirParser);
        when(fhirParser.setPrettyPrint(true)).thenReturn(fhirParser);
        when(fhirParser.encodeResourceToString(any())).thenReturn("{ \"resourceType\": \"Bundle\" }");

        String jsonResult = organizationExportService.exportFhirOrganizationsFromOrganizations(true);

        // Then
        assertNotNull(jsonResult);
        verify(fhirTransformService, times(1)).createReferenceFor(any(org.hl7.fhir.r4.model.Organization.class));
        verify(fhirPersistanceService, times(1)).makeTransactionBundleForCreate(any());
        verify(fhirTransformService, times(1)).createReferenceFor(any(org.hl7.fhir.r4.model.Organization.class));

    }

    @Test
    public void testExportFhirOrganizationsFromOrganizations_DuplicateParentId() throws Exception {
        Organization parentOrg = new Organization();
        parentOrg.setId("parent-99");

        Organization org1 = new Organization();
        org1.setId("1");
        org1.setOrganization(parentOrg);

        Organization org2 = new Organization();
        org2.setId("2");
        org2.setOrganization(parentOrg);

        org.hl7.fhir.r4.model.Organization fhirOrg1 = new org.hl7.fhir.r4.model.Organization();
        fhirOrg1.setId("Organization/1");

        org.hl7.fhir.r4.model.Organization fhirOrg2 = new org.hl7.fhir.r4.model.Organization();
        fhirOrg2.setId("Organization/2");

        org.hl7.fhir.r4.model.Organization parentFhirOrg = new org.hl7.fhir.r4.model.Organization();
        parentFhirOrg.setId("Organization/parent-99");

        when(organizationService.getActiveOrganizations()).thenReturn(List.of(org1, org2));
        when(fhirTransformService.transformToFhirOrganization(org1)).thenReturn(fhirOrg1);
        when(fhirTransformService.transformToFhirOrganization(org2)).thenReturn(fhirOrg2);
        when(fhirTransformService.transformToFhirOrganization(parentOrg)).thenReturn(parentFhirOrg);
        when(fhirTransformService.createReferenceFor(parentFhirOrg)).thenReturn(new Reference());
        when(fhirPersistanceService.makeTransactionBundleForCreate(any())).thenReturn(new Bundle());

        when(fhirContext.newJsonParser()).thenReturn(fhirParser);
        when(fhirParser.setPrettyPrint(true)).thenReturn(fhirParser);
        when(fhirParser.encodeResourceToString(any())).thenReturn("{ \"resourceType\": \"Bundle\" }");

        String jsonResult = organizationExportService.exportFhirOrganizationsFromOrganizations(true);

        assertNotNull(jsonResult);
        verify(fhirTransformService, atLeastOnce()).transformToFhirOrganization(any());
        verify(fhirTransformService, atLeastOnce()).createReferenceFor(any(org.hl7.fhir.r4.model.Organization.class));
        verify(fhirPersistanceService, atLeastOnce()).makeTransactionBundleForCreate(any());
    }
}
