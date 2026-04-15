package org.openelisglobal.address.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

@RunWith(MockitoJUnitRunner.class)
public class AddressHierarchyConfigurationHandlerTest {

    @Mock
    private OrganizationTypeService organizationTypeService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private SiteInformationService siteInformationService;

    @Mock
    private PatientIdentityTypeService patientIdentityTypeService;

    @InjectMocks
    private AddressHierarchyConfigurationHandler handler;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        // patientIdentityTypeService returns null by default (types don't exist yet)
        when(patientIdentityTypeService.getNamedIdentityType(anyString())).thenReturn(null);
        when(patientIdentityTypeService.insert(any(PatientIdentityType.class))).thenReturn("1");
        // organizationService returns empty list by default (no coded values loaded)
        when(organizationService.getOrganizationsByTypeName(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
    }

    // --- Domain metadata ---

    @Test
    public void testGetDomainName() {
        assertEquals("address-hierarchy", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testGetFileMatcher() {
        assertEquals("*-levels.csv", handler.getFileMatcher());
    }

    // --- Error cases ---

    @Test
    public void testProcessConfiguration_EmptyFile_Skips() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("".getBytes());
        handler.processConfiguration(inputStream, "test-levels.csv");
        verify(organizationTypeService, never()).insert(any(OrganizationType.class));
    }

    @Test
    public void testProcessConfiguration_MissingLevelColumn_ThrowsException() throws Exception {
        String csv = "typeName,defaultValue\nProvince,\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must have a 'level' column");

        handler.processConfiguration(inputStream, "test-levels.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTypeNameColumn_ThrowsException() throws Exception {
        String csv = "level,defaultValue\n1,\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must have a 'typeName' column");

        handler.processConfiguration(inputStream, "test-levels.csv");
    }

    // --- New org type creation ---

    @Test
    public void testProcessConfiguration_NewLevel_CreatesOrganizationType() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n1,Province,,false\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Province")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        handler.processConfiguration(inputStream, "test-levels.csv");

        ArgumentCaptor<OrganizationType> captor = ArgumentCaptor.forClass(OrganizationType.class);
        verify(organizationTypeService).insert(captor.capture());
        OrganizationType created = captor.getValue();
        assertEquals("Province", created.getName());
        assertEquals(Integer.valueOf(1), created.getHierarchyLevel());
        assertEquals(Boolean.FALSE, created.getAllowFreeText());
    }

    @Test
    public void testProcessConfiguration_NewLevel_AllowFreeText_True() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n3,Village,,true\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Village")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        handler.processConfiguration(inputStream, "test-levels.csv");

        ArgumentCaptor<OrganizationType> captor = ArgumentCaptor.forClass(OrganizationType.class);
        verify(organizationTypeService).insert(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().getAllowFreeText());
    }

    @Test
    public void testProcessConfiguration_AllowFreeTextColumn_Missing_DefaultsFalse() throws Exception {
        // Backward-compatible: old CSV without allowFreeText column
        String csv = "level,typeName,defaultValue\n1,Province,\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Province")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        handler.processConfiguration(inputStream, "test-levels.csv");

        ArgumentCaptor<OrganizationType> captor = ArgumentCaptor.forClass(OrganizationType.class);
        verify(organizationTypeService).insert(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getAllowFreeText());
    }

    // --- Existing org type update ---

    @Test
    public void testProcessConfiguration_ExistingLevel_UpdatesOrganizationType() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n2,District,,false\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        OrganizationType existing = new OrganizationType();
        existing.setId("5");
        existing.setName("District");
        when(organizationTypeService.getOrganizationTypeByName("District")).thenReturn(existing);

        handler.processConfiguration(inputStream, "test-levels.csv");

        verify(organizationTypeService, never()).insert(any(OrganizationType.class));
        verify(organizationTypeService).update(existing);
        assertEquals(Integer.valueOf(2), existing.getHierarchyLevel());
        assertEquals(Boolean.FALSE, existing.getAllowFreeText());
    }

    // --- Identity type registration ---

    @Test
    public void testProcessConfiguration_RegistersIdentityType_ForCodedLevel() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n1,Province,,false\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Province")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        handler.processConfiguration(inputStream, "test-levels.csv");

        // CSV level 1 → 0-based index 0 → ADDRESS_HIERARCHY_0
        verify(patientIdentityTypeService).getNamedIdentityType("ADDRESS_HIERARCHY_0");
        verify(patientIdentityTypeService).insert(any(PatientIdentityType.class));
        verify(patientIdentityTypeService, never()).getNamedIdentityType("ADDRESS_HIERARCHY_0_TEXT");
    }

    @Test
    public void testProcessConfiguration_RegistersIdentityTypes_ForFreeTextLevel() throws Exception {
        // CSV level 4 → 0-based index 3 → ADDRESS_HIERARCHY_3_TEXT only (exclusive)
        String csv = "level,typeName,defaultValue,allowFreeText\n4,Village,,true\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Village")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        handler.processConfiguration(inputStream, "test-levels.csv");

        verify(patientIdentityTypeService).getNamedIdentityType("ADDRESS_HIERARCHY_3_TEXT");
        verify(patientIdentityTypeService, never()).getNamedIdentityType("ADDRESS_HIERARCHY_3");
        verify(patientIdentityTypeService, times(1)).insert(any(PatientIdentityType.class));
    }

    @Test
    public void testProcessConfiguration_SkipsInsert_WhenIdentityTypeAlreadyExists() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n1,Province,,false\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Province")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        PatientIdentityType existingType = new PatientIdentityType();
        existingType.setId("99");
        // CSV level 1 → 0-based index 0 → ADDRESS_HIERARCHY_0
        when(patientIdentityTypeService.getNamedIdentityType("ADDRESS_HIERARCHY_0")).thenReturn(existingType);

        handler.processConfiguration(inputStream, "test-levels.csv");

        verify(patientIdentityTypeService, never()).insert(any(PatientIdentityType.class));
    }

    // --- Multiple levels ---

    @Test
    public void testProcessConfiguration_MultipleRows_AllProcessed() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n" + "1,Province,,false\n" + "2,District,,false\n"
                + "3,Village,,true\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName(anyString())).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("1", "2", "3");

        handler.processConfiguration(inputStream, "test-levels.csv");

        verify(organizationTypeService, times(3)).insert(any(OrganizationType.class));
        // 1 identity type per level — coded levels get ADDRESS_HIERARCHY_N,
        // free text levels get ADDRESS_HIERARCHY_N_TEXT exclusively
        verify(patientIdentityTypeService, times(3)).insert(any(PatientIdentityType.class));
    }

    // --- Edge cases ---

    @Test
    public void testProcessConfiguration_EmptyLinesIgnored() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n" + "1,Province,,false\n" + "\n"
                + "2,District,,false\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName(anyString())).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("1", "2");

        handler.processConfiguration(inputStream, "test-levels.csv");

        verify(organizationTypeService, times(2)).insert(any(OrganizationType.class));
    }

    @Test
    public void testProcessConfiguration_InvalidLevelValue_RowSkipped() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n" + "notanumber,Province,,false\n"
                + "1,District,,false\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("District")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("1");

        handler.processConfiguration(inputStream, "test-levels.csv");

        // Only the valid row processed
        verify(organizationTypeService, times(1)).insert(any(OrganizationType.class));
    }

    // --- allowFreeText + coded values guard ---

    @Test
    public void testProcessConfiguration_AllowFreeText_WithExistingCodedValues_ForcedFalse() throws Exception {
        // If allowFreeText=true but coded Organization records already exist for this
        // level, the flag must be silently forced to false to protect existing
        // CSV level 3 → 0-based index 2 → ADDRESS_HIERARCHY_2_TEXT
        // patients.
        String csv = "level,typeName,defaultValue,allowFreeText\n3,Village,,true\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        Organization existingOrg = new Organization();
        existingOrg.setId("100");
        when(organizationService.getOrganizationsByTypeName(anyString(), anyString()))
                .thenReturn(java.util.List.of(existingOrg));
        when(organizationTypeService.getOrganizationTypeByName("Village")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        handler.processConfiguration(inputStream, "test-levels.csv");

        ArgumentCaptor<OrganizationType> captor = ArgumentCaptor.forClass(OrganizationType.class);
        verify(organizationTypeService).insert(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getAllowFreeText());
        // Forced to coded — ADDRESS_HIERARCHY_2 registered, _TEXT never registered
        verify(patientIdentityTypeService).getNamedIdentityType("ADDRESS_HIERARCHY_2");
        verify(patientIdentityTypeService, never()).getNamedIdentityType("ADDRESS_HIERARCHY_2_TEXT");
    }

    @Test
    public void testProcessConfiguration_AllowFreeText_WithNoCodedValues_Permitted() throws Exception {
        // CSV level 3 → 0-based index 2 → ADDRESS_HIERARCHY_2_TEXT
        String csv = "level,typeName,defaultValue,allowFreeText\n3,Village,,true\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Village")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");

        handler.processConfiguration(inputStream, "test-levels.csv");

        ArgumentCaptor<OrganizationType> captor = ArgumentCaptor.forClass(OrganizationType.class);
        verify(organizationTypeService).insert(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().getAllowFreeText());
        verify(patientIdentityTypeService).getNamedIdentityType("ADDRESS_HIERARCHY_2_TEXT");
    }

    @Test
    public void testProcessConfiguration_DefaultValue_StoredInSiteInformation() throws Exception {
        String csv = "level,typeName,defaultValue,allowFreeText\n1,Province,DKI Jakarta,false\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        when(organizationTypeService.getOrganizationTypeByName("Province")).thenReturn(null);
        when(organizationTypeService.insert(any(OrganizationType.class))).thenReturn("10");
        when(siteInformationService.getSiteInformationByName("AddrHierarchyDefault_1")).thenReturn(null);

        handler.processConfiguration(inputStream, "test-levels.csv");

        ArgumentCaptor<SiteInformation> captor = ArgumentCaptor.forClass(SiteInformation.class);
        verify(siteInformationService).insert(captor.capture());
        assertEquals("AddrHierarchyDefault_1", captor.getValue().getName());
        assertEquals("DKI Jakarta", captor.getValue().getValue());
    }
}
