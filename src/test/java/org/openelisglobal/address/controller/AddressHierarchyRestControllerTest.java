package org.openelisglobal.address.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.test.util.ReflectionTestUtils;

public class AddressHierarchyRestControllerTest {

    private OrganizationService organizationService;
    private OrganizationTypeService organizationTypeService;
    private SiteInformationService siteInformationService;
    private AddressHierarchyRestController controller;

    @Before
    public void setUp() {
        organizationService = mock(OrganizationService.class);
        organizationTypeService = mock(OrganizationTypeService.class);
        siteInformationService = mock(SiteInformationService.class);

        controller = new AddressHierarchyRestController();
        ReflectionTestUtils.setField(controller, "organizationService", organizationService);
        ReflectionTestUtils.setField(controller, "organizationTypeService", organizationTypeService);
        ReflectionTestUtils.setField(controller, "siteInformationService", siteInformationService);
    }

    @Test
    public void getLevels_includesConfiguredDisplaySortInputAndBindMetadata() {
        OrganizationType province = orgType("13", "Province", 1);
        OrganizationType fokontany = orgType("14", "Fokontany", 4);
        when(organizationTypeService.getAllOrganizationTypes()).thenReturn(List.of(fokontany, province));
        when(organizationService.getOrganizationsByTypeName(anyString(), anyString())).thenReturn(List.of());

        when(siteInformationService.getSiteInformationByName("AddrHierarchyDisplayKey_4"))
                .thenReturn(siteInfo("AddrHierarchyDisplayKey_4", "patient.address.fokontany"));
        when(siteInformationService.getSiteInformationByName("AddrHierarchySortOrder_4"))
                .thenReturn(siteInfo("AddrHierarchySortOrder_4", "2"));
        when(siteInformationService.getSiteInformationByName("AddrHierarchyInputType_4"))
                .thenReturn(siteInfo("AddrHierarchyInputType_4", "FreeText"));
        when(siteInformationService.getSiteInformationByName("AddrHierarchyBindKey_4"))
                .thenReturn(siteInfo("AddrHierarchyBindKey_4", "addressHierarchy_3"));

        List<AddressHierarchyRestController.AddressHierarchyLevel> levels = controller.getLevels();

        assertEquals("Province remains first by logical hierarchy level", "Province", levels.get(0).getTypeName());
        AddressHierarchyRestController.AddressHierarchyLevel fokontanyLevel = levels.get(1);
        assertEquals("Fokontany", fokontanyLevel.getTypeName());
        assertEquals("patient.address.fokontany", fokontanyLevel.getDisplayKey());
        assertEquals(Integer.valueOf(2), fokontanyLevel.getSortOrder());
        assertEquals("freetext", fokontanyLevel.getInputType());
        assertEquals("addressHierarchy_3", fokontanyLevel.getBindKey());
    }

    @Test
    public void getLevels_defaultsMetadataWhenRowsAreMissingOrInvalid() {
        OrganizationType province = orgType("13", "Province", 1);
        when(organizationTypeService.getAllOrganizationTypes()).thenReturn(List.of(province));
        when(organizationService.getOrganizationsByTypeName(anyString(), anyString())).thenReturn(List.of());
        when(siteInformationService.getSiteInformationByName("AddrHierarchySortOrder_1"))
                .thenReturn(siteInfo("AddrHierarchySortOrder_1", "not-a-number"));

        AddressHierarchyRestController.AddressHierarchyLevel level = controller.getLevels().get(0);

        assertNull(level.getDisplayKey());
        assertNull(level.getSortOrder());
        assertEquals("dropdown", level.getInputType());
        assertNull(level.getBindKey());
    }

    private static OrganizationType orgType(String id, String name, int level) {
        OrganizationType orgType = new OrganizationType();
        orgType.setId(id);
        orgType.setName(name);
        orgType.setHierarchyLevel(level);
        return orgType;
    }

    private static SiteInformation siteInfo(String name, String value) {
        SiteInformation siteInformation = new SiteInformation();
        siteInformation.setName(name);
        siteInformation.setValue(value);
        return siteInformation;
    }
}
