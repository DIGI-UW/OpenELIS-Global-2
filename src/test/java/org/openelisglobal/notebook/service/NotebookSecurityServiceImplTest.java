package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;

/**
 * Unit tests for NotebookSecurityServiceImpl role checking hierarchy.
 *
 * Role checking priority (from hasRequiredRoleForLabUnit): 1. Global Roles -
 * checked via userRoleService.userInRole(sysUserId, requiredRoles) 2. Lab Unit
 * Roles - AllLabUnits - checked via userLabRoles with labUnit="AllLabUnits" 3.
 * Lab Unit Roles - Specific Units - checked via userLabRoles with specific
 * labUnit ID
 *
 * These tests validate that the role checking hierarchy is correctly
 * implemented.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("unchecked")
public class NotebookSecurityServiceImplTest {

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private NotebookEntryService notebookEntryService;

    @Mock
    private NoteBookPageService noteBookPageService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private NotebookSecurityServiceImpl securityService;

    // Test constants
    private static final String GLOBAL_ADMIN_USER_ID = "10";
    private static final String GLOBAL_TECH_USER_ID = "11";
    private static final String GLOBAL_PATHOLOGIST_USER_ID = "12";
    private static final String ALL_LAB_UNITS_TECH_USER_ID = "13";
    private static final String CYTOLOGY_TECH_USER_ID = "14";
    private static final String HEMATOLOGY_TECH_USER_ID = "15";
    private static final String NO_ROLES_USER_ID = "16";

    private static final String CYTOLOGY_LAB_UNIT = "Cytology";
    private static final String HEMATOLOGY_LAB_UNIT = "Hematology";
    private static final String BIOCHEMISTRY_LAB_UNIT = "Biochemistry";

    private static final String ROLE_TECHNICIAN = "Technician";
    private static final String ROLE_PATHOLOGIST = "Pathologist";

    private static final String TECHNICIAN_ROLE_ID = "2";
    private static final String PATHOLOGIST_ROLE_ID = "4";

    private static final String CYTOLOGY_TEST_SECTION_ID = "100";
    private static final String HEMATOLOGY_TEST_SECTION_ID = "101";

    private NoteBook templateWithAllowedRoles;
    private NoteBook templateWithNoRoleRestrictions;

    @Before
    public void setUp() {
        // Setup template with role restrictions (requires Technician or Pathologist)
        templateWithAllowedRoles = new NoteBook();
        templateWithAllowedRoles.setId(100);
        templateWithAllowedRoles.setIsTemplate(true);
        Set<String> allowedRoles = new HashSet<>();
        allowedRoles.add(ROLE_TECHNICIAN);
        allowedRoles.add(ROLE_PATHOLOGIST);
        templateWithAllowedRoles.setAllowedRoles(allowedRoles);

        // Setup template with no role restrictions
        templateWithNoRoleRestrictions = new NoteBook();
        templateWithNoRoleRestrictions.setId(101);
        templateWithNoRoleRestrictions.setIsTemplate(true);
        templateWithNoRoleRestrictions.setAllowedRoles(new HashSet<>());

        // Setup test sections using mocked objects to avoid SpringContext dependency
        TestSection cytologySection = Mockito.mock(TestSection.class);
        when(cytologySection.getId()).thenReturn(CYTOLOGY_TEST_SECTION_ID);
        when(cytologySection.getTestSectionName()).thenReturn(CYTOLOGY_LAB_UNIT);
        when(cytologySection.getLocalizedName()).thenReturn(CYTOLOGY_LAB_UNIT);

        TestSection hematologySection = Mockito.mock(TestSection.class);
        when(hematologySection.getId()).thenReturn(HEMATOLOGY_TEST_SECTION_ID);
        when(hematologySection.getTestSectionName()).thenReturn(HEMATOLOGY_LAB_UNIT);
        when(hematologySection.getLocalizedName()).thenReturn(HEMATOLOGY_LAB_UNIT);

        when(testSectionService.get(CYTOLOGY_TEST_SECTION_ID)).thenReturn(cytologySection);
        when(testSectionService.get(HEMATOLOGY_TEST_SECTION_ID)).thenReturn(hematologySection);

        // Setup roles
        Role technicianRole = new Role();
        technicianRole.setId(TECHNICIAN_ROLE_ID);
        technicianRole.setName(ROLE_TECHNICIAN);

        Role pathologistRole = new Role();
        pathologistRole.setId(PATHOLOGIST_ROLE_ID);
        pathologistRole.setName(ROLE_PATHOLOGIST);

        when(roleService.getRoleById(TECHNICIAN_ROLE_ID)).thenReturn(technicianRole);
        when(roleService.getRoleById(PATHOLOGIST_ROLE_ID)).thenReturn(pathologistRole);
    }

    // ========== TEST: GLOBAL ADMIN BYPASS ==========

    @Test
    public void canCreateEntry_globalAdmin_alwaysAllowed() {
        // Setup: User is Global Administrator
        when(userRoleService.userInRole(eq(GLOBAL_ADMIN_USER_ID), eq(Constants.ROLE_GLOBAL_ADMIN))).thenReturn(true);

        // Test: Global Admin can create entry regardless of allowed roles
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, GLOBAL_ADMIN_USER_ID, CYTOLOGY_LAB_UNIT);

        assertTrue("Global Admin should always be able to create entries", result);
    }

    // ========== TEST: GLOBAL ROLES (Priority 1) ==========

    @Test
    public void canCreateEntry_userWithGlobalTechnicianRole_allowedWhenTechnicianInAllowedRoles() {
        // Setup: User has global Technician role (not lab-unit specific)
        setupNonAdminUser(GLOBAL_TECH_USER_ID);
        setupCanViewTemplate(GLOBAL_TECH_USER_ID, CYTOLOGY_LAB_UNIT);

        // User has global Technician role
        when(userRoleService.userInRole(eq(GLOBAL_TECH_USER_ID), any(Collection.class))).thenAnswer(invocation -> {
            Collection<String> roles = invocation.getArgument(1);
            return roles.contains(ROLE_TECHNICIAN);
        });

        // Test: User with global Technician role can create entry
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, GLOBAL_TECH_USER_ID,
                CYTOLOGY_LAB_UNIT);

        assertTrue("User with global Technician role should be allowed when Technician is in allowedRoles", result);
    }

    @Test
    public void canCreateEntry_userWithGlobalPathologistRole_allowedWhenPathologistInAllowedRoles() {
        // Setup: User has global Pathologist role
        setupNonAdminUser(GLOBAL_PATHOLOGIST_USER_ID);
        setupCanViewTemplate(GLOBAL_PATHOLOGIST_USER_ID, CYTOLOGY_LAB_UNIT);

        // User has global Pathologist role
        when(userRoleService.userInRole(eq(GLOBAL_PATHOLOGIST_USER_ID), any(Collection.class)))
                .thenAnswer(invocation -> {
                    Collection<String> roles = invocation.getArgument(1);
                    return roles.contains(ROLE_PATHOLOGIST);
                });

        // Test: User with global Pathologist role can create entry
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, GLOBAL_PATHOLOGIST_USER_ID,
                CYTOLOGY_LAB_UNIT);

        assertTrue("User with global Pathologist role should be allowed when Pathologist is in allowedRoles", result);
    }

    // ========== TEST: ALL LAB UNITS ROLES (Priority 2) ==========

    @Test
    public void canCreateEntry_userWithAllLabUnitsTechnicianRole_allowedWhenTechnicianInAllowedRoles() {
        // Setup: User has Technician role for AllLabUnits (not global, but lab-unit
        // level)
        setupNonAdminUser(ALL_LAB_UNITS_TECH_USER_ID);
        setupCanViewTemplate(ALL_LAB_UNITS_TECH_USER_ID, CYTOLOGY_LAB_UNIT);

        // User does NOT have global roles
        when(userRoleService.userInRole(eq(ALL_LAB_UNITS_TECH_USER_ID), any(Collection.class))).thenReturn(false);

        // User has AllLabUnits Technician role
        UserLabUnitRoles labRoles = createUserLabUnitRoles("AllLabUnits", TECHNICIAN_ROLE_ID);
        when(userRoleService.getUserLabUnitRoles(ALL_LAB_UNITS_TECH_USER_ID)).thenReturn(labRoles);

        // Test: User with AllLabUnits Technician role can create entry
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, ALL_LAB_UNITS_TECH_USER_ID,
                CYTOLOGY_LAB_UNIT);

        assertTrue("User with AllLabUnits Technician role should be allowed when Technician is in allowedRoles",
                result);
    }

    @Test
    public void canCreateEntry_userWithAllLabUnitsTechnicianRole_allowedForAnyLabUnit() {
        // Setup: User has Technician role for AllLabUnits
        setupNonAdminUser(ALL_LAB_UNITS_TECH_USER_ID);
        setupCanViewTemplate(ALL_LAB_UNITS_TECH_USER_ID, HEMATOLOGY_LAB_UNIT);

        when(userRoleService.userInRole(eq(ALL_LAB_UNITS_TECH_USER_ID), any(Collection.class))).thenReturn(false);

        UserLabUnitRoles labRoles = createUserLabUnitRoles("AllLabUnits", TECHNICIAN_ROLE_ID);
        when(userRoleService.getUserLabUnitRoles(ALL_LAB_UNITS_TECH_USER_ID)).thenReturn(labRoles);

        // Test: AllLabUnits role works for any lab unit (Hematology in this case)
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, ALL_LAB_UNITS_TECH_USER_ID,
                HEMATOLOGY_LAB_UNIT);

        assertTrue("User with AllLabUnits role should be allowed for any lab unit", result);
    }

    // ========== TEST: SPECIFIC LAB UNIT ROLES (Priority 3) ==========

    @Test
    public void canCreateEntry_userWithCytologyTechnicianRole_allowedForCytologyLabUnit() {
        // Setup: User has Technician role only for Cytology
        setupNonAdminUser(CYTOLOGY_TECH_USER_ID);
        setupCanViewTemplate(CYTOLOGY_TECH_USER_ID, CYTOLOGY_LAB_UNIT);

        when(userRoleService.userInRole(eq(CYTOLOGY_TECH_USER_ID), any(Collection.class))).thenReturn(false);

        // Specific lab unit role (Cytology = test section ID 100)
        UserLabUnitRoles labRoles = createUserLabUnitRoles(CYTOLOGY_TEST_SECTION_ID, TECHNICIAN_ROLE_ID);
        when(userRoleService.getUserLabUnitRoles(CYTOLOGY_TECH_USER_ID)).thenReturn(labRoles);

        // Test: User with Cytology Technician role can create entry for Cytology
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, CYTOLOGY_TECH_USER_ID,
                CYTOLOGY_LAB_UNIT);

        assertTrue("User with Cytology Technician role should be allowed for Cytology lab unit", result);
    }

    @Test
    public void canCreateEntry_userWithCytologyTechnicianRole_deniedForHematologyLabUnit() {
        // Setup: User has Technician role only for Cytology, but tries to access
        // Hematology
        setupNonAdminUser(CYTOLOGY_TECH_USER_ID);
        setupCanViewTemplate(CYTOLOGY_TECH_USER_ID, HEMATOLOGY_LAB_UNIT);

        when(userRoleService.userInRole(eq(CYTOLOGY_TECH_USER_ID), any(Collection.class))).thenReturn(false);

        // Specific lab unit role (Cytology = test section ID 100)
        UserLabUnitRoles labRoles = createUserLabUnitRoles(CYTOLOGY_TEST_SECTION_ID, TECHNICIAN_ROLE_ID);
        when(userRoleService.getUserLabUnitRoles(CYTOLOGY_TECH_USER_ID)).thenReturn(labRoles);

        // Test: User with Cytology role should NOT be allowed for Hematology
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, CYTOLOGY_TECH_USER_ID,
                HEMATOLOGY_LAB_UNIT);

        assertFalse("User with Cytology Technician role should NOT be allowed for Hematology lab unit", result);
    }

    @Test
    public void canCreateEntry_userWithHematologyTechnicianRole_allowedForHematologyLabUnit() {
        // Setup: User has Technician role only for Hematology
        setupNonAdminUser(HEMATOLOGY_TECH_USER_ID);
        setupCanViewTemplate(HEMATOLOGY_TECH_USER_ID, HEMATOLOGY_LAB_UNIT);

        when(userRoleService.userInRole(eq(HEMATOLOGY_TECH_USER_ID), any(Collection.class))).thenReturn(false);

        UserLabUnitRoles labRoles = createUserLabUnitRoles(HEMATOLOGY_TEST_SECTION_ID, TECHNICIAN_ROLE_ID);
        when(userRoleService.getUserLabUnitRoles(HEMATOLOGY_TECH_USER_ID)).thenReturn(labRoles);

        // Test: User with Hematology Technician role can create entry for Hematology
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, HEMATOLOGY_TECH_USER_ID,
                HEMATOLOGY_LAB_UNIT);

        assertTrue("User with Hematology Technician role should be allowed for Hematology lab unit", result);
    }

    // ========== TEST: NO MATCHING ROLES ==========

    @Test
    public void canCreateEntry_userWithNoMatchingRoles_denied() {
        // Setup: User has no roles that match the template's allowedRoles
        setupNonAdminUser(NO_ROLES_USER_ID);
        setupCanViewTemplate(NO_ROLES_USER_ID, CYTOLOGY_LAB_UNIT);

        when(userRoleService.userInRole(eq(NO_ROLES_USER_ID), any(Collection.class))).thenReturn(false);
        when(userRoleService.getUserLabUnitRoles(NO_ROLES_USER_ID)).thenReturn(null);

        // Test: User with no matching roles should be denied
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, NO_ROLES_USER_ID, CYTOLOGY_LAB_UNIT);

        assertFalse("User with no matching roles should be denied", result);
    }

    @Test
    public void canCreateEntry_userWithNoMatchingRoles_butNoRoleRestrictions_allowed() {
        // Setup: User has no roles, but template has no role restrictions
        setupNonAdminUser(NO_ROLES_USER_ID);
        setupCanViewTemplateNoRestrictions(NO_ROLES_USER_ID, CYTOLOGY_LAB_UNIT);

        // Test: Template with no role restrictions allows any user
        boolean result = securityService.canCreateEntry(templateWithNoRoleRestrictions, NO_ROLES_USER_ID,
                CYTOLOGY_LAB_UNIT);

        assertTrue("User should be allowed when template has no role restrictions", result);
    }

    // ========== TEST: ROLE HIERARCHY PRIORITY ==========

    @Test
    public void canCreateEntry_globalRolesCheckedBeforeLabUnitRoles() {
        // This test verifies that global roles are checked first (priority 1)
        // If the user has a matching global role, lab unit roles should not matter
        setupNonAdminUser(GLOBAL_TECH_USER_ID);
        setupCanViewTemplate(GLOBAL_TECH_USER_ID, BIOCHEMISTRY_LAB_UNIT);

        // User has global Technician role
        when(userRoleService.userInRole(eq(GLOBAL_TECH_USER_ID), any(Collection.class))).thenAnswer(invocation -> {
            Collection<String> roles = invocation.getArgument(1);
            return roles.contains(ROLE_TECHNICIAN);
        });

        // Even though user has NO lab unit roles for Biochemistry, global role should
        // suffice
        when(userRoleService.getUserLabUnitRoles(GLOBAL_TECH_USER_ID)).thenReturn(null);

        // Test: Global role should allow access even without lab unit role
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, GLOBAL_TECH_USER_ID,
                BIOCHEMISTRY_LAB_UNIT);

        assertTrue("Global Technician role should allow access regardless of lab unit roles", result);
    }

    @Test
    public void canCreateEntry_allLabUnitsCheckedBeforeSpecificLabUnits() {
        // This test verifies that AllLabUnits is checked before specific lab units
        // (priority 2)
        setupNonAdminUser(ALL_LAB_UNITS_TECH_USER_ID);
        setupCanViewTemplate(ALL_LAB_UNITS_TECH_USER_ID, BIOCHEMISTRY_LAB_UNIT);

        when(userRoleService.userInRole(eq(ALL_LAB_UNITS_TECH_USER_ID), any(Collection.class))).thenReturn(false);

        // User has AllLabUnits role (no specific Biochemistry role needed)
        UserLabUnitRoles labRoles = createUserLabUnitRoles("AllLabUnits", TECHNICIAN_ROLE_ID);
        when(userRoleService.getUserLabUnitRoles(ALL_LAB_UNITS_TECH_USER_ID)).thenReturn(labRoles);

        // Test: AllLabUnits role should allow access to Biochemistry
        boolean result = securityService.canCreateEntry(templateWithAllowedRoles, ALL_LAB_UNITS_TECH_USER_ID,
                BIOCHEMISTRY_LAB_UNIT);

        assertTrue("AllLabUnits role should allow access to any lab unit including Biochemistry", result);
    }

    // ========== HELPER METHODS ==========

    private void setupNonAdminUser(String userId) {
        // User is NOT Global Administrator
        when(userRoleService.userInRole(eq(userId), eq(Constants.ROLE_GLOBAL_ADMIN))).thenReturn(false);
    }

    private void setupCanViewTemplate(String userId, String loginLabUnit) {
        // Setup so canViewTemplate returns true (user can view template based on
        // departments)
        // Use mocked TestSection objects to avoid SpringContext dependency
        Set<TestSection> departments = new HashSet<>();
        departments.add(createMockedTestSection(CYTOLOGY_TEST_SECTION_ID, CYTOLOGY_LAB_UNIT));
        departments.add(createMockedTestSection(HEMATOLOGY_TEST_SECTION_ID, HEMATOLOGY_LAB_UNIT));
        departments.add(createMockedTestSection("102", BIOCHEMISTRY_LAB_UNIT));

        templateWithAllowedRoles.setDepartments(departments);
    }

    private void setupCanViewTemplateNoRestrictions(String userId, String loginLabUnit) {
        // Setup template with no role restrictions and departments
        Set<TestSection> departments = new HashSet<>();
        departments.add(createMockedTestSection(CYTOLOGY_TEST_SECTION_ID, CYTOLOGY_LAB_UNIT));
        templateWithNoRoleRestrictions.setDepartments(departments);
    }

    private TestSection createMockedTestSection(String id, String name) {
        TestSection ts = Mockito.mock(TestSection.class);
        when(ts.getId()).thenReturn(id);
        when(ts.getTestSectionName()).thenReturn(name);
        when(ts.getLocalizedName()).thenReturn(name);
        return ts;
    }

    private UserLabUnitRoles createUserLabUnitRoles(String labUnit, String... roleIds) {
        UserLabUnitRoles userLabRoles = new UserLabUnitRoles();

        Set<LabUnitRoleMap> labUnitRoleMaps = new HashSet<>();
        LabUnitRoleMap roleMap = new LabUnitRoleMap();
        roleMap.setLabUnit(labUnit);

        Set<String> roles = new HashSet<>();
        for (String roleId : roleIds) {
            roles.add(roleId);
        }
        roleMap.setRoles(roles);
        labUnitRoleMaps.add(roleMap);

        userLabRoles.setLabUnitRoleMap(labUnitRoleMaps);
        return userLabRoles;
    }
}
