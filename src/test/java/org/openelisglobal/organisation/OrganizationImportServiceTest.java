package org.openelisglobal.organisation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IGetPage;
import ca.uhn.fhir.rest.gclient.IGetPageTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.service.OrganizationImportServiceImpl;
import org.openelisglobal.organization.service.OrganizationServiceImpl;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationImportServiceTest {

    @Mock
    private FhirUtil fhirUtil;
    @Mock
    private FhirTransformService fhirTransformService;
    @Mock
    private FhirPersistanceService fhirPersistanceService;
    @Mock
    private OrganizationServiceImpl organizationService;
    @Mock
    private OrganizationTypeService organizationTypeService;
    @Mock
    private IGenericClient iGenericClient;
    @Mock
    private IUntypedQuery<IBaseBundle> untypedQuery;
    @Mock
    private IQuery<IBaseBundle> query;
    @InjectMocks
    private OrganizationImportServiceImpl organizationImportService;

    @Before
    public void setUp() {
        DisplayListService displayListServiceMock = mock(DisplayListService.class);
        ReflectionTestUtils.setField(DisplayListService.class, "instance", displayListServiceMock);
        ReflectionTestUtils.setField(organizationImportService, "facilityFhirStore", "http://fhir.test/store");
        ReflectionTestUtils.setField(organizationImportService, "facilityAuth", "basic");
    }

    @Test
    public void testImportOrganizationList_BasicAuthSuccess() throws FhirGeneralException, IOException {
        Bundle orgBundle = new Bundle();
        when(fhirUtil.getFhirClient(anyString())).thenReturn(iGenericClient);
        when(iGenericClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Organization.class)).thenReturn(query);
        when(untypedQuery.forResource(Location.class)).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn((IQuery) query);
        when(query.execute()).thenReturn(orgBundle);

        organizationImportService.importOrganizationList();

        verify(fhirUtil, times(1)).getFhirClient(anyString());
        verify(organizationService, times(1)).deactivateAllOrganizations();
        verify(fhirPersistanceService, atLeastOnce()).updateFhirResourcesInFhirStore(anyMap());
    }

    @Test
    public void testImportOrganizationList_TokenAuth() throws Exception {
        ReflectionTestUtils.setField(organizationImportService, "facilityAuth", "token");
        ReflectionTestUtils.setField(organizationImportService, "facilityAuthUrl", "http://fhir.test/auth");
        ReflectionTestUtils.setField(organizationImportService, "facilityUserName", "admin_user");
        ReflectionTestUtils.setField(organizationImportService, "facilityPassword", "admin_password");

        when(fhirUtil.getAccessToken(anyString(), anyString(), anyString())).thenReturn("token-5432");
        when(fhirUtil.getFhirClient(anyString(), eq("token-5432"))).thenReturn(iGenericClient);

        Bundle newBundle = new Bundle();

        when(iGenericClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Organization.class)).thenReturn(query);
        when(untypedQuery.forResource(Location.class)).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn((IQuery) query);
        when(query.execute()).thenReturn(newBundle);

        organizationImportService.importOrganizationList();

        verify(fhirUtil, times(1)).getAccessToken("http://fhir.test/auth", "admin_user", "admin_password");
        verify(fhirUtil, times(1)).getFhirClient("http://fhir.test/store", "token-5432");
        verify(organizationService, times(1)).deactivateAllOrganizations();
        verify(fhirPersistanceService, atLeastOnce()).updateFhirResourcesInFhirStore(anyMap());
    }

    @Test
    public void testImportOrganizationList_BlankStoreSkipsImport() throws FhirGeneralException, IOException {
        ReflectionTestUtils.setField(organizationImportService, "facilityFhirStore", "");
        organizationImportService.importOrganizationList();
        verify(fhirUtil, never()).getFhirClient(anyString());
        verify(organizationService, never()).deactivateAllOrganizations();
    }

    @Test
    public void testImportOrganizationList_WithPagination() throws FhirGeneralException, IOException {
        Bundle page1Bundle = mock(Bundle.class);
        Bundle page2Bundle = mock(Bundle.class);

        when(fhirUtil.getFhirClient(anyString())).thenReturn(iGenericClient);
        when(iGenericClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Organization.class)).thenReturn(query);
        when(untypedQuery.forResource(Location.class)).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn((IQuery) query);

        Bundle.BundleLinkComponent linkComponent = new Bundle.BundleLinkComponent();
        when(page1Bundle.getLink(IBaseBundle.LINK_NEXT)).thenReturn(linkComponent);
        when(page2Bundle.getLink(IBaseBundle.LINK_NEXT)).thenReturn(null);

        when(query.execute()).thenReturn(page1Bundle);

        IGetPage loadPageMock = mock(IGetPage.class);
        IGetPageTyped<Bundle> orNextMock = mock(IGetPageTyped.class);

        when(iGenericClient.loadPage()).thenReturn(loadPageMock);
        when(loadPageMock.next(page1Bundle)).thenReturn(orNextMock);
        when(orNextMock.execute()).thenReturn(page2Bundle);

        organizationImportService.importOrganizationList();

        verify(fhirUtil, times(1)).getFhirClient(anyString());
        verify(organizationService, times(1)).deactivateAllOrganizations();
        verify(fhirPersistanceService, atLeastOnce()).updateFhirResourcesInFhirStore(anyMap());
    }

    @Test
    public void testImportOrganizationList_WithLocationEntry_UpdatesFhirStore()
            throws FhirGeneralException, IOException {
        Bundle orgBundle = mock(Bundle.class);
        Bundle locBundle = mock(Bundle.class);
        IdType realId = new IdType("loc-123");

        Bundle.BundleEntryComponent entryComponentMock = mock(Bundle.BundleEntryComponent.class);
        Location locationMock = mock(Location.class);

        when(orgBundle.getEntry()).thenReturn(List.of());
        when(locBundle.getEntry()).thenReturn(List.of(entryComponentMock));
        when(entryComponentMock.hasResource()).thenReturn(true);
        when(entryComponentMock.getResource()).thenReturn(locationMock);
        when(locationMock.getIdElement()).thenReturn(realId);
        when(locationMock.getResourceType()).thenReturn(ResourceType.Location);

        when(fhirUtil.getFhirClient(anyString())).thenReturn(iGenericClient);
        when(iGenericClient.search()).thenReturn(untypedQuery);

        IQuery<IBaseBundle> orgQueryMock = mock(IQuery.class);
        IQuery<Bundle> queryMock = mock(IQuery.class);
        when(untypedQuery.forResource(Organization.class)).thenReturn(orgQueryMock);
        when(orgQueryMock.returnBundle(Bundle.class)).thenReturn(queryMock);
        when(queryMock.execute()).thenReturn(orgBundle);

        IQuery<IBaseBundle> locQueryMock = mock(IQuery.class);
        IQuery<Bundle> queryMock2 = mock(IQuery.class);
        when(untypedQuery.forResource(Location.class)).thenReturn(locQueryMock);
        when(locQueryMock.returnBundle(Bundle.class)).thenReturn(queryMock2);
        when(queryMock2.execute()).thenReturn(locBundle);

        organizationImportService.importOrganizationList();

        verify(fhirUtil, times(1)).getFhirClient(anyString());
        verify(organizationService, times(1)).deactivateAllOrganizations();
        verify(fhirPersistanceService, atLeastOnce()).updateFhirResourcesInFhirStore(anyMap());
    }

    @Test
    public void testImportOrganizationList_WithOrganizationEntry_UpdatesFhirStore()
            throws FhirGeneralException, IOException {
        Bundle orgBundle = mock(Bundle.class);

        org.openelisglobal.organization.valueholder.Organization realTransformedOrg = new org.openelisglobal.organization.valueholder.Organization();
        OrganizationType orgType = new OrganizationType();
        orgType.setName("Clinic");
        orgType.setOrganizations(new HashSet<>());
        realTransformedOrg.setOrganizationTypes(Set.of(orgType));

        Bundle.BundleEntryComponent entryComponentMock = mock(Bundle.BundleEntryComponent.class);
        IdType realId = new IdType("org-425");

        Organization organizationMock = mock(Organization.class);

        when(orgBundle.getEntry()).thenReturn(List.of(entryComponentMock));
        when(entryComponentMock.hasResource()).thenReturn(true);
        when(entryComponentMock.getResource()).thenReturn(organizationMock);
        when(organizationMock.getIdElement()).thenReturn(realId);
        when(organizationMock.getResourceType()).thenReturn(ResourceType.Organization);

        when(fhirUtil.getFhirClient(anyString())).thenReturn(iGenericClient);
        when(iGenericClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Organization.class)).thenReturn(query);
        when(untypedQuery.forResource(Location.class)).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn((IQuery) query);
        when(query.execute()).thenReturn(orgBundle);

        when(fhirTransformService.transformToOrganization(any(Organization.class))).thenReturn(realTransformedOrg);
        when(organizationService.getOrganizationByFhirId(anyString())).thenReturn(realTransformedOrg);
        when(organizationTypeService.getOrganizationTypeByName(anyString())).thenReturn(orgType);

        organizationImportService.importOrganizationList();

        verify(fhirUtil, times(1)).getFhirClient(anyString());
        verify(organizationService, times(1)).deactivateAllOrganizations();
        verify(fhirPersistanceService, atLeastOnce()).updateFhirResourcesInFhirStore(anyMap());

    }

    @Test
    public void testImportOrganizationList_WithParentOrganization_SetsParentUuid() throws Exception {
        Bundle orgBundle = mock(Bundle.class);

        Bundle.BundleEntryComponent parentEntryMock = mock(Bundle.BundleEntryComponent.class);
        Bundle.BundleEntryComponent childEntryMock = mock(Bundle.BundleEntryComponent.class);

        Organization parentFhirOrg = mock(Organization.class);
        Organization childFhirOrg = mock(Organization.class);
        IdType parentId = new IdType("parent-org-uuid-123");
        IdType childId = new IdType("child-org-425");

        org.openelisglobal.organization.valueholder.Organization realTransformedOrg = new org.openelisglobal.organization.valueholder.Organization();
        OrganizationType orgType = new OrganizationType();
        orgType.setOrganizations(new HashSet<>());
        realTransformedOrg.setOrganizationTypes(Set.of(orgType));

        when(parentEntryMock.hasResource()).thenReturn(true);
        when(parentEntryMock.getResource()).thenReturn(parentFhirOrg);
        when(parentFhirOrg.getIdElement()).thenReturn(parentId);
        when(parentFhirOrg.getResourceType()).thenReturn(ResourceType.Organization);
        when(parentFhirOrg.getPartOf()).thenReturn(null);

        when(childEntryMock.hasResource()).thenReturn(true);
        when(childEntryMock.getResource()).thenReturn(childFhirOrg);
        when(childFhirOrg.getIdElement()).thenReturn(childId);
        when(childFhirOrg.getResourceType()).thenReturn(ResourceType.Organization);

        org.hl7.fhir.r4.model.Reference partOfRefMock = mock(org.hl7.fhir.r4.model.Reference.class);
        IdType partOfIdMock = new IdType("parent-org-uuid-123");
        when(childFhirOrg.getPartOf()).thenReturn(partOfRefMock);
        when(partOfRefMock.getReferenceElement()).thenReturn(partOfIdMock);

        when(orgBundle.getEntry()).thenReturn(List.of(parentEntryMock, childEntryMock));

        org.openelisglobal.organization.valueholder.Organization realParentTransformed = new org.openelisglobal.organization.valueholder.Organization();
        realParentTransformed.setFhirUuid(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        org.openelisglobal.organization.valueholder.Organization realChildTransformed = new org.openelisglobal.organization.valueholder.Organization();
        realChildTransformed.setFhirUuid(UUID.fromString("987e6543-e21b-12d3-a456-426614174fff"));

        orgType.setName("Clinic");
        realParentTransformed.setOrganizationTypes(Set.of(orgType));
        realChildTransformed.setOrganizationTypes(Set.of(orgType));

        when(fhirUtil.getFhirClient(anyString())).thenReturn(iGenericClient);
        when(iGenericClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Organization.class)).thenReturn(query);
        when(untypedQuery.forResource(Location.class)).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn((IQuery) query);
        when(query.execute()).thenReturn(orgBundle);

        when(fhirTransformService.transformToOrganization(any(Organization.class))).thenReturn(realTransformedOrg);
        when(organizationService.getOrganizationByFhirId(anyString())).thenReturn(realTransformedOrg);
        when(organizationTypeService.getOrganizationTypeByName(anyString())).thenReturn(orgType);

        organizationImportService.importOrganizationList();

        verify(fhirUtil, atLeastOnce()).getFhirClient(anyString());
        verify(organizationService, atLeastOnce()).deactivateAllOrganizations();
        verify(fhirPersistanceService, atLeastOnce()).updateFhirResourcesInFhirStore(anyMap());
        verify(fhirUtil, atLeastOnce()).getFhirClient(anyString());
    }
}
