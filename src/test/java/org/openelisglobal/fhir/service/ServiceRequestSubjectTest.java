package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisAnchorService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1399 (IG Known Issue 3): an order without a patient (environmental,
 * vector, or clinical entered without one) was published with no
 * ServiceRequest.subject, which R4 requires. Its subject is now the sampling
 * site's Location when the site stands for an organization, else a contained
 * Group describing what was sampled. A patient's order keeps the Patient.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ServiceRequestSubjectTest {

    private static final String SITE_ORG_UUID = "20e61b21-1399-42cc-b708-1a1194888781";
    private static final String REFERRING_ORG_UUID = "cff456ed-1399-47dc-86ce-0d21de36db91";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private AnalysisAnchorService analysisAnchorService;
    @Mock
    private IStatusService statusService;
    @Mock
    private FhirCommonTransformService common;
    @Mock
    private org.openelisglobal.samplehuman.service.SampleHumanService sampleHumanService;
    @Mock
    private org.openelisglobal.sample.service.SampleService sampleService;
    @Mock
    private org.openelisglobal.observationhistory.service.ObservationHistoryService observationHistoryService;
    @Mock
    private org.openelisglobal.note.service.NoteService noteService;
    @Mock
    private TerminologyTransformService terminologyTransformService;
    @Mock
    private SampleLocationService sampleLocationService;

    @InjectMocks
    private ServiceRequestTransformServiceImpl transformService;

    private Sample sample;
    private SampleItem sampleItem;
    private Analysis analysis;
    private TableIdService tableIdsBefore;

    @Before
    public void setUp() {
        tableIdsBefore = TableIdService.getInstance();
        TableIdService tableIds = new TableIdService();
        tableIds.REFERRING_ORG_TYPE_ID = "1";
        tableIds.REFERRING_ORG_DEPARTMENT_TYPE_ID = "2";
        ReflectionTestUtils.setField(TableIdService.class, "INSTANCE", tableIds);
        sample = new Sample();
        sample.setId("9399");
        sample.setAccessionNumber("DEV01260000000001399");

        sampleItem = new SampleItem();
        sampleItem.setId("399");
        sampleItem.setFhirUuid(UUID.fromString("5b1e0c2d-1399-4f3a-9c8b-7d6e5f4a3b21"));

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("7");
        analysis = new Analysis();
        analysis.setId("1399");
        analysis.setFhirUuid(UUID.fromString("0d6c2a41-1399-4a7e-8f0b-3c5d9e1a2b47"));
        analysis.setStatusId("4");
        analysis.setTest(test);
        analysis.setSampleItem(sampleItem);

        when(analysisAnchorService.resolveSample(analysis)).thenReturn(sample);
        when(electronicOrderService.getElectronicOrdersByExternalId(any())).thenReturn(List.of());
        when(statusService.getStatusID(any(AnalysisStatus.class))).thenReturn("-1");
        when(common.createReferenceFor(any(ResourceType.class), anyString()))
                .thenAnswer(call -> new Reference(call.getArgument(0) + "/" + call.getArgument(1)));
    }

    @After
    public void restoreTableIds() {
        ReflectionTestUtils.setField(TableIdService.class, "INSTANCE", tableIdsBefore);
    }

    private VectorSamplingSite samplingSite(String name, Organization organization) {
        VectorSamplingSite site = new VectorSamplingSite();
        site.setName(name);
        when(sampleLocationService.getSamplingSite(sample, sampleItem)).thenReturn(site);
        when(sampleLocationService.getSamplingSiteOrganization(site)).thenReturn(organization);
        return site;
    }

    private Organization organization(String fhirUuid) {
        Organization organization = new Organization();
        organization.setId("54");
        organization.setFhirUuid(UUID.fromString(fhirUuid));
        return organization;
    }

    @Test
    public void aPatientsOrderKeepsThePatientAsSubject() {
        sample.setDomain("H");
        Patient patient = new Patient();
        patient.setFhirUuid(UUID.fromString("7a8b9c0d-1399-4e5f-a6b7-c8d9e0f1a2b3"));
        when(sampleHumanService.getPatientForSample(sample)).thenReturn(patient);

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals("Patient/7a8b9c0d-1399-4e5f-a6b7-c8d9e0f1a2b3", serviceRequest.getSubject().getReference());
        assertFalse(serviceRequest.hasContained());
    }

    @Test
    public void aVectorOrderIsAboutTheSamplingSitesLocation() {
        sample.setDomain("V");
        samplingSite("Bekasi Riverside", organization(SITE_ORG_UUID));

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals("Location/" + SITE_ORG_UUID, serviceRequest.getSubject().getReference());
        assertFalse(serviceRequest.hasContained());
    }

    @Test
    public void anEnvironmentalOrderWhoseSiteHasNoOrganizationIsAboutAContainedSubstanceGroup() {
        sample.setDomain("E");
        samplingSite("Well 7", null);

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals("#subject", serviceRequest.getSubject().getReference());
        assertEquals(1, serviceRequest.getContained().size());
        Group sampled = (Group) serviceRequest.getContained().get(0);
        assertEquals("subject", sampled.getIdElement().getIdPart());
        assertEquals(GroupType.SUBSTANCE, sampled.getType());
        assertFalse(sampled.getActual());
        assertEquals("Well 7", sampled.getName());
    }

    @Test
    public void aVectorOrderWithNoSamplingSiteIsAboutAContainedAnimalGroup() {
        sample.setDomain("V");

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        Group sampled = (Group) serviceRequest.getContained().get(0);
        assertEquals("#subject", serviceRequest.getSubject().getReference());
        assertEquals(GroupType.ANIMAL, sampled.getType());
        assertNull(sampled.getName());
    }

    @Test
    public void aClinicalOrderEnteredWithoutAPatientIsAboutAContainedPersonGroup() {
        sample.setDomain("H");

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals(GroupType.PERSON, ((Group) serviceRequest.getContained().get(0)).getType());
    }

    @Test
    public void theReferringSiteStaysTheFirstLocationReference() {
        sample.setDomain("E");
        Organization referringSite = organization(REFERRING_ORG_UUID);
        when(sampleService.getOrganizationRequester(sample, TableIdService.getInstance().REFERRING_ORG_TYPE_ID))
                .thenReturn(referringSite);
        samplingSite("Bekasi Riverside", organization(SITE_ORG_UUID));

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals(1, serviceRequest.getLocationReference().size());
        assertEquals("Location/" + REFERRING_ORG_UUID, serviceRequest.getLocationReferenceFirstRep().getReference());
        assertTrue(serviceRequest.getSubject().getReference().startsWith("Location/"));
    }
}
