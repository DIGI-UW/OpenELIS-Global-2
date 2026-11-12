package org.openelisglobal.referral.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirFacilityOrganizationService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.locations.LocationsApi;
import org.openelisglobal.organization.locations.LocationsServiceImpl;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.referral.controller.rest.ReferralSubcontractDispatchRestController;
import org.openelisglobal.referral.fhir.service.FhirReferralServiceImpl;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Two OpenELIS labs can exchange a referral without database work: the sending
 * lab names itself as the referring laboratory, dispatch does not depend on a
 * handoff time being filled in, and a reference lab's FHIR id can be set to the
 * id the reference lab knows itself by.
 */
public class ReferralAddressingTest {

    private static final String FACILITY = "980cef27-2539-419f-befd-5565d07a3a94";

    @Test
    public void theReferralTaskNamesThisLabAsTheReferringLaboratory() {
        FhirReferralServiceImpl service = new FhirReferralServiceImpl();
        FhirTransformService transform = mock(FhirTransformService.class);
        FhirConfig config = mock(FhirConfig.class);
        FhirFacilityOrganizationService facility = mock(FhirFacilityOrganizationService.class);
        ReflectionTestUtils.setField(service, "fhirTransformService", transform);
        ReflectionTestUtils.setField(service, "fhirConfig", config);
        ReflectionTestUtils.setField(service, "facilityOrganizationService", facility);
        when(config.getOeFhirSystem()).thenReturn("https://fhir.example/oe");
        when(config.getRemoteStoreIdentifier()).thenReturn(Collections.singletonList("Practitioner/*"));
        when(facility.getFacilityUuid()).thenReturn(FACILITY);
        when(transform.createReferenceFor(any(Organization.class))).thenReturn(new Reference("Organization/lab-b"));
        when(transform.createReferenceFor(any(ServiceRequest.class))).thenReturn(new Reference("ServiceRequest/1"));
        when(transform.createReferenceFor(any(Practitioner.class))).thenReturn(new Reference("Practitioner/1"));
        when(transform.createReferenceFor(any(Patient.class))).thenReturn(new Reference("Patient/1"));
        Sample sample = new Sample();
        sample.setAccessionNumber("DEV01260000000000001");

        Task task = service.createReferralTask(new Organization(), new Patient(), new ServiceRequest(),
                Optional.of(new Practitioner()), sample);

        assertEquals("Organization/" + FACILITY, task.getRestriction().getRecipientFirstRep().getReference());
        assertEquals("Organization/lab-b", task.getOwner().getReference());
    }

    @Test
    public void dispatchWithoutAHandoffTimeRecordsNow() {
        ReferralSubcontractDispatchRestController controller = new ReferralSubcontractDispatchRestController();
        ReferralService referralService = mock(ReferralService.class);
        ReflectionTestUtils.setField(controller, "referralService", referralService);
        Referral referral = new Referral();
        referral.setId("41");
        referral.setStatus(org.openelisglobal.referral.valueholder.ReferralStatus.REQUESTED);
        when(referralService.getReferralById("41")).thenReturn(referral);
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(1);
        request.setAttribute(IActionConstants.USER_SESSION_DATA, user);
        long before = System.currentTimeMillis();

        ResponseEntity<Map<String, Object>> response = controller.dispatchReferral("41", null, request);

        assertEquals(200, response.getStatusCode().value());
        ArgumentCaptor<Timestamp> handoff = ArgumentCaptor.forClass(Timestamp.class);
        verify(referralService).dispatchReferral(eq("41"), handoff.capture(), eq("1"), any());
        assertTrue("the handoff time is now", handoff.getValue().getTime() >= before);
        assertNull("a referral without a subcontract record answers without failing",
                response.getBody().get("subcontractId"));
    }

    @Test
    public void aReferenceLabsFhirIdIsValidatedAndSet() {
        LocationsServiceImpl locations = new LocationsServiceImpl();
        OrganizationService organizationService = mock(OrganizationService.class);
        ReflectionTestUtils.setField(locations, "organizationService", organizationService);
        org.openelisglobal.organization.valueholder.Organization lab = new org.openelisglobal.organization.valueholder.Organization();
        lab.setId("3");
        lab.setFhirUuid(UUID.fromString("6ad4712d-0da1-442f-9666-e2ed59db5862"));

        Map<String, String> errors = new HashMap<>();
        ReflectionTestUtils.invokeMethod(locations, "applyReferralFhirId", lab, " " + FACILITY + " ", errors);
        assertTrue(errors.isEmpty());
        assertEquals(FACILITY, lab.getFhirUuidAsString());

        ReflectionTestUtils.invokeMethod(locations, "applyReferralFhirId", lab, "not-a-uuid", errors);
        assertNotNull(errors.get("referral.fhirId"));

        org.openelisglobal.organization.valueholder.Organization other = new org.openelisglobal.organization.valueholder.Organization();
        other.setId("9");
        other.setOrganizationName("Kisenyi Health Centre IV");
        String taken = "11111111-2222-3333-4444-555555555555";
        when(organizationService.getOrganizationByFhirId(taken)).thenReturn(other);
        errors.clear();
        ReflectionTestUtils.invokeMethod(locations, "applyReferralFhirId", lab, taken, errors);
        assertTrue(errors.get("referral.fhirId").contains("Kisenyi Health Centre IV"));
        assertEquals("an id another organization uses is not taken", FACILITY, lab.getFhirUuidAsString());
    }

    @Test
    public void thisLaboratoryIsTheOwnFacilityRecordWithItsFhirId() {
        LocationsServiceImpl locations = new LocationsServiceImpl();
        OrganizationService organizationService = mock(OrganizationService.class);
        ReflectionTestUtils.setField(locations, "organizationService", organizationService);
        assertNull(locations.thisLaboratory());

        org.openelisglobal.organization.valueholder.Organization own = new org.openelisglobal.organization.valueholder.Organization();
        own.setId("2");
        own.setOrganizationName("Uganda National Reference Laboratory");
        own.setShortName(FhirFacilityOrganizationService.FACILITY_ORG_SHORT_NAME);
        own.setFhirUuid(UUID.fromString(FACILITY));
        when(organizationService.getOrganizationByShortName(FhirFacilityOrganizationService.FACILITY_ORG_SHORT_NAME,
                false)).thenReturn(own);

        LocationsApi.ThisLaboratory thisLab = locations.thisLaboratory();
        assertEquals("2", thisLab.id());
        assertEquals("Uganda National Reference Laboratory", thisLab.name());
        assertEquals(FACILITY, thisLab.fhirId());
    }
}
