package org.openelisglobal.address.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.test.util.ReflectionTestUtils;

public class AddressHierarchyConfigurationHandlerMetadataTest {

    private DisplayListService previousDisplayListService;
    private DisplayListService displayListService;
    private OrganizationTypeService organizationTypeService;
    private SiteInformationService siteInformationService;
    private AddressHierarchyConfigurationHandler handler;

    @Before
    public void setUp() {
        previousDisplayListService = (DisplayListService) ReflectionTestUtils.getField(DisplayListService.class,
                "instance");
        displayListService = mock(DisplayListService.class);
        ReflectionTestUtils.setField(DisplayListService.class, "instance", displayListService);

        organizationTypeService = mock(OrganizationTypeService.class);
        siteInformationService = mock(SiteInformationService.class);

        handler = new AddressHierarchyConfigurationHandler();
        ReflectionTestUtils.setField(handler, "organizationTypeService", organizationTypeService);
        ReflectionTestUtils.setField(handler, "siteInformationService", siteInformationService);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(DisplayListService.class, "instance", previousDisplayListService);
    }

    @Test
    public void processConfiguration_persistsDisplaySortInputAndBindMetadata() throws Exception {
        when(organizationTypeService.getOrganizationTypeByName("Fokontany")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("14");
        when(siteInformationService.getSiteInformationByName(any())).thenReturn(null);

        String csv = String.join("\n",
                "level,typeName,displayKey,sortOrder,defaultValue,inputType,bindKey",
                "4,Fokontany,patient.address.fokontany,2,,freetext,addressHierarchy_3");

        handler.processConfiguration(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                "madagascar-levels.csv");

        ArgumentCaptor<SiteInformation> captor = ArgumentCaptor.forClass(SiteInformation.class);
        verify(siteInformationService, org.mockito.Mockito.times(4)).insert(captor.capture());

        List<String> persistedMetadata = captor.getAllValues().stream()
                .map(siteInfo -> siteInfo.getName() + "=" + siteInfo.getValue())
                .collect(Collectors.toList());

        assertEquals(List.of(
                "AddrHierarchyDisplayKey_4=patient.address.fokontany",
                "AddrHierarchySortOrder_4=2",
                "AddrHierarchyInputType_4=freetext",
                "AddrHierarchyBindKey_4=addressHierarchy_3"), persistedMetadata);
    }

    @Test
    public void processConfiguration_updatesExistingMetadataRows() throws Exception {
        OrganizationType existingType = new OrganizationType();
        existingType.setId("15");
        existingType.setName("Hamlet/Lot");
        when(organizationTypeService.getOrganizationTypeByName("Hamlet/Lot")).thenReturn(existingType);

        SiteInformation existingDisplayKey = new SiteInformation();
        existingDisplayKey.setName("AddrHierarchyDisplayKey_5");
        SiteInformation existingSortOrder = new SiteInformation();
        existingSortOrder.setName("AddrHierarchySortOrder_5");
        SiteInformation existingInputType = new SiteInformation();
        existingInputType.setName("AddrHierarchyInputType_5");
        SiteInformation existingBindKey = new SiteInformation();
        existingBindKey.setName("AddrHierarchyBindKey_5");

        when(siteInformationService.getSiteInformationByName("AddrHierarchyDisplayKey_5"))
                .thenReturn(existingDisplayKey);
        when(siteInformationService.getSiteInformationByName("AddrHierarchySortOrder_5")).thenReturn(existingSortOrder);
        when(siteInformationService.getSiteInformationByName("AddrHierarchyInputType_5")).thenReturn(existingInputType);
        when(siteInformationService.getSiteInformationByName("AddrHierarchyBindKey_5")).thenReturn(existingBindKey);

        String csv = String.join("\n", "level,typeName,displayKey,sortOrder,defaultValue,inputType,bindKey",
                "5,Hamlet/Lot,patient.address.hamletOrLot,3,,freetext,addressHierarchy_4");

        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                "madagascar-levels.csv");

        assertEquals("patient.address.hamletOrLot", existingDisplayKey.getValue());
        assertEquals("3", existingSortOrder.getValue());
        assertEquals("freetext", existingInputType.getValue());
        assertEquals("addressHierarchy_4", existingBindKey.getValue());

        verify(siteInformationService).update(existingDisplayKey);
        verify(siteInformationService).update(existingSortOrder);
        verify(siteInformationService).update(existingInputType);
        verify(siteInformationService).update(existingBindKey);
    }
}
