package org.openelisglobal.organization.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.locations.LocationsApi.ActiveRequest;
import org.openelisglobal.organization.locations.LocationsApi.ActiveResult;
import org.openelisglobal.organization.locations.LocationsApi.Detail;
import org.openelisglobal.organization.locations.LocationsApi.Row;
import org.openelisglobal.organization.locations.LocationsApi.SaveRequest;
import org.openelisglobal.organization.locations.LocationsApi.SaveResult;
import org.openelisglobal.organization.locations.LocationsService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * OGC-1399: the Locations and Organizations screen saved organizations without
 * writing them to the FHIR store, so the store kept a renamed or deactivated
 * organization, and its site Location, until an order referencing it was saved.
 * Each save and each activation change now writes the organization.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LocationsRestControllerPublishTest {

    @Mock
    private LocationsService locationsService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private FhirTransformService fhirTransformService;
    @Mock
    private org.openelisglobal.common.services.DisplayListService displayListService;

    @InjectMocks
    private LocationsRestController controller;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Organization aceh = new Organization();

    @Before
    public void setUp() {
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(1);
        request.setAttribute(IActionConstants.USER_SESSION_DATA, user);
        aceh.setId("4");
        when(organizationService.get("4")).thenReturn(aceh);
    }

    private SaveResult savedAs(String id) {
        Row row = new Row(id, "ACEH", null, null, null, List.of(), null, null, null, 0, null, null, true, false, false,
                false, null);
        Detail detail = new Detail(row, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, List.of(), List.of(), null, null, null, 0);
        return new SaveResult(detail, List.of());
    }

    private SaveRequest body() {
        return new SaveRequest(null, null, "ACEH", null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void anEditedOrganizationIsWrittenToTheFhirStore() throws Exception {
        when(locationsService.save(any(SaveRequest.class), anyString())).thenReturn(savedAs("4"));

        controller.update("4", body(), request);

        verify(fhirTransformService).transformPersistOrganization(aceh);
    }

    @Test
    public void aNewOrganizationIsWrittenToTheFhirStore() throws Exception {
        when(locationsService.save(any(SaveRequest.class), anyString())).thenReturn(savedAs("4"));

        controller.create(body(), request);

        verify(fhirTransformService).transformPersistOrganization(aceh);
    }

    @Test
    public void everyOrganizationWhoseActiveFlagChangedIsWrittenToTheFhirStore() throws Exception {
        Organization ward = new Organization();
        ward.setId("9102");
        when(organizationService.get("9102")).thenReturn(ward);
        when(locationsService.setActive(any(ActiveRequest.class), anyString()))
                .thenReturn(new ActiveResult(List.of("4", "9102"), List.of("ACEH", "Outpatient Department")));

        controller.setActive(new ActiveRequest(List.of("4", "9102"), false, false), request);

        verify(fhirTransformService).transformPersistOrganization(aceh);
        verify(fhirTransformService).transformPersistOrganization(ward);
    }

    @Test
    public void aStoreThatCannotBeReachedDoesNotFailTheSave() throws Exception {
        when(locationsService.save(any(SaveRequest.class), anyString())).thenReturn(savedAs("4"));
        doThrow(new FhirLocalPersistingException("store down")).when(fhirTransformService)
                .transformPersistOrganization(eq(aceh));

        controller.update("4", body(), request);
    }

    @Test
    public void nothingIsWrittenWhenTheOrganizationCannotBeRead() throws Exception {
        when(locationsService.save(any(SaveRequest.class), anyString())).thenReturn(savedAs("404"));

        controller.update("404", body(), request);

        verify(fhirTransformService, never()).transformPersistOrganization(any());
    }

    /**
     * The reference labs Refer Out offers are a cached list; a save, a new
     * organization and an activation change reload it, so a new or renamed
     * reference lab is offered without a restart.
     */
    @Test
    public void everySaveReloadsTheOrganizationPickLists() throws Exception {
        when(locationsService.save(any(SaveRequest.class), anyString())).thenReturn(savedAs("4"));
        when(locationsService.setActive(any(ActiveRequest.class), anyString()))
                .thenReturn(new ActiveResult(List.of("4"), List.of("ACEH")));

        controller.create(body(), request);
        controller.update("4", body(), request);
        controller.setActive(new ActiveRequest(List.of("4"), true, false), request);

        verify(displayListService, org.mockito.Mockito.times(3)).refreshOrganizationLists();
    }
}
