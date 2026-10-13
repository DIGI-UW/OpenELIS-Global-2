package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.fhir.service.FhirCommonTransformService;
import org.openelisglobal.fhir.service.OrganizationTransformService;
import org.openelisglobal.fhir.service.ServiceRequestTransformService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;

/**
 * OGC-1399 (IG Known Issue 9): ServiceRequest.locationReference and the subject
 * of a patientless order point at Location/{organization fhir id}, and no such
 * Location was ever written to the FHIR store. Each ServiceRequest now goes to
 * the store with the Organization and site Location behind every Location it
 * references, and saving an organization writes its Location too.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirTransformSitesTest {

    private static final String REQUEST_ID = "0d6c2a41-1399-4a7e-8f0b-3c5d9e1a2b47";
    private static final String REFERRING_SITE = "cff456ed-1399-47dc-86ce-0d21de36db91";
    private static final String SAMPLING_SITE = "20e61b21-1399-42cc-b708-1a1194888781";
    private static final String UNKNOWN_SITE = "00000000-1399-0000-0000-000000000000";

    @Mock
    private AnalysisService analysisService;
    @Mock
    private ServiceRequestTransformService serviceRequestTransformService;
    @Mock
    private IStatusService statusService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private OrganizationTransformService organizationTransformService;
    @Mock
    private FhirCommonTransformService common;
    @Mock
    private FhirPersistanceService fhirPersistanceService;

    @InjectMocks
    private FhirTransformServiceImpl fhirTransformService;

    private final Analysis analysis = new Analysis();

    @Before
    public void setUp() {
        analysis.setId("1399");
        when(analysisService.get("1399")).thenReturn(analysis);
        when(statusService.matches(any(), any(AnalysisStatus.class))).thenReturn(false);
        when(common.setTempIdIfMissing(any(), any())).thenReturn(false);
        knownOrganization(REFERRING_SITE);
        knownOrganization(SAMPLING_SITE);
    }

    private Organization knownOrganization(String fhirId) {
        Organization organization = new Organization();
        organization.setFhirUuid(UUID.fromString(fhirId));
        when(organizationService.getOrganizationByFhirId(fhirId)).thenReturn(organization);
        org.hl7.fhir.r4.model.Organization fhirOrganization = new org.hl7.fhir.r4.model.Organization();
        fhirOrganization.setId(fhirId);
        when(organizationTransformService.transformToFhirOrganization(organization)).thenReturn(fhirOrganization);
        Location site = new Location();
        site.setId(fhirId);
        when(organizationTransformService.transformToFhirLocation(organization)).thenReturn(site);
        return organization;
    }

    private Set<String> storedResources() throws Exception {
        ArgumentCaptor<FhirOperations> operations = ArgumentCaptor.forClass(FhirOperations.class);
        verify(fhirPersistanceService).createUpdateFhirResourcesInFhirStore(operations.capture());
        return operations.getValue().updateResources.keySet();
    }

    private ServiceRequest serviceRequest(String subject, String... locations) {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(REQUEST_ID);
        serviceRequest.setSubject(new Reference(subject));
        for (String location : locations) {
            serviceRequest.addLocationReference(new Reference("Location/" + location));
        }
        when(serviceRequestTransformService.transformToServiceRequest(analysis)).thenReturn(serviceRequest);
        return serviceRequest;
    }

    @Test
    public void aServiceRequestGoesToTheStoreWithTheSitesItPointsAt() throws Exception {
        serviceRequest("Location/" + SAMPLING_SITE, REFERRING_SITE);

        fhirTransformService.transformAnalysisByIds(List.of("1399"));

        assertEquals(Set.of("ServiceRequest/" + REQUEST_ID, "Location/" + REFERRING_SITE,
                "Organization/" + REFERRING_SITE, "Location/" + SAMPLING_SITE, "Organization/" + SAMPLING_SITE),
                storedResources());
    }

    @Test
    public void aPatientSubjectAndAContainedGroupAddNoSite() throws Exception {
        serviceRequest("Patient/7a8b9c0d-1399-4e5f-a6b7-c8d9e0f1a2b3");

        fhirTransformService.transformAnalysisByIds(List.of("1399"));

        assertEquals(Set.of("ServiceRequest/" + REQUEST_ID), storedResources());
    }

    @Test
    public void aLocationThatIsNoOrganizationsSiteIsLeftAlone() throws Exception {
        serviceRequest("#subject", UNKNOWN_SITE);

        fhirTransformService.transformAnalysisByIds(List.of("1399"));

        assertEquals(Set.of("ServiceRequest/" + REQUEST_ID), storedResources());
    }

    @Test
    public void savingAnOrganizationWritesItsSiteLocationToo() throws Exception {
        Organization organization = organizationService.getOrganizationByFhirId(SAMPLING_SITE);

        fhirTransformService.transformPersistOrganization(organization);

        Set<String> stored = storedResources();
        assertTrue(stored.contains("Organization/" + SAMPLING_SITE));
        assertTrue(stored.contains("Location/" + SAMPLING_SITE));
    }
}
