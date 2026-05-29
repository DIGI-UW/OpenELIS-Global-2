package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookStageAction;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.web.server.ResponseStatusException;

@RunWith(MockitoJUnitRunner.class)
public class NotebookStageAccessServiceTest {

    @Mock
    private WorkflowRegistryService workflowRegistryService;

    @Mock
    private NoteBookPageService noteBookPageService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private NotebookStageAccessService service;

    private NoteBook notebook;
    private NoteBookPage page;

    @Before
    public void setUp() {
        notebook = new NoteBook();
        notebook.setWorkflowType("biorepository");
        page = new NoteBookPage();
        page.setOrder(1);
        page.setId(1);
        page.setPageId("intake");
        page.setAllowedRoles(Set.of("Sample Collector"));
    }

    @Test
    public void assertPersonaForPage_deniesWhenUserLacksPersona() {
        doNothing().when(departmentIsolationService).assertNotebookDepartmentAccess(any(), any());
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(Set.of(5));
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);
        when(departmentIsolationService.getSysUserId(request)).thenReturn("1");
        when(noteBookPageService.get(1)).thenReturn(page);
        when(workflowRegistryService.isActionPermitted(any(), any(), any(Integer.class), any(NotebookStageAction.class)))
                .thenReturn(true);
        when(workflowRegistryService.resolveAllowedPersonasForAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of("Lab Manager"));
        when(userRoleService.getRoleIdsForUser("1")).thenReturn(List.of());
        when(userRoleService.getUserLabUnitRoles("1")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> service.assertPersonaForPage(request, notebook, 1));
    }

    @Test
    public void assertPersonaForPage_allowsMatchingPersona() {
        doNothing().when(departmentIsolationService).assertNotebookDepartmentAccess(any(), any());
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(java.util.Set.of(5));
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);
        when(departmentIsolationService.getSysUserId(request)).thenReturn("1");
        lenient().when(departmentIsolationService.getLoginLabUnit(request)).thenReturn("Biorepository Laboratory");
        when(noteBookPageService.get(1)).thenReturn(page);
        when(workflowRegistryService.isActionPermitted(any(), any(), any(Integer.class), any(NotebookStageAction.class)))
                .thenReturn(true);
        when(workflowRegistryService.resolveAllowedPersonasForAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of("Sample Collector"));

        org.openelisglobal.role.valueholder.Role collector = new org.openelisglobal.role.valueholder.Role();
        collector.setName("Sample Collector");
        when(userRoleService.getRoleIdsForUser("1")).thenReturn(List.of("role-1"));
        when(roleService.getRoleById("role-1")).thenReturn(collector);
        when(userRoleService.getUserLabUnitRoles("1")).thenReturn(null);

        service.assertPersonaForPage(request, notebook, 1);
    }

    @Test
    public void assertPersonaForPage_usesRegistryWhenPageRolesEmpty() {
        page.setAllowedRoles(new HashSet<>());
        doNothing().when(departmentIsolationService).assertNotebookDepartmentAccess(any(), any());
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(java.util.Set.of(5));
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);
        when(departmentIsolationService.getSysUserId(request)).thenReturn("1");
        lenient().when(departmentIsolationService.getLoginLabUnit(request)).thenReturn("Biorepository Laboratory");
        when(noteBookPageService.get(1)).thenReturn(page);
        when(workflowRegistryService.isActionPermitted(any(), any(), any(Integer.class), any(NotebookStageAction.class)))
                .thenReturn(true);
        when(workflowRegistryService.resolveAllowedPersonasForAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of("Lab Manager"));

        org.openelisglobal.role.valueholder.Role manager = new org.openelisglobal.role.valueholder.Role();
        manager.setName("Lab Manager");
        when(userRoleService.getRoleIdsForUser("1")).thenReturn(List.of("role-2"));
        when(roleService.getRoleById("role-2")).thenReturn(manager);
        when(userRoleService.getUserLabUnitRoles("1")).thenReturn(null);

        service.assertPersonaForPage(request, notebook, 1);
    }

    @Test
    public void assertPersonaForPage_matchesLabUnitRoleStoredAsId() {
        doNothing().when(departmentIsolationService).assertNotebookDepartmentAccess(any(), any());
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(Set.of(182));
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);
        when(departmentIsolationService.getSysUserId(request)).thenReturn("1");
        when(noteBookPageService.get(1)).thenReturn(page);
        when(workflowRegistryService.isActionPermitted(any(), any(), any(Integer.class), any(NotebookStageAction.class)))
                .thenReturn(true);
        when(workflowRegistryService.resolveAllowedPersonasForAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of("Sample Collector"));
        when(userRoleService.getRoleIdsForUser("1")).thenReturn(List.of());

        LabUnitRoleMap map = new LabUnitRoleMap();
        map.setLabUnit("182");
        map.setRoles(Set.of("role-1"));
        UserLabUnitRoles labRoles = new UserLabUnitRoles();
        labRoles.setLabUnitRoleMap(Set.of(map));
        when(userRoleService.getUserLabUnitRoles("1")).thenReturn(labRoles);
        when(departmentIsolationService.activeLoginLabUnitMatches(request, "182")).thenReturn(true);

        org.openelisglobal.role.valueholder.Role collector = new org.openelisglobal.role.valueholder.Role();
        collector.setName("Sample Collector");
        when(roleService.getRoleById("role-1")).thenReturn(collector);

        service.assertPersonaForPage(request, notebook, 1);
    }

    @Test
    public void assertStageAccess_deniesWrongDepartmentBeforePersonaCheck() {
        org.springframework.web.server.ResponseStatusException denied = new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Notebook is not accessible for the active department");
        org.mockito.Mockito.doThrow(denied).when(departmentIsolationService).assertNotebookDepartmentAccess(request,
                notebook);
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(Set.of(5));
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> service.assertStageAccess(request, notebook, page, NotebookStageAction.EDIT));
        verify(workflowRegistryService, never()).isActionPermitted(any(), any(), any(Integer.class), any());
    }
}
