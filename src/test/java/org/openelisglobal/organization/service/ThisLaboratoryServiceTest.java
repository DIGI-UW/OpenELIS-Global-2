package org.openelisglobal.organization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * This laboratory's organization record is found by the id the system stored,
 * so renaming the record (its short name included) does not lose it.
 */
public class ThisLaboratoryServiceTest {

    private ThisLaboratoryServiceImpl service;
    private OrganizationService organizationService;
    private SiteInformationService siteInformationService;
    private SiteInformation setting;

    @Before
    public void setUp() {
        service = new ThisLaboratoryServiceImpl();
        organizationService = mock(OrganizationService.class);
        siteInformationService = mock(SiteInformationService.class);
        ReflectionTestUtils.setField(service, "organizationService", organizationService);
        ReflectionTestUtils.setField(service, "siteInformationService", siteInformationService);
        setting = new SiteInformation();
        setting.setName(ThisLaboratoryService.SETTING);
        setting.setValue("");
        when(siteInformationService.getSiteInformationByName(ThisLaboratoryService.SETTING)).thenReturn(setting);
    }

    private Organization organization(String id, String shortName) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setShortName(shortName);
        return organization;
    }

    @Test
    public void theRecordIsFoundByTheStoredIdAfterItsShortNameWasChanged() {
        Organization renamed = organization("2", "UNRL");
        setting.setValue("2");
        when(organizationService.getOrganizationById("2")).thenReturn(renamed);

        assertSame(renamed, service.find());
        assertTrue(service.isThisLaboratory(organization("2", "anything")));
        assertFalse(service.isThisLaboratory(organization("3", ThisLaboratoryService.FACILITY_ORG_SHORT_NAME)));
    }

    @Test
    public void beforeTheIdIsStoredTheSystemShortNameFindsIt() {
        Organization facility = organization("7", ThisLaboratoryService.FACILITY_ORG_SHORT_NAME);
        when(organizationService.getOrganizationByShortName(ThisLaboratoryService.FACILITY_ORG_SHORT_NAME, false))
                .thenReturn(facility);

        assertSame(facility, service.find());
    }

    @Test
    public void recordingStoresTheIdOnceAndLeavesItWhenUnchanged() {
        service.record(organization("7", ThisLaboratoryService.FACILITY_ORG_SHORT_NAME));
        assertEquals("7", setting.getValue());
        verify(siteInformationService).update(setting);

        SiteInformation same = new SiteInformation();
        same.setValue("7");
        when(siteInformationService.getSiteInformationByName(ThisLaboratoryService.SETTING)).thenReturn(same);
        service.record(organization("7", "renamed"));
        verify(siteInformationService, never()).update(same);
    }

    @Test
    public void withoutTheSettingRowNothingIsWritten() {
        when(siteInformationService.getSiteInformationByName(ThisLaboratoryService.SETTING)).thenReturn(null);
        service.record(organization("7", ThisLaboratoryService.FACILITY_ORG_SHORT_NAME));
        verify(siteInformationService, never()).update(any());
    }
}
