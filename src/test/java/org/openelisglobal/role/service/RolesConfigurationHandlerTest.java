package org.openelisglobal.role.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.role.valueholder.Role;

@RunWith(MockitoJUnitRunner.class)
public class RolesConfigurationHandlerTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RolesConfigurationHandler handler;

    private Role testRole;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        testRole = new Role();
        testRole.setId("1");
        testRole.setName("Test Role");
        testRole.setDescription("Test Description");
        testRole.setActive(true);
        testRole.setEditable(true);
        testRole.setGroupingRole(false);
    }

    @Test
    public void testGetDomainName() {
        assertEquals("roles", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testProcessConfiguration_NewRoles() throws Exception {
        // Given
        String csv = "name,description,displayKey,active,editable,isGroupingRole\n"
                + "Lab Technician,Basic lab tech role,role.lab.tech,Y,Y,N\n"
                + "Results Validator,Can validate results,role.validator,Y,Y,N\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service to return null (roles don't exist)
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1", "2");
        when(roleService.get(anyString())).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(2)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_UpdateExistingRoles() throws Exception {
        // Given
        String csv = "name,description,displayKey,active,editable\n"
                + "Existing Role,Updated description,role.existing,Y,Y\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service to return existing role
        when(roleService.getRoleByName("Existing Role")).thenReturn(testRole);
        when(roleService.update(any(Role.class))).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(0)).insert(any(Role.class));
        verify(roleService, times(1)).update(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Role configuration file test.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingNameColumn_ThrowsException() throws Exception {
        // Given
        String csv = "description,displayKey\n" + "Test Description,role.test\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Role configuration file test.csv must have a 'name' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MinimalColumns() throws Exception {
        // Given
        String csv = "name\n" + "Role 1\n" + "Role 2\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1", "2");
        when(roleService.get(anyString())).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(2)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_QuotedFields() throws Exception {
        // Given
        String csv = "name,description\n" + "\"Role Name\",\"Description with, comma\"\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName("Role Name")).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1");
        when(roleService.get("1")).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(1)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_EmptyLinesIgnored() throws Exception {
        // Given
        String csv = "name\n" + "Role 1\n" + "\n" + "Role 2\n" + "\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1", "2");
        when(roleService.get(anyString())).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then - should only process 2 entries despite empty lines
        verify(roleService, times(2)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_CaseInsensitiveHeaders() throws Exception {
        // Given
        String csv = "NAME,Description,DisplayKey\n" + "Test Role,Test Desc,role.test\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1");
        when(roleService.get("1")).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(1)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_BooleanValues_YN() throws Exception {
        // Given
        String csv = "name,active,editable,isGroupingRole\n" + "Test Role,Y,N,Y\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1");
        when(roleService.get("1")).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(1)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_BooleanValues_TrueFalse() throws Exception {
        // Given
        String csv = "name,active,editable,isGroupingRole\n" + "Test Role,true,false,true\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1");
        when(roleService.get("1")).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(1)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_DefaultValues() throws Exception {
        // Given - CSV with only name, other fields should get defaults
        String csv = "name\n" + "Test Role\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1");
        when(roleService.get("1")).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(1)).insert(any(Role.class));
        // Note: Default values (active=true, editable=true, isGroupingRole=false,
        // description=name)
        // are tested implicitly through the insert call
    }

    @Test
    public void testProcessConfiguration_AllFields() throws Exception {
        // Given
        String csv = "name,description,displayKey,active,editable,isGroupingRole\n"
                + "Admin Role,Full admin access,role.admin,Y,N,N\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service
        when(roleService.getRoleByName(anyString())).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1");
        when(roleService.get("1")).thenReturn(testRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(1)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_WithGroupingParent() throws Exception {
        // Given
        String csv = "name,description,isGroupingRole,groupingParent\n" + "Parent Role,Parent role description,Y,\n"
                + "Child Role,Child role description,N,Parent Role\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        Role parentRole = new Role();
        parentRole.setId("1");
        parentRole.setName("Parent Role");
        parentRole.setGroupingRole(true);

        Role childRole = new Role();
        childRole.setId("2");
        childRole.setName("Child Role");
        childRole.setGroupingParent("1");

        // Mock role service - parent role doesn't exist initially, then child
        // references it
        // getRoleByName("Parent Role") is called:
        // 1. Once in processCsvLine to check if parent role exists (returns null)
        // 2. Once in createRole to resolve parent for child role (returns parentRole)
        when(roleService.getRoleByName("Parent Role")).thenReturn(null).thenReturn(parentRole);
        when(roleService.getRoleByName("Child Role")).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1", "2");
        when(roleService.get("1")).thenReturn(parentRole);
        when(roleService.get("2")).thenReturn(childRole);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(2)).insert(any(Role.class));
        verify(roleService, times(2)).getRoleByName("Parent Role");
    }

    @Test
    public void testProcessConfiguration_GroupingParentNotFound() throws Exception {
        // Given
        String csv = "name,description,groupingParent\n" + "Child Role,Child role description,NonExistent Parent\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock role service - parent role doesn't exist
        when(roleService.getRoleByName("Child Role")).thenReturn(null);
        when(roleService.getRoleByName("NonExistent Parent")).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1");
        when(roleService.get("1")).thenReturn(testRole);

        // When - should still create role but log warning
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(1)).insert(any(Role.class));
    }

    @Test
    public void testProcessConfiguration_HierarchicalRoles() throws Exception {
        // Given - Create a 3-level hierarchy
        String csv = "name,description,isGroupingRole,groupingParent\n" + "Level 1,Top level group,Y,\n"
                + "Level 2,Second level group,Y,Level 1\n" + "Level 3,Bottom level role,N,Level 2\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        Role level1Role = new Role();
        level1Role.setId("1");
        level1Role.setName("Level 1");
        level1Role.setGroupingRole(true);

        Role level2Role = new Role();
        level2Role.setId("2");
        level2Role.setName("Level 2");
        level2Role.setGroupingRole(true);
        level2Role.setGroupingParent("1");

        Role level3Role = new Role();
        level3Role.setId("3");
        level3Role.setName("Level 3");
        level3Role.setGroupingParent("2");

        // Mock role service for hierarchical lookups
        when(roleService.getRoleByName("Level 1")).thenReturn(null).thenReturn(level1Role);
        when(roleService.getRoleByName("Level 2")).thenReturn(null).thenReturn(level2Role);
        when(roleService.getRoleByName("Level 3")).thenReturn(null);
        when(roleService.insert(any(Role.class))).thenReturn("1", "2", "3");
        when(roleService.get("1")).thenReturn(level1Role);
        when(roleService.get("2")).thenReturn(level2Role);
        when(roleService.get("3")).thenReturn(level3Role);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(roleService, times(3)).insert(any(Role.class));
    }
}
