package org.openelisglobal.organization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.service.FhirFacilityOrganizationService;
import org.openelisglobal.organization.locations.LocationsApi.ThisLaboratory;
import org.openelisglobal.organization.locations.LocationsService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The system marks the facility record it creates as this laboratory; an
 * administrator renaming the record, short name included, does not unmark it.
 */
public class ThisLaboratoryRenameTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FhirFacilityOrganizationService facilityOrganizationService;

    @Autowired
    private ThisLaboratoryService thisLaboratoryService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private LocationsService locationsService;

    @Test
    public void theFacilityRecordStaysThisLaboratoryAfterItsShortNameIsChanged() {
        facilityOrganizationService.initialize();
        Organization facility = thisLaboratoryService.find();
        assertNotNull(facility);
        assertEquals(facility.getId(),
                siteInformationService.getSiteInformationByName(ThisLaboratoryService.SETTING).getValue());

        String shortName = facility.getShortName();
        facility.setShortName("UNRL");
        facility.setSysUserId("1");
        organizationService.update(facility);
        try {
            assertEquals(facility.getId(), thisLaboratoryService.find().getId());
            ThisLaboratory shown = locationsService.thisLaboratory();
            assertEquals(facility.getId(), shown.id());

            facilityOrganizationService.initialize();
            assertEquals("a restart does not create a second facility record", 0, organizationService.getAll().stream()
                    .filter(o -> ThisLaboratoryService.FACILITY_ORG_SHORT_NAME.equals(o.getShortName())).count());
            assertEquals(facility.getId(), thisLaboratoryService.find().getId());
        } finally {
            Organization restore = organizationService.get(facility.getId());
            restore.setShortName(shortName);
            restore.setSysUserId("1");
            organizationService.update(restore);
        }
    }
}
