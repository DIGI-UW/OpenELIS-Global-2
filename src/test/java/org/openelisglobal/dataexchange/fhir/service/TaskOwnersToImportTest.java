package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A lab imports the referrals addressed to its own facility, configured or not.
 */
public class TaskOwnersToImportTest {

    private List<String> owners(List<String> configured, String facilityUuid, boolean facilityFails) {
        FhirApiWorkFlowServiceImpl service = new FhirApiWorkFlowServiceImpl();
        FhirConfig config = mock(FhirConfig.class);
        FhirFacilityOrganizationService facility = mock(FhirFacilityOrganizationService.class);
        when(config.getRemoteStoreIdentifier()).thenReturn(configured);
        if (facilityFails) {
            when(facility.getFacilityUuid()).thenThrow(new IllegalStateException("not ready"));
        } else {
            when(facility.getFacilityUuid()).thenReturn(facilityUuid);
        }
        ReflectionTestUtils.setField(service, "fhirConfig", config);
        ReflectionTestUtils.setField(service, "facilityOrganizationService", facility);
        return service.taskOwnersToImport();
    }

    @Test
    public void ownFacilityIsAddedToTheConfiguredOwners() {
        assertEquals(List.of("Practitioner/abc", "Organization/980cef27"),
                owners(List.of("Practitioner/abc"), "980cef27", false));
    }

    @Test
    public void ownFacilityAloneWhenNothingIsConfigured() {
        assertEquals(List.of("Organization/980cef27"), owners(Collections.emptyList(), "980cef27", false));
    }

    @Test
    public void configuredOwnersStillWorkWhenTheFacilityIsNotReady() {
        assertEquals(List.of("Organization/lab-b"), owners(List.of("Organization/lab-b"), null, true));
    }

    @Test
    public void noDuplicateWhenTheFacilityIsAlreadyConfigured() {
        assertEquals(List.of("Organization/980cef27"), owners(List.of("Organization/980cef27"), "980cef27", false));
    }
}
